package com.example.hectoclash

import android.content.Context
import androidx.core.content.edit

object SoundManager {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_SOUND_STATE = "sound_state"

    fun getSoundState(context: Context): SoundState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ordinal = prefs.getInt(KEY_SOUND_STATE, 0)
        return SoundState.entries[ordinal]
    }

    fun setSoundState(context: Context, state: SoundState) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit() { putInt(KEY_SOUND_STATE, state.ordinal) }
        MusicManager.updateVolumeAll(context)
    }

    fun cycleSoundState(context: Context): SoundState {
        val current = getSoundState(context)
        val next = SoundState.next(current)
        setSoundState(context, next)
        return next
    }

    fun getVolumeMultiplier(context: Context): Float {
        return getSoundState(context).volumeMultiplier
    }
}