package io.github.monun.speculation.game

class PieceTeam(val name: String, val pieces: List<Piece>) {
    val survivors
        get() = pieces.filter { !it.isBankrupt}
}