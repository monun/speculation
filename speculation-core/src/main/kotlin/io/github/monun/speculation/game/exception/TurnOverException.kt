package io.github.monun.speculation.game.exception

import io.github.monun.speculation.game.Piece
import java.lang.Exception

class TurnOverException(val piece: Piece): Exception()