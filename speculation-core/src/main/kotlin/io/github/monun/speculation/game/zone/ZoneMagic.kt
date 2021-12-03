package io.github.monun.speculation.game.zone

import io.github.monun.speculation.game.Journey
import io.github.monun.speculation.game.Movement
import io.github.monun.speculation.game.MovementCause
import io.github.monun.speculation.game.Piece
import io.github.monun.speculation.game.dialog.GameDialogDice
import io.github.monun.speculation.game.dialog.GameDialogMagic
import io.github.monun.speculation.game.dialog.GameDialogTargetPiece
import io.github.monun.speculation.game.dialog.GameDialogTargetZone
import io.github.monun.speculation.game.message.GameMessage
import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random

class ZoneMagic : Zone() {
    companion object {
        private val magics = Magic::class.nestedClasses.map { it.objectInstance as Magic }
    }

    override suspend fun onArrive(journey: Journey) {
        val piece = journey.piece
        val magic = piece.request(GameDialogMagic(magics), GameMessage.MAGIC) { magics.random() }

        magic.dispatch(this, piece)
    }
}

sealed interface Magic {
    suspend fun dispatch(zone: ZoneMagic, piece: Piece)

    // 긴급체포: 감옥으로 즉시이동
    object Arrest : Magic {
        override suspend fun dispatch(zone: ZoneMagic, piece: Piece) {
            piece.moveTo(zone.board.zoneJail, Movement.TELEPORT, MovementCause.MAGIC, piece)
        }
    }

    // 폭풍우: 무작위 위치로 이동
    object Storm : Magic {
        override suspend fun dispatch(zone: ZoneMagic, piece: Piece) {
            val to = piece.board.zones.filter { it !is ZoneMagic }.random()
            piece.moveTo(to, Movement.TELEPORT, MovementCause.MAGIC, piece)
        }
    }

    // 밀치기: 지정한 대상을 한칸 밀침
    object Push : Magic {
        override suspend fun dispatch(zone: ZoneMagic, piece: Piece) {
            val target = piece.request(
                GameDialogTargetPiece(piece.board.pieces),
                GameMessage.PIECE_FOR_PUSH
            ) {
                piece.board.pieces.random()
            }

            target.runCatching {
                moveTo(target.zone.shift(Movement.REVERSE), Movement.REVERSE, MovementCause.MAGIC, piece)
            }.onFailure { exception ->
                if (target == piece) throw exception
            }
        }
    }

    // 천벌: 감옥에 있는 무작위 대상을 자신의 무작위 부동산으로 소환
    object Punishment : Magic {
        override suspend fun dispatch(zone: ZoneMagic, piece: Piece) {
            piece.properties.randomOrNull()?.let { property ->
                val target = piece.board.zoneJail.pieces.randomOrNull() ?: return
                target.runCatching {
                    moveTo(property, Movement.TELEPORT, MovementCause.MAGIC, piece)
                }.onFailure {
                    if (target == piece) throw it
                }
            }
        }
    }

    // 바가지요금: 지정한 자신의 부동산의 통행료를 x2배 상승
    object Overprice : Magic {
        override suspend fun dispatch(zone: ZoneMagic, piece: Piece) {
            val properties = piece.properties
            if (properties.isEmpty()) return

            piece.request(GameDialogTargetZone(properties), GameMessage.ZONE_FOR_OVERPRICE) {
                properties.random()
            }.let { target ->
                (target as ZoneProperty).addAmplifier(this, 2.0, piece)
            }
        }
    }

    // 문워크: 다시 한번 주사위를 굴려 뒤로 이동
    object Moonwalk : Magic {
        override suspend fun dispatch(zone: ZoneMagic, piece: Piece) {
            val diceResult = piece.request(
                GameDialogDice(2),
                GameMessage.ROLL_THE_DICE_FOR_MOONWALK
            ) {
                List(2) {
                    1 + Random.nextInt(6)
                }
            }

            piece.zone.onTryLeave(piece, diceResult)
            piece.moveTo(piece.zone.shift(-diceResult.sum()), Movement.REVERSE, MovementCause.DICE, piece)
        }
    }

    // 싱글: 다음 턴 주사위는 1개를 던짐
    object SingleDice : Magic {
        override suspend fun dispatch(zone: ZoneMagic, piece: Piece) {
            piece.numberOfDice = 1
        }
    }

    // 트리플: 다음 턴 주사위는 3개를 던짐
    object TripleDice : Magic {
        override suspend fun dispatch(zone: ZoneMagic, piece: Piece) {
            piece.numberOfDice = 3
        }
    }

    // 쿼드러플: 다음 턴 주사위는 4개를 던짐
    object QuadrupleDice : Magic {
        override suspend fun dispatch(zone: ZoneMagic, piece: Piece) {
            piece.numberOfDice = 4
        }
    }

    // 서울구경: 서울로 즉시 이동
    object MoveToSeoul : Magic {
        override suspend fun dispatch(zone: ZoneMagic, piece: Piece) {
            piece.moveTo(piece.board.zones.last(), Movement.TELEPORT, MovementCause.MAGIC, piece)
        }
    }

    // 초심으로: 시작지점으로 이동
    object MoveToStart : Magic {
        override suspend fun dispatch(zone: ZoneMagic, piece: Piece) {
            piece.moveTo(piece.board.zoneStart, Movement.FORWARD, MovementCause.MAGIC, piece)
        }
    }

    // 부동산 증여: 지정한 자신의 부동산을 대상에게 전달
    object GiftProperty : Magic {
        override suspend fun dispatch(zone: ZoneMagic, piece: Piece) {
            val properties = piece.properties; if (properties.isEmpty()) return
            val pieces = piece.board.survivors.filter { it != piece }; if (pieces.isEmpty()) return

            val targetProperty = piece.request(
                GameDialogTargetZone(properties),
                GameMessage.PIECE_FOR_GIFT_PROPERTY
            ) { properties.random() } as ZoneProperty
            val targetPiece =
                piece.request(GameDialogTargetPiece(pieces), GameMessage.PIECE_FOR_GIFT_PROPERTY) { pieces.random() }

            targetProperty.update(targetPiece to targetProperty.level)
        }
    }

    // 소매치기: 주사위를 한개를 던져 지나치는 적의 돈을 훔침
    object Pickpocket : Magic {
        override suspend fun dispatch(zone: ZoneMagic, piece: Piece) {
            val diceResult = piece.request(
                GameDialogDice(1),
                GameMessage.ROLL_THE_DICE
            ) {
                List(1) {
                    1 + Random.nextInt(6)
                }
            }

            piece.zone.onTryLeave(piece, diceResult)
            piece.moveTo(
                piece.zone.shift(diceResult.sum()),
                Movement.FORWARD,
                MovementCause.DICE,
                piece
            ) { _, movedZone ->
                movedZone.pieces.filter { !it.isFriendly(piece) }.forEach { enemy ->
                    val amount = (Random.nextDouble().pow(2) * enemy.balance).toInt()

                    if (amount > 0) {
                        enemy.transfer(amount, piece, movedZone)
                    }
                }
            }
        }
    }

    // 지진: 지정한 땅의 등급을 한단계 낮춤
    object Earthquake : Magic {
        override suspend fun dispatch(zone: ZoneMagic, piece: Piece) {
            val properties = piece.board.zoneProperties.filter {
                val owner = it.owner
                owner != null && owner != piece
            }
            if (properties.isEmpty()) return

            piece.request(GameDialogTargetZone(properties), GameMessage.ZONE_FOR_EARTHQUAKE) {
                properties.random()
            }.let {
                val property = (it as ZoneProperty)
                val level = property.level

                if (level <= 0) property.update(null)
                else property.update(property.owner!! to max(0, level - 1))
            }
        }
    }

    // 천사: 1회 통행료 무료
    object Angel : Magic {
        override suspend fun dispatch(zone: ZoneMagic, piece: Piece) {
            piece.hasAngel = true
        }
    }
}