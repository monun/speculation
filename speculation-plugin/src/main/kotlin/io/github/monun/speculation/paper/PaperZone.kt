package io.github.monun.speculation.paper

import io.github.monun.speculation.game.zone.Zone
import io.github.monun.speculation.game.zone.ZoneProperty
import org.bukkit.Location
import org.bukkit.Rotation
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import kotlin.random.Random

abstract class PaperZone {
    companion object {
        fun of(zone: Zone): PaperZone = when (zone) {
            is ZoneProperty -> PaperZoneProperty(zone)
            else -> PaperZoneSpecial(zone)
        }
    }

    abstract val zone: Zone

    lateinit var process: PaperGameProcess
    var isCorner = false
    lateinit var box: BoundingBox
    lateinit var forwardFace: Face
    lateinit var reverseFace: Face
    lateinit var innerFace: Face
    lateinit var outerFace: Face
    lateinit var rotation: Rotation

    val position: Vector
        get() = box.center.apply { y = box.maxY }

    val location: Location
        get() = position.toLocation(process.world)

    fun nextLocation() = location.apply {
        val wiggle = 1.0
        val half = wiggle / 2.0
        x += Random.nextDouble(wiggle) - half
        z += Random.nextDouble(wiggle) - half
    }

    abstract fun destroy()
}