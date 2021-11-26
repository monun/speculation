package io.github.monun.speculation.game.event

import io.github.monun.speculation.game.Journey
import io.github.monun.speculation.game.Piece
import io.github.monun.speculation.game.zone.Zone

interface GameEvent

abstract class PieceEvent(val piece: Piece): GameEvent
abstract class ZoneEvent(val piece: Piece): GameEvent

class PieceTakeTurnEvent(piece: Piece) : PieceEvent(piece)
class PieceTurnOverEvent(piece: Piece) : PieceEvent(piece)
class PieceDepositEvent(piece: Piece, val amount: Int, val source: Any?): PieceEvent(piece)
class PieceTransferEvent(piece: Piece, val amount: Int, val receiver: Piece, val source: Any?): PieceEvent(piece)
class PieceWithdrawEvent(piece: Piece, val amount: Int, val source: Any?): PieceEvent(piece)
class PieceBankruptEvent(piece: Piece) : PieceEvent(piece)
class PieceMoveEvent(piece: Piece, val journey: Journey, val zone: Zone): PieceEvent(piece)

class GameOverEvent(): GameEvent

