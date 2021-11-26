package io.github.monun.speculation.game.zone

import io.github.monun.speculation.game.Journey
import io.github.monun.speculation.game.Movement
import io.github.monun.speculation.game.Piece
import javax.print.attribute.standard.Destination
import kotlin.math.abs

abstract class Zone {
    lateinit var previous: Zone
    lateinit var next: Zone

    fun shift(movement: Movement): Zone {
        return when (movement) {
            Movement.FORWARD -> next
            Movement.REVERSE -> previous
            else -> error("$movement shift?")
        }
    }

    fun shift(move: Int): Zone {
        if (move == 0) return this

        val direction: (Zone) -> Zone = if (move < 0) {
            { it.previous }
        } else {
            { it.next }
        }

        var zone = this

        repeat(abs(move)) {
            zone = direction(zone)
        }

        return zone
    }

    open suspend fun onTakeTurn(piece: Piece) = Unit
    open suspend fun onTryLeave(piece: Piece, diceResult: List<Int>) = Unit
    open suspend fun navigate(journey: Journey): Boolean = true
    open suspend fun onLeave(journey: Journey) = Unit
    open suspend fun onPass(journey: Journey) = Unit
    open suspend fun onArrive(journey: Journey) = Unit


}