package com.example.hectoclash

import android.content.Context
import android.media.MediaPlayer

object SfxManager {
    fun playSfx(context: Context, soundResId: Int) {
        val volume = SoundManager.getVolumeMultiplier(context)
        val mediaPlayer = MediaPlayer.create(context, soundResId)
        mediaPlayer?.apply {
            setVolume(volume, volume)
            start()
            setOnCompletionListener {
                it.release()
            }
        }
    }
}
