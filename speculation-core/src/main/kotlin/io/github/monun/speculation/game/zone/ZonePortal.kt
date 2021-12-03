package io.github.monun.speculation.game.zone

import io.github.monun.speculation.game.Journey
import io.github.monun.speculation.game.Movement
import io.github.monun.speculation.game.MovementCause
import io.github.monun.speculation.game.Piece
import io.github.monun.speculation.game.dialog.GameDialogTargetZone
import io.github.monun.speculation.game.exception.TurnOverException
import io.github.monun.speculation.game.message.GameMessage

class ZonePortal : Zone() {
    override suspend fun onTakeTurn(piece: Piece) {
        val zones = board.zones.filter { it != this }
        var to = piece.request(GameDialogTargetZone(zones), GameMessage.ZONE_FOR_PORTAL) {
            zones.random()
        }

        if (to == this) to = zones.random()
        piece.moveTo(to, Movement.FORWARD, MovementCause.PORTAL, piece)
    }

    override suspend fun onArrive(journey: Journey) {
        throw TurnOverException()
    }
}