package io.github.monun.speculation.game.zone

import io.github.monun.speculation.game.Journey
import io.github.monun.speculation.game.dialog.GameDialogTax
import io.github.monun.speculation.game.message.GameMessage
import kotlin.math.max
import kotlin.random.Random

class ZoneNTS: Zone() {
    override suspend fun onArrive(journey: Journey) {
        val piece = journey.piece
        val balance = piece.balance

        if (balance <= 0) return

        val amount = piece.request(GameDialogTax(piece.balance, this), GameMessage.TAX) {
            Random.nextInt(balance + 1)
        }

        if (amount > 0) {
            piece.withdraw(amount, this)

            val others = piece.board.survivors.filter { it != piece }

            if (others.isNotEmpty()) {
                val refund = max(1, amount / others.count())
                others.forEach { it.deposit(refund, this) }
            }
        }
    }
}