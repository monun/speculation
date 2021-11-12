package io.github.monun.speculation.game.zone

import io.github.monun.speculation.game.Journey
import io.github.monun.speculation.game.Piece

abstract class Zone {
    open suspend fun onTakeTurn(piece: Piece) = Unit
    open suspend fun onRollDice(piece: Piece, diceResult: Pair<Int, Int>) = Unit
    open suspend fun onLeave(piece: Piece, journey: Journey) = Unit
    open suspend fun onPass(piece: Piece, journey: Journey) = Unit
    open suspend fun onArrive(piece: Piece, journey: Journey) = Unit
}