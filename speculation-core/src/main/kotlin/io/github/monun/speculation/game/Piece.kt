package io.github.monun.speculation.game

import io.github.monun.speculation.ref.upstream

class Piece(board: Board, val name: String) {
    val board = upstream(board)

    var team: PieceTeam? = null

    val hasTurn: Boolean
        get() = board.game.currentTurn === this

    fun isFriendly(piece: Piece): Boolean {
        if (piece === this) return true

        val team = team
        return team != null && team === piece.team
    }
}