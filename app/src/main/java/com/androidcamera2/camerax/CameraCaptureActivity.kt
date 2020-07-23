package com.androidcamera2.camerax

import android.content.Intent
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import com.androidcamera2.MainActivity
import com.androidcamera2.R
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.control.*
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 该用例在Preview的基础上演示如何使用CameraX实现拍照和切换前后置相机的功能
 * 新增的代码会用线包围起来
 */
class CameraCaptureActivity : AppCompatActivity() {
    //==========================================================================================
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    //==========================================================================================
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var cameraProvider: ProcessCameraProvider? = null

    //==========================================================================================
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    //==========================================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.fragment_camera)
        // 等待PreviewView被正确地布局
        viewFinder.post {
            // 设置相机和它的use cases
            setUpCamera()
        }

        //==========================================================================================
        camera_switch_button.visibility = View.VISIBLE
        camera_capture_button.visibility = View.VISIBLE
        photo_view_button.visibility = View.VISIBLE

        cameraExecutor = Executors.newSingleThreadExecutor()

        camera_switch_button.setOnClickListener {
            // 在相机被设置完成前禁用按钮
            it.isEnabled = false
            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            // 重新绑定用例来更新绑定的相机
            bindCameraUseCases()
        }

        camera_capture_button.setOnClickListener {
            imageCapture?.let { imageCapture ->

                // 创建输出文件来保存图像
                val photoFile = createImageFile(
                    MainActivity.getOutputDirectory(
                        this
                    ), "yyyy-MM-dd-HH-mm-ss-SSS", ".jpg")

                // 设置图像捕捉元数据
                val metadata = ImageCapture.Metadata().apply {
                    // 当使用前置摄像头时，镜像化图像
                    isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
                }

                // 创建包含输出文件和元数据的输出选项
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                    .setMetadata(metadata)
                    .build()

                // 设置图片捕捉监听器，在拍照后触发
                imageCapture.takePicture(
                    outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                        /**
                         * 当发生错误时触发该回调
                         */
                        override fun onError(exc: ImageCaptureException) {
                            Log.e("TestCamera", "照片捕捉失败: ${exc.message}", exc)
                        }

                        /**
                         * 当图像被保存时触发该回调
                         */
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                            Log.d("TestCamera", "照片捕捉成功: $savedUri")

                            // 我们只能在M及以上的SDK版本才能修改前景图像
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                // 用最新的照片来更新图片缩略图
                                photo_view_button.post {
                                    Glide.with(photo_view_button)
                                        .load(savedUri)
                                        .apply(RequestOptions.circleCropTransform())
                                        .into(photo_view_button)
                                }
                            }

                            // 对于运行API级别 >= 24的设备，隐式广播将被忽略
                            // 因此，如果您的目标API级别为24+，您可以删除该语句
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                sendBroadcast(
                                    Intent(Camera.ACTION_NEW_PICTURE, savedUri)
                                )
                            }

                            // 如果所选文件夹是外部媒体目录，这样做就不必要，但其他应用程序将无法访问我们的图像，
                            // 除非我们扫描他们使用[MediaScannerConnection]
                            val mimeType = MimeTypeMap.getSingleton()
                                .getMimeTypeFromExtension(savedUri.toFile().extension)
                            MediaScannerConnection.scanFile(
                                this@CameraCaptureActivity,
                                arrayOf(savedUri.toFile().absolutePath),
                                arrayOf(mimeType)
                            ) { _, uri ->
                                Log.d("TestCamera", "Image capture scanned into media store: $uri")
                            }
                        }
                    })
            }
        }
        //==========================================================================================

    }

    /** 初始化CameraX, 并准备去绑定camera的use cases  */
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // 获取到相机提供者
            cameraProvider = cameraProviderFuture.get()

            // 构建和绑定相机用例
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    /** 声明和绑定预览, 捕捉和分析用例 */
    private fun bindCameraUseCases() {

        // 获取用于设置摄像头全屏分辨率的屏幕参数
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }

        // 屏幕宽高比例
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)

        // 预览视图的旋转角度
        val rotation = viewFinder.display.rotation

        // CameraProvider
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        // 相机镜头方向选择器
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // Preview
        preview = Preview.Builder()
            // 设置目标宽高比
            .setTargetAspectRatio(screenAspectRatio)
            // 设置初始目标旋转角度
            .setTargetRotation(rotation)
            .build()

        // 在绑定用例之前必须先解绑它们
        cameraProvider.unbindAll()

        //==========================================================================================
        // ImageCapture - 图像捕捉
        imageCapture = ImageCapture.Builder()
            // 设置捕捉模式为-最小化延迟
            // 最小化延迟模式简介：优化捕获管道，优先考虑等待时间而不是图像质量。
            // 当捕获模式设置为MIN_LATENCY时，图像捕获速度可能会更快，但图像质量可能会降低。
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // 设置宽高比例，无需设置分辨率来匹配预览配置，CameraX会在各种分辨率下进行优化来适应我们的用例
            .setTargetAspectRatio(screenAspectRatio)
            // 设置初始目标旋转，如果在此用例的生命周期中旋转改变，我们将不得不再次调用它
            .setTargetRotation(rotation)
            .build()
        //==========================================================================================

        try {
            // 绑定Activity的生命周期
            // 传入可变数量的用例，在此只传入preview
            cameraProvider.bindToLifecycle(
                //==========================================================================================
                this, cameraSelector, preview, imageCapture
                //==========================================================================================
            )
            // 附加PreviewView的surface provider给preview用例
            preview?.setSurfaceProvider(viewFinder.createSurfaceProvider())
        } catch (exc: Exception) {
            Log.e("TestCamera", "Use case binding failed", exc)
        }

        camera_switch_button.isEnabled = true
    }

    /**
     * 传入预览的宽高值来获取最适合的宽高比（4:3 或者 16:9）
     *  @param width - 预览宽度
     *  @param height - 预览高度
     *  @return 最合适的宽高比
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 4.0 / 3.0)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun createImageFile(baseFolder: File, format: String, extension: String) =
        File(baseFolder, SimpleDateFormat(format, Locale.US)
            .format(System.currentTimeMillis()) + extension)
}