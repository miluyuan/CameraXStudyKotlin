package com.example.camerastudykotlin

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.camerastudykotlin.databinding.CameraControlUiBinding
import com.example.camerastudykotlin.databinding.FragmentCameraViewBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 使用封装好的 CameraView 实现拍照功能
 * @author wzw
 * @date 2020/5/13 11:19
 */
class CameraViewFragment : Fragment() {
    private lateinit var binding: FragmentCameraViewBinding

    private lateinit var container: ConstraintLayout
    private lateinit var outputDir: File
    private lateinit var executor: ExecutorService
    private var lastPhoto: File? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCameraViewBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view as ConstraintLayout
        outputDir = CameraFragment.getOutputDir(requireContext())
        executor = Executors.newSingleThreadExecutor()

        binding.cameraView.post {
            updateCameraUi()

            bindCamera()
        }
    }

    private fun bindCamera() {


        binding.cameraView.bindToLifecycle(this)
    }

    private fun updateCameraUi() {
        container.findViewById<ConstraintLayout>(R.id.camera_ui_container)?.let {
            container.removeView(it)
        }

        val controlsView = View.inflate(requireContext(), R.layout.camera_control_ui, container)
        val controlsBinding = CameraControlUiBinding.bind(controlsView)

        controlsBinding.rgFlash.setOnCheckedChangeListener { group, checkedId ->
            binding.cameraView.flash = when (checkedId) {
                R.id.rb_flash_off -> ImageCapture.FLASH_MODE_OFF
                R.id.rb_flash_on -> ImageCapture.FLASH_MODE_ON
                else -> ImageCapture.FLASH_MODE_AUTO
            }
        }

        controlsBinding.btnCameraSwitch.setOnClickListener {
            // 切换摄像头
            binding.cameraView.cameraLensFacing = if (binding.cameraView.cameraLensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            // 或者：
//            binding.cameraView.toggleCamera()
        }

        controlsBinding.btnCameraCapture.setOnClickListener {
            // 拍照
            val photoFile = CameraFragment.createFile(outputDir, CameraFragment.FILENAME, CameraFragment.PHOTO_EXTENSION)
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            // OnImageSavedCallback 返回图片 Uri
            binding.cameraView.takePicture(photoFile, executor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    //注意 这个方法在子线程
                    lastPhoto = photoFile
                    setGalleryThumbnail(Uri.fromFile(photoFile))

                    Log.e(CameraFragment.TAG, "Photo capture succeeded file length: ${photoFile.length() / 1024f / 1024}")

                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(CameraFragment.TAG, "Photo capture failed: ${exc.message}", exc)
                }

            })

        }

        controlsBinding.btnPhotoView.setOnClickListener {
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
}