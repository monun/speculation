package io.github.monun.speculation.paper

import io.github.monun.speculation.game.zone.*
import org.bukkit.Location
import org.bukkit.Rotation
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import kotlin.random.Random

abstract class PaperZone {
    companion object {
        fun of(zone: Zone): PaperZone = when (zone) {
            is ZoneProperty -> PaperZoneProperty(zone)
            is ZoneStart -> PaperZoneStart(zone)
            is ZoneGamble -> PaperZoneGamble(zone)
            is ZoneJail -> PaperZoneJail(zone)
            is ZoneMagic -> PaperZoneMagic(zone)
            is ZoneFestival -> PaperZoneFestival(zone)
            is ZonePortal -> PaperZonePortal(zone)
            is ZoneNTS -> PaperZoneNTS(zone)
            else -> error("impossible")
        }
    }

    abstract val zone: Zone

    lateinit var process: PaperGameProcess
    var isCorner = false
    lateinit var name: String
    lateinit var author: String
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

    fun nextPieceLocation() = location.apply {
        val wiggle = 1.0
        val half = wiggle / 2.0
        x += Random.nextDouble(wiggle) - half
        z += Random.nextDouble(wiggle) - half
    }

    fun nextLocation() = box.min.apply { y = box.maxY }.apply {
        x += Random.nextDouble(box.widthX)
        z += Random.nextDouble(box.widthZ)
    }.toLocation(process.world)

    open fun onUpdate() = Unit
    open suspend fun playPassEffect() = Unit
    open suspend fun playArriveEffect(piece: PaperPiece) = Unit
    open suspend fun playLeaveEffect(piece: PaperPiece) = Unit

    abstract fun destroy()
}