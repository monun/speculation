package io.github.monun.speculation.game.zone

import io.github.monun.speculation.game.Journey
import io.github.monun.speculation.game.Movement
import io.github.monun.speculation.game.MovementCause
import io.github.monun.speculation.game.Piece
import io.github.monun.speculation.game.dialog.GameDialogPortal
import io.github.monun.speculation.game.exception.TurnOverException
import io.github.monun.speculation.game.message.GameMessage

class ZonePortal: Zone() {
    override suspend fun onTakeTurn(piece: Piece) {
        val default = board.zones.filter { it != this }.random()
        var to = piece.request(GameDialogPortal(), GameMessage.PORTAL) {
            default
        }

        if (to == this) to = default
        piece.moveTo(to, Movement.FORWARD, MovementCause.PORTAL, piece)
    }

    override suspend fun onArrive(journey: Journey) {
        throw TurnOverException()
    }
}