package com.example.hectoclash

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MatchHistoryEntry(
    val feedback: String = "",
    val opponentUID: String = "",
    val puzzle: String = "",
    val result: String = "",
    val selfUID: String = "",
    val timestamp: Long = 0L
) : Parcelable
