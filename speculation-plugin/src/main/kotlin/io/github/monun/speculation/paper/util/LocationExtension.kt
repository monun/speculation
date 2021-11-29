package io.github.monun.speculation.paper.util

import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.SoundCategory


fun Location.playSound(sound: Sound, pitch: Float) {
    world.playSound(this, sound, SoundCategory.MASTER, 100.0F, pitch)
}