package io.github.monun.speculation.game

import io.github.monun.speculation.game.dialog.GameDialogAdapter
import io.github.monun.speculation.game.dialog.GameDialogDice
import io.github.monun.speculation.game.event.GameEventAdapter
import io.github.monun.speculation.game.event.GameOverEvent
import io.github.monun.speculation.game.event.PieceBankruptEvent
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
            val piecesByTeam = board.pieces.values.groupByTo(linkedMapOf(), Piece::team).values.toMutableList().onEach {
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
            try {
                while (isActive) {
                    if (turnQueue.isEmpty()) turnQueue.addAll(turns.filter { !it.isBankrupt })

                    val piece = turnQueue.remove()
                    currentTurn = piece

                    try {
                        val from = piece.zone

                        from.onTakeTurn(piece)
                        val diceResult = piece.request(
                            GameDialogDice(2),
                            GameMessage.ROLL_THE_DICE
                        ) {
                            List(2) {
                                1 + Random.nextInt(6)
                            }
                        }

                        from.onTryLeave(piece, diceResult)
                        piece.moveTo(from.shift(diceResult.sum()), Movement.FORWARD, MovementCause.DICE, piece)

                    } catch (turnOver: TurnOverException) {
                        eventAdapter.call(PieceTurnOverEvent(turnOver.piece))
                    } catch (bankrupt: BankruptException) {
                        eventAdapter.call(PieceBankruptEvent(bankrupt.piece))
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
}

enum class GameState {
    NEW,
    ACTIVE,
    COMPLETED,
    CANCELLED
}