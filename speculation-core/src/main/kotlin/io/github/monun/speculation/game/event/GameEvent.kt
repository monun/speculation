package io.github.monun.speculation.game.event

import io.github.monun.speculation.game.Journey
import io.github.monun.speculation.game.Piece
import io.github.monun.speculation.game.zone.Zone
import io.github.monun.speculation.game.zone.ZoneProperty

interface GameEvent

abstract class PieceEvent(val piece: Piece): GameEvent
class PieceTakeTurnEvent(piece: Piece) : PieceEvent(piece)
class PieceTurnOverEvent(piece: Piece) : PieceEvent(piece)
class PieceDepositEvent(piece: Piece, val amount: Int, val zone: Zone): PieceEvent(piece)
class PieceTransferEvent(piece: Piece, val amount: Int, val receiver: Piece, val zone: Zone): PieceEvent(piece)
class PieceWithdrawEvent(piece: Piece, val amount: Int, val zone: Zone): PieceEvent(piece)
class PieceBankruptEvent(piece: Piece) : PieceEvent(piece)
class PieceMoveEvent(piece: Piece, val journey: Journey, val from: Zone, val to: Zone): PieceEvent(piece)

abstract class PropertyEvent(val property: ZoneProperty): GameEvent
//class PropertyUpdateEvent(property: ZoneProperty) : PropertyEvent(property)
class PropertyUpgradeEvent(property: ZoneProperty, val level: ZoneProperty.Level, val piece: Piece) : PropertyEvent(property)
class PropertyAcquisitionEvent(property: ZoneProperty, val from: Piece, val to: Piece) : PropertyEvent(property)
class PropertyClearEvent(property: ZoneProperty, val oldOwner: Piece, val oleLevel: Int) : PropertyEvent(property)

class GameOverEvent(): GameEvent

