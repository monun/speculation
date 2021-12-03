package io.github.monun.speculation.game.zone

import io.github.monun.speculation.game.Journey
import io.github.monun.speculation.game.Piece
import io.github.monun.speculation.game.event.PieceJailbreakEvent
import io.github.monun.speculation.game.exception.TurnOverException

class ZoneJail : Zone() {
    companion object {
        const val count = 3
    }

    override suspend fun onTryLeave(piece: Piece, diceResult: List<Int>) {
        if (piece.jailCount > 0) {
            val value = diceResult.first()
            // 모든 주사위가 같다면
            if (diceResult.all { it == value }) {
                board.game.eventAdapter.call(PieceJailbreakEvent(piece, piece.jailCount, true))
                return
            }

            board.game.eventAdapter.call(PieceJailbreakEvent(piece, piece.jailCount, false))
            piece.jailCount--
            throw TurnOverException()
        }

        board.game.eventAdapter.call(PieceJailbreakEvent(piece, 0, true))
    }

    override suspend fun onArrive(journey: Journey) {
        journey.piece.jailCount = count
        throw TurnOverException()
    }

    override suspend fun onLeave(journey: Journey) {
        journey.piece.jailCount = 0
    }
}