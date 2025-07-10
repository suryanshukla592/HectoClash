package com.example.hectoclash

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

object MusicManager {

    private var mediaPlayer: MediaPlayer? = null
    private var isPaused = false
    private var currentMusicResId: Int? = null

    fun startMusic(context: Context, musicResId: Int, seekTimeMs: Int) {
        if (mediaPlayer == null || currentMusicResId != musicResId) {
            stopMusic()

            mediaPlayer = MediaPlayer.create(context.applicationContext, musicResId)
            mediaPlayer?.isLooping = true
            mediaPlayer?.setVolume(0f, 0f)
            mediaPlayer?.seekTo(seekTimeMs)
            mediaPlayer?.start()
            currentMusicResId = musicResId

            // Fade in
            val handler = Handler(Looper.getMainLooper())
            val steps = 10
            val duration = 500L
            val delay = duration / steps
            val targetVolume = SoundManager.getVolumeMultiplier(context)
            var currentStep = 0

            val fadeIn = object : Runnable {
                override fun run() {
                    if (currentStep <= steps) {
                        val volume = targetVolume * (currentStep.toFloat() / steps)
                        mediaPlayer?.setVolume(volume, volume)
                        currentStep++
                        handler.postDelayed(this, delay)
                    } else {
                        mediaPlayer?.setVolume(targetVolume, targetVolume)
                    }
                }
            }
            handler.post(fadeIn)
        } else {
            mediaPlayer?.seekTo(seekTimeMs)
            if (!mediaPlayer!!.isPlaying) mediaPlayer?.start()
        }
    }
    fun pauseMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPaused = true
            }
        }
    }
    fun resumeMusic() {
        mediaPlayer?.let {
            if (isPaused) {
                it.start()
                isPaused = false
            }
        }
    }
    fun stopMusic() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentMusicResId = null
        isPaused = false
    }
    fun setMusicVolume(context: Context) {
        val volume = SoundManager.getVolumeMultiplier(context)
        mediaPlayer?.setVolume(volume, volume)
    }
    fun updateVolumeAll(context: Context) {
        setMusicVolume(context) // add more if needed (like SFX manager)
    }
    fun getCurrentSeekTime(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }
}

