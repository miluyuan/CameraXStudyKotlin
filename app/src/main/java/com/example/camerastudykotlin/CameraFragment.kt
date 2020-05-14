package com.example.camerastudykotlin

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 *
 * @author wzw
 * @date 2020/5/7 15:00
 */
class CameraFragment : Fragment() {
    private lateinit var previewView: PreviewView
    private lateinit var container: ConstraintLayout
    private lateinit var outputDir: File
    private lateinit var executor: ExecutorService
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null

    private var camera: Camera? = null
    private var aspectRatio: Int = AspectRatio.RATIO_4_3

    //前置摄像头默认就有镜像效果
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    private var lastPhoto: File? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view as ConstraintLayout
        previewView = view.findViewById<PreviewView>(R.id.preview_view)

        // 设置预览视图的对齐方式，默认 FILL_CENTER 等比例铺满 previewView
        previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
        outputDir = getOutputDir(requireContext())
        executor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        previewView.post {

            updateCameraUi()

            bindCameraUseCase()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }

    private fun updateCameraUi() {
        container.findViewById<ConstraintLayout>(R.id.camera_ui_container)?.let {
            container.removeView(it)
        }

        val controlsView = View.inflate(requireContext(), R.layout.camera_control_ui, container)

        controlsView.findViewById<RadioGroup>(R.id.rg_flash).setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_flash_auto -> imageCapture?.flashMode = ImageCapture.FLASH_MODE_AUTO
                R.id.rb_flash_off -> imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
                R.id.rb_flash_on -> imageCapture?.flashMode = ImageCapture.FLASH_MODE_ON
            }
        }
        controlsView.findViewById<Button>(R.id.btn_camera_switch).setOnClickListener {
            // 切换摄像头
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            bindCameraUseCase()
        }

        controlsView.findViewById<Button>(R.id.btn_camera_capture).setOnClickListener {
            // 拍照
            val photoFile = createFile(outputDir, FILENAME, PHOTO_EXTENSION)
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            // OnImageSavedCallback 返回图片 Uri
            imageCapture?.takePicture(outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // 只有传入的是 ImageCapture.OutputFileOptions.Builder(ContentResolver, Uri, ContentValues) 时 savedUri 才有值
                    //注意 这个方法在子线程
                    Log.e(TAG, Thread.currentThread().name + " =================")
                    Log.e(TAG, "Photo capture succeeded: ${outputFileResults.savedUri}")
                    val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                    Log.e(TAG, "Photo capture succeeded: $savedUri")
                    lastPhoto = photoFile
                    setGalleryThumbnail(savedUri)

                    Log.e(TAG, "Photo capture succeeded file length: ${photoFile.length() / 1024f / 1024}")

                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

            })

            // OnImageCapturedCallback 返回图片数据
//            imageCapture?.takePicture(
//                    executor,
//                    object : ImageCapture.OnImageCapturedCallback() {
//                        override fun onCaptureSuccess(image: ImageProxy) {
//                            // 将 imageProxy 转为 byte数组
//                            val buffer: ByteBuffer = image.planes[0].buffer
//                            // 倒带到起始位置 0
//                            buffer.rewind()
//                            // 新建指定长度数组
//                            val byteArray = ByteArray(buffer.remaining())
//                            // 数据复制到数组
//                            buffer.get(byteArray)
//                            Log.e(TAG, "size: ${byteArray.size/1024/1024} M")
//
//                            val imageView = container.findViewById<ImageView>(R.id.image_view)
//                            imageView.post {
//                                Glide.with(imageView)
//                                        .load(byteArray)
//                                        .into(imageView)
//                            }
//
//                            image.close()
//                        }
//
//                        override fun onError(exc: ImageCaptureException) {
//                            Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
//                        }
//
//                    })
        }

        controlsView.findViewById<ImageView>(R.id.btn_photo_view).setOnClickListener {
            // 照片查看
            fragmentManager?.beginTransaction()?.apply {
                // 动画要放 fragment 前面, 注意参数位置
                setCustomAnimations(R.anim.fragment_enter, 0, 0, R.anim.fragment_exit)
                add(R.id.fragment_container, PhotoFragment.create(lastPhoto?.absolutePath ?: ""))
                addToBackStack(null)
                commit()
            }
        }
    }

    private fun setGalleryThumbnail(savedUri: Uri) {
        val imageButton = container.findViewById<ImageView>(R.id.btn_photo_view)
        // 在主线程执行
        imageButton.post {
            imageButton.setPadding(12)
            Glide.with(imageButton)
                    .load(savedUri)
                    .apply(RequestOptions.circleCropTransform())
                    .into(imageButton)
        }
    }

    private fun bindCameraUseCase() {
        val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            preview = getPreview()
            imageCapture = getImageCapture()
            //绑定前需解绑
            cameraProvider.unbindAll()
            try {
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                preview?.setSurfaceProvider(previewView.createSurfaceProvider(camera?.cameraInfo))

                // 可控制自动对焦，缩放等
                /* val factory = previewView.createMeteringPointFactory(cameraSelector)
                 val action = FocusMeteringAction.Builder(factory.createPoint(0f, 0f)).build()
                 camera?.cameraControl?.startFocusAndMetering(action)*/
//                camera?.cameraControl?.setLinearZoom()
//                camera?.cameraControl?.setZoomRatio()
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun getImageCapture(): ImageCapture {
        return ImageCapture.Builder()
                // 设置是拍照质量优先还是速度优先，在小米6手机上二者大小相差1M左右 （都是2160*3840， 3.2M 和 2.07M）
                // 默认速度优先
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                // 设置宽高比率
                .setTargetAspectRatio(aspectRatio)
                // 设置照片方向
                .setTargetRotation(previewView.display.rotation)
                // 默认关闭
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .build()
    }

    private fun getPreview(): Preview {
        return Preview.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetRotation(previewView.display.rotation)
                .build()
    }

    companion object {
        const val TAG = "CameraFragment"
        const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val PHOTO_EXTENSION = ".jpg"

        fun getOutputDir(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir: File? = appContext.externalMediaDirs.firstOrNull()
                    ?.let {
                        File(it, appContext.getString(R.string.app_name)).apply {
                            mkdirs()
                        }
                    }
            return if (mediaDir != null && mediaDir.exists()) {
                mediaDir
            } else {
                appContext.filesDir
            }
        }

        fun createFile(dir: File, format: String, extension: String): File {
            return File(
                    dir,
                    SimpleDateFormat(format).format(System.currentTimeMillis()) + extension
            )
        }
    }
}