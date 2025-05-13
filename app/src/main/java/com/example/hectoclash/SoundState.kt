package com.example.hectoclash

import android.content.Context

enum class SoundState(val volumeMultiplier: Float) {
    ON(1.0f),
    HALF(0.5f),
    OFF(0.0f);

    companion object {
        fun next(state: SoundState): SoundState {
            return values()[(state.ordinal + 1) % values().size]
        }
    }
}
