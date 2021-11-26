package io.github.monun.speculation.game.zone

import io.github.monun.speculation.game.Journey

class ZoneStart : Zone() {
    override suspend fun onPass(journey: Journey) {
        journey.piece.deposit(100, this)
    }

    override suspend fun onArrive(journey: Journey) {
        journey.piece.deposit(200, this)
    }
}