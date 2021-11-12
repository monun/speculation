package io.github.monun.speculation.game

import io.github.monun.speculation.ref.upstream

class Board(game: Game) {
    internal val game = upstream(game)

    private val _pieces = LinkedHashMap<String, Piece>()

    private val _teams = LinkedHashMap<String, PieceTeam>()

    fun newPiece(name: String): Piece {
        game.checkState(GameState.NEW)
        require(name !in _pieces) { "Already registered piece name '$name'" }

        return Piece(this, name).also {
            _pieces[name] = it
        }
    }

    fun newTeam(name: String, members: List<Piece>): PieceTeam {
        game.checkState(GameState.NEW)
        require(name !in _teams) { "Already registered team name '$name'" }
        require(members.all { it.team == null }) { "Already piece has team" }

        return PieceTeam(name, members.toList()).also {
            _teams[name] = it

            for (member in members) {
                member.team = it
            }
        }
    }
}