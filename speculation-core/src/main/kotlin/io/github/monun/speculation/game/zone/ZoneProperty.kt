package io.github.monun.speculation.game.zone

import io.github.monun.speculation.game.Journey
import io.github.monun.speculation.game.Piece
import io.github.monun.speculation.game.dialog.GameDialogAcquisition
import io.github.monun.speculation.game.dialog.GameDialogUpgrade
import io.github.monun.speculation.game.message.GameMessage

class ZoneProperty : Zone() {
    var owner: Piece? = null
        internal set

    var level = 0
        internal set

    // 0: 땅, 1: 빌라, 2: 빌딩, 3: 호텔, 4: 랜드마크
    val levels = List(5) {
        Level(it)
    }

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
            return tolls
        }

    /**
     * 총 자산 가치 x0.5
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

    override suspend fun onArrive(journey: Journey) {
        val piece = journey.piece
        owner?.let { owner ->
            if (owner.isFriendly(piece)) return@let

            piece.transfer(tolls, owner, this)

            val acquisitionCosts = acquisitionCosts

            if (piece.balance >= acquisitionCosts) {
                if (piece.request(GameDialogAcquisition(this), GameMessage.ACQUISITION) { false }) {
                    piece.transfer(tolls, owner, this)
                    this.owner = piece
                }
            }
        }

        val owner = owner

        if (owner == null || owner.isFriendly(piece)) {
            for (level in levels) {
                if (level.condition) continue

                val costs = level.costs
                if (costs > piece.balance || !piece.request(
                        GameDialogUpgrade(this, level),
                        GameMessage.UPGRADE
                    ) { false }
                ) break

                piece.withdraw(tolls, this)

                if (this.owner == null) this.owner = piece
                this.level = level.value
            }
        }
    }

    fun clear() {
        owner = null
        level = 0
    }

    inner class Level(val value: Int) {
        var tolls = 0
            internal set

        var costs = 0
            internal set

        val condition: Boolean
            get() = owner != null && level <= value
    }
}