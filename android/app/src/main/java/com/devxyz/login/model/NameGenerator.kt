package com.devxyz.login.model

import kotlin.random.Random

/**
 * Random profile-name generator for the Add profile sheet (spec v2:
 * the real app pre-fills the Name field with a generated word-word
 * name, e.g. "rapid-unit"). Purely cosmetic — the user can edit or
 * clear it before creating the profile.
 */
object ProfileNames {

    private val adjectives = listOf(
        "rapid", "swift", "quiet", "bright", "calm", "bold", "clever",
        "eager", "gentle", "happy", "lucky", "merry", "noble", "prime",
        "quick", "royal", "solar", "turbo", "ultra", "vivid", "amber",
        "cosmic", "delta", "ember", "frost", "golden", "hidden", "iron",
        "jade", "lunar"
    )

    private val nouns = listOf(
        "unit", "fox", "wave", "star", "moon", "pine", "rock", "lion",
        "bear", "wolf", "hawk", "kite", "leaf", "snow", "rain", "wind",
        "lake", "hill", "port", "beam", "cloud", "drift", "echo",
        "flame", "grove", "harbor", "isle", "peak", "reef", "trail"
    )

    fun random(): String =
        adjectives[Random.nextInt(adjectives.size)] + "-" +
            nouns[Random.nextInt(nouns.size)]
}
