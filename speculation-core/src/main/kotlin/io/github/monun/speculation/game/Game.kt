package io.github.monun.speculation.game

import io.github.monun.speculation.game.dialog.GameDialogAdapter
import io.github.monun.speculation.game.dialog.GameDialogDice
import io.github.monun.speculation.game.event.GameEventAdapter
import io.github.monun.speculation.game.event.GameOverEvent
import io.github.monun.speculation.game.event.PieceTakeTurnEvent
import io.github.monun.speculation.game.event.PieceTurnOverEvent
import io.github.monun.speculation.game.exception.BankruptException
import io.github.monun.speculation.game.exception.GameOverException
import io.github.monun.speculation.game.exception.TurnOverException
import io.github.monun.speculation.game.message.GameMessage
import kotlinx.coroutines.*
import java.util.*
import kotlin.random.Random

class Game {
    val board: Board = Board(this)

    val dialogAdapter = GameDialogAdapter()
    val eventAdapter = GameEventAdapter()

    var state: GameState = GameState.NEW
        private set

    private lateinit var turns: List<Piece>

    private lateinit var turnQueue: Queue<Piece>

    lateinit var currentTurn: Piece
        private set

    fun launch(scope: CoroutineScope, dispatcher: CoroutineDispatcher = Dispatchers.Default) {
        require(state == GameState.NEW) { "state != NEW" }

        state = GameState.ACTIVE

        // 순서 구성
        // 팀 구성이 a(3) - b(3) - c(3) 일때 무작위 교차배치
        // a b c a b c a b c a b c
        turns = arrayListOf<Piece>().apply {
            val piecesByTeam = board.survivors.groupByTo(linkedMapOf(), Piece::team).values.toMutableList().onEach {
                it.shuffle()
            }.apply { shuffle() }
            while (piecesByTeam.isNotEmpty()) {
                piecesByTeam.removeIf { pieces ->
                    pieces.removeFirstOrNull()?.let { add(it) }
                    pieces.isEmpty()
                }
            }
        }.toList()
        turnQueue = ArrayDeque()

        scope.launch(dispatcher) {
            // ======================================= debug start =======================================
//            val pieces = board.pieces.values
//            board.zoneProperties.forEach {
//                pieces.random().let { piece ->
//                        it.upgrade(piece, piece, 4)
//                }
//            }
//            pieces.forEach { it.withdraw(400, board.zones.first()) }
//            pieces.forEach { it.zone = board.zones.last() }
            // ======================================= debug end =======================================

            try {
                while (isActive) {
                    if (turnQueue.isEmpty()) turnQueue.addAll(turns.filter { !it.isBankrupt })

                    val piece = turnQueue.remove()
                    if (piece.isBankrupt) continue

                    currentTurn = piece

                    try {
                        eventAdapter.call(PieceTakeTurnEvent(piece))
                        piece.zone.onTakeTurn(piece)

                        val diceResult = piece.request(
                            GameDialogDice(2),
                            GameMessage.ROLL_THE_DICE
                        ) {
                            List(2) {
                                1 + Random.nextInt(6)
                            }
                        }

                        piece.zone.onTryLeave(piece, diceResult)
                        piece.moveTo(piece.zone.shift(diceResult.sum()), Movement.FORWARD, MovementCause.DICE, piece)

                        // ======================================= 디버그 시작 =======================================
                        // piece.moveTo(board.zoneSpecials.last(), Movement.TELEPORT, MovementCause.DICE, piece)
                        // ======================================= 디버그 끝 =======================================
                    }
                    catch (bankrupt: BankruptException) {}
                    catch (turnOver: TurnOverException) {
                        eventAdapter.call(PieceTurnOverEvent(piece))
                    }
                }
            } catch (gameOver: GameOverException) {
                eventAdapter.call(GameOverEvent())
            } catch (cancellation: CancellationException) {
                state = GameState.CANCELLED
            }

            if (state != GameState.CANCELLED) state = GameState.COMPLETED
        }
    }

    fun checkState(state: GameState) {
        require(this.state == state) { "state must be ${state.name}" }
    }

    internal fun checkGameOver() {
        val pieces = board.pieces
        val survivors = pieces.filter { !it.isBankrupt }

        if (survivors.isEmpty()) throw GameOverException()
        if (survivors.count() == 1) throw GameOverException(survivors.first())
    }
}

enum class GameState {
    NEW,
    ACTIVE,
    COMPLETED,
    CANCELLED
}