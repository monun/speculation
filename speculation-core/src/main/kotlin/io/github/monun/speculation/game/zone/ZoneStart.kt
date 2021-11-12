package io.github.monun.speculation.game.zone

import io.github.monun.speculation.game.Journey
import io.github.monun.speculation.game.Piece

class ZoneStart : Zone() {
    override suspend fun onPass(piece: Piece, journey: Journey) {
        TODO("돈 주기")
    }

    override suspend fun onArrive(piece: Piece, journey: Journey) {
        TODO("돈 두배 주기")
    }
}