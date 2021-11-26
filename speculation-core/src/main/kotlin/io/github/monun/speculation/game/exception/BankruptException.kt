package io.github.monun.speculation.game.exception

import io.github.monun.speculation.game.Piece

class BankruptException(val piece: Piece): Exception()