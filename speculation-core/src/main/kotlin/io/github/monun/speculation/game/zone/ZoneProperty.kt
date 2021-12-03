package io.github.monun.speculation.game.zone

import io.github.monun.speculation.game.Journey
import io.github.monun.speculation.game.Piece
import io.github.monun.speculation.game.dialog.GameDialogAcquisition
import io.github.monun.speculation.game.dialog.GameDialogUpgrade
import io.github.monun.speculation.game.event.*
import io.github.monun.speculation.game.message.GameMessage
import java.util.concurrent.ConcurrentHashMap

class ZoneProperty : Zone() {

    var owner: Piece? = null
        internal set

    var level = 0
        internal set

    val levelFlag = Level(0)
    val levelVilla = Level(1)
    val levelBuilding = Level(2)
    val levelHotel = Level(3)
    val levelLandmark = Level(4)
    val levels = listOf(levelFlag, levelVilla, levelBuilding, levelHotel, levelLandmark)

    private val _amplifiers = ConcurrentHashMap<Any, Double>()

    val amplifiers: Map<Any, Double>
        get() = _amplifiers

    val amplifierValue
        get() = if (_amplifiers.isEmpty()) 1.0 else _amplifiers.values.sum()

    /**
     * 총 통행료
     */
    val tolls: Int
        get() {
            if (owner == null) return 0

            var tolls = 0
            for (level in levels) {
                if (!level.condition) break

                tolls += level.tolls
            }

            return (tolls * amplifierValue).toInt()
        }

    /**
     * 총 자산 가치 = 총 비용 x 0.5
     */
    val assets: Int
        get() {
            if (owner == null) return 0

            var assets = 0
            for (level in levels) {
                if (!level.condition) break
                assets += level.costs
            }
            return assets.shr(2)
        }

    /**
     * 인수비용 x1.5
     */
    val acquisitionCosts: Int
        get() {
            if (owner == null) return 0

            var amount = 0
            for (level in levels) {
                if (!level.condition) break
                amount += level.costs
            }
            return amount + amount.shr(1)
        }

    internal suspend fun addAmplifier(owner: Any, amplifier: Double, piece: Piece) {
        _amplifiers[owner] = amplifier
        board.game.eventAdapter.call(PropertyAddAmplifierEvent(this, owner, amplifier, piece))
    }

    internal suspend fun removeAmplifier(owner: Any, piece: Piece): Double? {
        return _amplifiers.remove(owner)?.also {
            board.game.eventAdapter.call(PropertyRemoveAmplifierEvent(this, owner, it, piece))
        }
    }

    fun hasAmplifier(owner: Any): Boolean {
        return _amplifiers.containsKey(owner)
    }

    override suspend fun onArrive(journey: Journey) {
        val piece = journey.piece
        owner?.let { owner ->
            if (owner.isFriendly(piece)) return@let

            piece.transfer(tolls, owner, this)
            piece.ensureAlive()

            val acquisitionCosts = acquisitionCosts

            if (piece.balance >= acquisitionCosts) {
                if (piece.request(GameDialogAcquisition(this, acquisitionCosts), GameMessage.ACQUISITION) { false }) {
                    piece.transfer(tolls, owner, this)
                    this.owner = piece

                    board.game.eventAdapter.call(PropertyAcquisitionEvent(this, owner, piece))
                }
            }
        }

        val owner = owner

        if (owner == null || owner.isFriendly(piece)) {
            val preLevel = level

            for (level in levels) {
                if (level.condition) continue // 이미 지어져 있다면 다음으로
                if (level.value > piece.level) break // 말의 레벨이 레벨의 값보다 작을때 종료 (주회)
                if (level == levelLandmark && preLevel != levelHotel.value) break // 랜드마크 업그레이트 루틴 시작시 레벨이 호텔이 아니라면 종료

                val costs = level.costs
                if (costs > piece.balance || !piece.request(
                        GameDialogUpgrade(this, level),
                        GameMessage.UPGRADE
                    ) { false }
                ) {
                    break
                }

                piece.withdraw(costs, this)
                upgrade(owner ?: piece, piece, level.value)
            }
        }
    }

    suspend fun upgrade(owner: Piece, piece: Piece, level: Int) {
        this.owner = owner
        this.level = level

        board.game.eventAdapter.call(PropertyUpgradeEvent(this@ZoneProperty, levels[level], owner, piece))
    }

    suspend fun clear() {
        val owner = owner
        val level = this.level

        this.owner = null
        this.level = 0

        if (owner != null) board.game.eventAdapter.call(PropertyClearEvent(this, owner, level))
    }

    fun initLevels(baseTolls: Int) {
        fun Level.init(tolls: Int) {
            this.costs = tolls
            this.tolls = tolls
        }

        levelFlag.init(baseTolls)
        levelVilla.init(baseTolls)
        levelBuilding.init(baseTolls + baseTolls / 2)
        levelHotel.init(baseTolls * 2)
        levelLandmark.init(baseTolls * 2 + baseTolls / 2)
    }

    inner class Level(val value: Int) {
        var tolls = 0
            internal set

        var costs = 0
            internal set

        val condition: Boolean
            get() = owner != null && value <= level
    }
}