package com.example.hectoclash

/**
 * Data class representing a Player in the leaderboard.
 * @param name The name of the player.
 */
data class Player(
    val name: String,  // Player's name
    val rating: Int,
    val accuracy: Int,
    val time: Int,
    val uid: String,
    val profileURL: String
)
