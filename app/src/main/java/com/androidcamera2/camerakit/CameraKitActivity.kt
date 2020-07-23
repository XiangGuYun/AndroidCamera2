package com.androidcamera2.camerakit

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.androidcamera2.R
import kotlinx.android.synthetic.main.activity_camera_kit.*

/**
 * CameraKit帮助您快速地添加可靠的相机到您的应用程序。
 * 我们的开源相机平台提供了一致的捕捉结果、可伸缩的服务和无限的相机可能性。
 *
 * 有了CameraKit，你可以毫不费力地做到以下几点:
 *
 * 图像和视频捕捉无缝工作与相同的预览会话。
 *
 * 自动系统权限处理。
 *
 * 自动缩放预览。
 *
 * 创建一个任意大小的摄像头视图(不仅仅是预设的!)
 *
 * 自动输出裁剪匹配你的相机视图边界。
 *
 * 多个捕获方法。
 * METHOD_STANDARD: 通常使用相机api捕获的图像。
 * METHOD_STILL: 相机速度较慢的设备的CameraView预览(类似于SnapChat和Instagram)的定格画面。
 * METHOD_SPEED: 基于测量速度的自动捕获方法。
 *
 * 内置持续聚焦。
 * 内置点击聚焦。
 * 内置手势缩放功能。
 *
 * 目前我们支持CameraKit的两个版本:v1.0.0-beta3。X和v0.13.X。
 *
 * 如果你只需要photo，那就在v1.0.0-beta3.11上试试最新最棒的CameraKit功能吧。我们的beta3.11版本还不支持视频，但该功能即将发布!
 *
 * 同时，如果您的应用需要视频，我们建议继续使用v0.13.4;最新的稳定版本与视频实现。
 */
class CameraKitActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_kit)
    }

    override fun onStart() {
        super.onStart()
        cameraKitView.onStart()
    }

    override fun onResume() {
        super.onResume()
        cameraKitView.onResume()
    }

    override fun onPause() {
        super.onPause()
        cameraKitView.onPause()
    }

    override fun onStop() {
        super.onStop()
        cameraKitView.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraKitView.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}