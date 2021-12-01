package io.github.monun.speculation.game.zone

import io.github.monun.speculation.game.Journey
import io.github.monun.speculation.game.dialog.GameDialogBetting
import io.github.monun.speculation.game.dialog.GameDialogDice
import io.github.monun.speculation.game.event.PieceGambleEndEvent
import io.github.monun.speculation.game.event.PieceGambleStartEvent
import io.github.monun.speculation.game.message.GameMessage
import kotlin.math.max
import kotlin.math.min

class ZoneGamble : Zone() {
    override suspend fun onArrive(journey: Journey) {
        val piece = journey.piece
        if (piece.balance <= 0) return

        var betting = piece.request(GameDialogBetting(), GameMessage.BETTING) { 0 }

        if (betting > 0) {
            betting = min(betting, piece.balance)
            val pieces = piece.board.pieces.values.toMutableList().apply {
                shuffle()
                remove(piece)
                add(0, piece)
            }

            board.game.eventAdapter.call(PieceGambleStartEvent(piece, betting, pieces))

            val results = pieces.associateWith { gambler ->
                gambler.request(GameDialogDice(3), GameMessage.ROLL_THE_DICE_FOR_GAMBLE) { List(3) { 1 } }.sum()
            }

            val maxValue = results.maxOf { it.value }
            val winners = results.filter { it.value == maxValue }
            val losers = results.filter { it.key !in winners }

            board.game.eventAdapter.call(PieceGambleEndEvent(winners.keys.toList(), losers.keys.toList(), betting))

            var prize = 0

            for (loser in losers.keys) {
                prize += loser.withdraw(betting, this)
            }

            val prizePerWinner = max(1, prize / winners.count())
            for (winner in winners.keys) {
                winner.deposit(prizePerWinner, this)
            }
        }
    }
}