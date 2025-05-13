package com.example.hectoclash

import android.content.Context
import android.media.MediaPlayer

object MusicManager {

    private var mediaPlayer: MediaPlayer? = null
    private var isPaused = false
    private var currentMusicResId: Int? = null

    fun startMusic(context: Context, musicResId: Int) {
        // Only create new player if it's a new track or player is null
        if (mediaPlayer == null || currentMusicResId != musicResId) {
            stopMusic() // stop old if any

            mediaPlayer = MediaPlayer.create(context.applicationContext, musicResId)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
            currentMusicResId = musicResId
        } else if (!mediaPlayer!!.isPlaying) {
            mediaPlayer?.start()
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
}
