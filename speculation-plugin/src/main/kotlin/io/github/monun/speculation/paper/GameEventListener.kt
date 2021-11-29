package io.github.monun.speculation.paper

import io.github.monun.heartbeat.coroutines.Heartbeat
import io.github.monun.heartbeat.coroutines.Suspension
import io.github.monun.speculation.game.MovementCause
import io.github.monun.speculation.game.event.PieceMoveEvent
import io.github.monun.speculation.game.event.PropertyUpgradeEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow

class GameEventListener(private val process: PaperGameProcess) {
    init {
        process.game.eventAdapter.apply {
            register(PieceMoveEvent::class.java, ::onPieceMove)
            register(PropertyUpgradeEvent::class.java, ::onPropertyUpgrade)
        }
    }

    private suspend fun onPieceMove(event: PieceMoveEvent) {
        val piece = event.piece
        val zone = event.to

        val journey = event.journey

        val paperPiece = piece.attachment<PaperPiece>()
        val paperZone = zone.attachment<PaperZone>()

        withContext(Dispatchers.Heartbeat) {
            val from = paperPiece.stand.location
            val to = paperZone.nextLocation()
            val distance = from.distance(to)
            var ticks = (distance * 1.5).toInt()
            if (distance < 8.0)
                ticks = 5
            if (journey.cause == MovementCause.PORTAL) ticks = ticks.shr(1)

            val vector = to.clone().subtract(from).toVector()
            val sus = Suspension()
            for (tick in 1..ticks) {
                sus.delay(50L)
                val parabolaX = tick.toDouble() / ticks
                val parabolaY = 4.0 * parabolaX - 4.0 * parabolaX.pow(2)

                val location = from.clone().apply {
                    add(vector.clone().multiply(parabolaX))
                    y += parabolaY * distance / 3.0
                }
                paperPiece.stand.moveTo(location)
            }
            paperPiece.playStepSound()
        }
    }

    private suspend fun onPropertyUpgrade(event: PropertyUpgradeEvent) {
        val property = event.property
        val piece = event.piece

        val paperProperty = property.attachment<PaperZoneProperty>()
        val paperPiece = piece.attachment<PaperPiece>()

        withContext(Dispatchers.Heartbeat) {
            paperProperty.playUpgradeEffect(paperPiece, event.level)
            paperProperty.updateSlots()
            paperProperty.updateTolls()
        }
    }
}