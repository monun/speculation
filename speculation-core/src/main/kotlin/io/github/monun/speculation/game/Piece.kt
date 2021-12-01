package io.github.monun.speculation.game

import io.github.monun.speculation.game.dialog.GameDialog
import io.github.monun.speculation.game.dialog.GameDialogSeizure
import io.github.monun.speculation.game.event.PieceDepositEvent
import io.github.monun.speculation.game.event.PieceMoveEvent
import io.github.monun.speculation.game.event.PieceTransferEvent
import io.github.monun.speculation.game.event.PieceWithdrawEvent
import io.github.monun.speculation.game.exception.BankruptException
import io.github.monun.speculation.game.message.GameMessage
import io.github.monun.speculation.game.zone.Zone
import io.github.monun.speculation.game.zone.ZoneProperty
import io.github.monun.speculation.ref.upstream
import kotlin.math.min

class Piece(board: Board, val name: String, zone: Zone) : Attachable() {

    val board = upstream(board)

    var team: PieceTeam? = null

    val hasTurn: Boolean
        get() = board.game.currentTurn === this

    var balance = 400
        private set

    var isBankrupt = false
        private set

    var zone: Zone = zone
        private set

    val properties: List<ZoneProperty>
        get() = board.zoneProperties.filter { it.owner == this }

    val assets: Int
        get() = properties.sumOf { it.assets }

    var level = 0

    fun isFriendly(piece: Piece): Boolean {
        if (piece === this) return true

        val team = team
        return team != null && team === piece.team
    }

    internal suspend fun <R> request(dialog: GameDialog<R>, message: GameMessage, default: () -> R): R {
        return board.game.dialogAdapter.request(dialog.apply {
            initialize(this@Piece, message, default)
        })
    }

    internal suspend fun moveTo(destination: Zone, movement: Movement, cause: MovementCause, source: Piece) {
        val journey = Journey(this, zone, destination, movement, cause, source).apply {
            pathfinding()
        }

        journey.from.onLeave(journey)
        var prev = journey.from

        for (zone in journey.path) {
            this.zone = zone
            board.game.eventAdapter.call(PieceMoveEvent(this, journey, prev, zone))
            zone.onPass(journey)
            journey.pass(zone)
            prev = zone
        }

        zone = journey.destination
        board.game.eventAdapter.call(PieceMoveEvent(this, journey, prev, zone))
        zone.onArrive(journey)
    }

    internal suspend fun deposit(amount: Int, source: Zone) {
        balance += amount
        board.game.eventAdapter.call(PieceDepositEvent(this, amount, source))
    }

    private fun selectProperties(target: Int): Pair<List<ZoneProperty>, List<ZoneProperty>> {
        val required = properties.toMutableList().apply { sortByDescending { it.assets } }
        val optional = arrayListOf<ZoneProperty>()

        var value = required.sumOf { it.assets }

        required.removeIf { zone ->
            val zoneAssets = zone.assets
            if (target < value - zoneAssets) {
                value -= zoneAssets
                optional.add(zone)
                true
            } else false
        }

        return required to optional
    }

    private suspend fun withdraw(requestAmount: Int): Int {
        if (balance < requestAmount) {
            val assets = assets
            val total = balance + assets

            if (total < requestAmount) {
                isBankrupt = true

                for (property in properties) {
                    property.clear()
                }

                balance = total
            } else {
                val target = requestAmount - balance

                selectProperties(target).let { (required, optional) ->
                    if (optional.isEmpty()) {
                        for (property in properties) property.clear()
                        balance = total
                    } else {
                        val selected = request(
                            GameDialogSeizure(requestAmount),
                            GameMessage.SEIZURE
                        ) { required }.filter { it.owner == this }

                        for (zone in selected) {
                            balance += zone.assets
                            zone.clear()
                        }

                        // 부족한 금액 메꾸기
                        if (balance < requestAmount) {
                            for (zone in selectProperties(requestAmount - balance).first) {
                                balance += zone.assets
                                zone.clear()
                            }
                        }
                    }
                }
            }
        }

        return min(balance, requestAmount).also { balance -= it }
    }

    internal suspend fun withdraw(requestAmount: Int, source: Zone) {
        val amount = withdraw(requestAmount)

        board.game.eventAdapter.call(PieceWithdrawEvent(this, amount, source))
        ensureAlive()
    }

    internal suspend fun transfer(requestAmount: Int, receiver: Piece, zone: Zone) {
        val amount = withdraw(requestAmount)
        receiver.balance += amount

        board.game.eventAdapter.call(PieceTransferEvent(this, amount, receiver, zone))
        ensureAlive()
    }

    private fun ensureAlive() {
        if (isBankrupt) throw BankruptException(this)
    }


}