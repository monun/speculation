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
    private var visitCount = 0

    override suspend fun onArrive(journey: Journey) {
        visitCount++
        val piece = journey.piece
        if (piece.balance <= 0) return

        val maxBetting = min(50 + (visitCount - 1) * 10, piece.balance)
        var betting = piece.request(GameDialogBetting(maxBetting), GameMessage.BETTING) { 0 }

        if (betting > 0) {
            betting = min(betting, maxBetting)
            val gamblers = piece.board.survivors.toMutableList().apply {
                shuffle()
                remove(piece)
                add(piece) // 주최자가 마지막에 주사위를 던짐
            }

            board.game.eventAdapter.call(PieceGambleStartEvent(piece, betting, gamblers))

            val results = gamblers.associateWith { gambler ->
                gambler.request(GameDialogDice(3), GameMessage.ROLL_THE_DICE_FOR_GAMBLE) { List(3) { 1 } }.sum()
            }

            val maxValue = results.maxOf { it.value }
            val winners = results.filter { it.value == maxValue }
            val losers = results.filter { it.key !in winners }

            var totalPrize = 0

            for (loser in losers.keys) {
                totalPrize += loser.withdraw(betting, this)
            }

            val prizePerWinner = max(1, totalPrize / winners.count())

            board.game.eventAdapter.call(
                PieceGambleEndEvent(
                    winners.keys.toList(),
                    losers.keys.toList(),
                    prizePerWinner
                )
            )

            for (winner in winners.keys) {
                winner.deposit(prizePerWinner, this)
            }

            board.game.checkGameOver()
        }
    }
}