package io.github.monun.speculation.game.exception

import io.github.monun.speculation.game.Piece

class GameOverException(val winner: Piece? = null): Exception() {
}