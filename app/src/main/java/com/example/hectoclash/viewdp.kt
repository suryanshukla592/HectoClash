package com.example.hectoclash

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import androidx.core.graphics.drawable.toDrawable


class viewdp(private val imageUrl: String) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dpview, container, false).apply {
            val photoView: PhotoView = findViewById(R.id.dp_image_view)
            val closeButton: ImageView = findViewById(R.id.close_button)

            context?.let { Glide.with(it).load(imageUrl).placeholder(R.drawable.defaultdp).into(photoView) }

            closeButton.setOnClickListener {
                SfxManager.playSfx(context, R.raw.button_sound)
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
    }
}
