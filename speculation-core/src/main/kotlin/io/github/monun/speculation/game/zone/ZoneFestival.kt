package io.github.monun.speculation.game.zone

import io.github.monun.speculation.game.Journey
import kotlin.math.pow
import kotlin.random.Random

class ZoneFestival: Zone() {
    override suspend fun onArrive(journey: Journey) {
        val piece = journey.piece
        val properties = piece.properties

        if (properties.isEmpty()) return

        val count = 1 + (Random.nextDouble().pow(2) * 3).toInt()
        val festivals = properties.shuffled().take(count)

        board.zoneProperties.forEach { it.removeAmplifier(this, piece) }
        festivals.forEach { it.addAmplifier(this, 2.0, piece) }
    }
}