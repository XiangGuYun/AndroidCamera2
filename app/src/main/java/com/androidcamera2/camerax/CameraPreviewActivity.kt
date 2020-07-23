package com.androidcamera2.camerax

import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.androidcamera2.R
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 演示如何用CameraX实现相机的预览功能
 *
 * 注意在build.gradle中需添加支持java8，否则会报错
 *   compileOptions {
 *      sourceCompatibility rootProject.ext.java_version
 *      targetCompatibility rootProject.ext.java_version
 *   }
 */
class CameraPreviewActivity : AppCompatActivity() {

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.fragment_camera)
        // 等待PreviewView被正确地布局
        viewFinder.post {
            // 设置相机和它的use cases
            setUpCamera()
        }
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

        try {
            // 绑定Activity的生命周期
            // 传入可变数量的用例，在此只传入preview
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview
            )
            // 附加PreviewView的surface provider给preview用例
            preview?.setSurfaceProvider(viewFinder.createSurfaceProvider())
        } catch (exc: Exception) {
            Log.e("TestCamera", "Use case binding failed", exc)
        }
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

}