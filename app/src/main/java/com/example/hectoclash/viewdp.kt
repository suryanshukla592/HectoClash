package com.example.hectoclash

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView


class viewdp(private val imageUrl: String) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dpview, container, false).apply {
            val photoView: PhotoView = findViewById(R.id.dp_image_view)
            val closeButton: ImageView = findViewById(R.id.close_button)

            context?.let { Glide.with(it).load(imageUrl).placeholder(R.drawable.defaultdp).into(photoView) }

            closeButton.setOnClickListener { dismiss() }
            val mediaPlayer = MediaPlayer.create(context, R.raw.button_sound)
            mediaPlayer.start()

            mediaPlayer.setOnCompletionListener {
                it.release()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }
}
