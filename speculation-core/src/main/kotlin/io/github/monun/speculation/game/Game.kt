package io.github.monun.speculation.game

import kotlinx.coroutines.*

class Game() {
    private val board: Board = Board(this)

    var state: GameState = GameState.NEW
        private set

    private lateinit var turns: List<Piece>

    lateinit var currentTurn: Piece
        private set

    fun launch(scope: CoroutineScope, dispatcher: CoroutineDispatcher = Dispatchers.Default) {
        require(state == GameState.NEW) { "state != NEW" }

        state = GameState.ACTIVE

        scope.launch(dispatcher) {

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