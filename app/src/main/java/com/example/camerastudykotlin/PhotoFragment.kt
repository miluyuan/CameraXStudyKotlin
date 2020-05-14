package com.example.camerastudykotlin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.camerastudykotlin.databinding.FragmentPhotoBinding

/**
 * @author wzw
 * @date 2020/5/12 15:57
 */
class PhotoFragment internal constructor() : Fragment() {
    private lateinit var binding: FragmentPhotoBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentPhotoBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val resource = arguments?.getString(KEY_FILE_NAME) ?: R.drawable.ic_photo
        Glide.with(binding.imageView).load(resource).into(binding.imageView)
    }

    companion object {
        private const val TAG = "PhotoFragment"
        private const val KEY_FILE_NAME = "file_name"

        fun create(path: String) = PhotoFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_FILE_NAME, path)
            }
        }
    }
}