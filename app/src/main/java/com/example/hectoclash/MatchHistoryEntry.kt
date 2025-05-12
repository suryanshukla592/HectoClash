package com.example.hectoclash

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
@Keep
@Parcelize
data class MatchHistoryEntry(
    val feedback: String = "",
    val opponentUID: String = "",
    val puzzle: String = "",
    val result: String = "",
    val selfUID: String = "",
    val timestamp: Long = 0L
) : Parcelable
