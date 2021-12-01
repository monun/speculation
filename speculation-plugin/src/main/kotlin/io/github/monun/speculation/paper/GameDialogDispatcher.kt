package io.github.monun.speculation.paper

import io.github.monun.heartbeat.coroutines.Heartbeat
import io.github.monun.heartbeat.coroutines.Suspension
import io.github.monun.speculation.game.Piece
import io.github.monun.speculation.game.dialog.GameDialogDice
import io.github.monun.speculation.game.dialog.GameDialogUpgrade
import io.github.monun.speculation.game.message.GameMessage
import io.github.monun.tap.math.toRadians
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.util.Vector
import java.time.Duration

class GameDialogDispatcher {
    private lateinit var process: PaperGameProcess

    var currentDialog: Dialog? = null
        get() = field?.takeIf { !it.isDisposed }
        private set

    fun register(process: PaperGameProcess) {
        this.process = process
        process.game.dialogAdapter.apply {
            register(GameDialogDice::class.java, ::dice)
            register(GameDialogUpgrade::class.java, ::upgrade)
        }
    }

    fun disposeCurrentDialog() {
        currentDialog?.run {
            dispose()
            currentDialog = null
        }
    }

    private fun newDialog(piece: Piece, init: Dialog.() -> Unit): Dialog {
        disposeCurrentDialog()

        return Dialog(piece.attachment()).apply(init).also {
            currentDialog = it
        }
    }

    private suspend fun dice(diceDialog: GameDialogDice): List<Int> {
        return withContext(Dispatchers.Heartbeat) {
            process.clearDices()

            val paperPiece: PaperPiece = diceDialog.piece.attachment()
            val location =
                paperPiece.player?.location?.apply { y += 2.5 } ?: PaperGameConfig.center.toLocation(process.world)
            val numberOfDice = diceDialog.numberOfDice
            val dices = List(diceDialog.numberOfDice) {
                process.spawnDice(
                    location,
                    paperPiece,
                    Vector(2.0, 2.5, 0.0).rotateAroundY((360.0 / numberOfDice).toRadians() * it)
                )
            }

            newDialog(diceDialog.piece) {
                message {
                    Component.text("보드를 클릭해 주사위를 굴리세요!")
                }
                button(PaperGameConfig.centerBox) {
                    onClick { player, _, _ ->
                        val velocity = player.location.direction
                        dices.forEach { dice ->
                            dice.roll(
                                velocity.clone()
                                    .add(Vector.getRandom().subtract(Vector(0.5, 0.5, 0.5)).normalize().multiply(0.2))
                            )
                        }
                        disposeCurrentDialog()
                    }
                }
                timeout(Component.text("주사위"), 15L * 1000L) {
                    dices.filter { it.isBeforeRoll }.forEach { it.roll(Vector(0.0, 1.0, 0.0)) }
                }
            }

            val sus = Suspension()

            while (isActive) {
                val player = paperPiece.player

                if (player == null) {
                    // 플레이어가 없을때 위로 모든 주사위를 굴림
                    dices.filter { it.isBeforeRoll }.forEach { it.roll(Vector(0.0, 1.0, 0.0)) }
                }
                // 모든 주사위가 완료되면 탈출
                if (dices.all { it.isOnGround }) break
                sus.delay(50L)
            }

            dices.forEach { it.remove(60) }

            val title = Component.text()
            if (dices.count() > 0 && dices.all { it.value == dices.first().value } && diceDialog.message == GameMessage.ROLL_THE_DICE) {
                title.append(Component.text("더블! ").decorate(TextDecoration.BOLD))
            }
            dices.forEachIndexed { index, dice ->
                if (index != 0) title.append(Component.text(" + "))
                title.append(Component.text(dice.value))
            }
            process.world.showTitle(
                Title.title(
                    title.build(),
                    paperPiece.name,
                    Title.Times.of(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(200))
                )
            )
            sus.delay(1000L)
            dices.map { it.value }
        }
    }

    private suspend fun upgrade(upgradeDialog: GameDialogUpgrade): Boolean {
        val piece = upgradeDialog.piece
        val property = upgradeDialog.property
        val level = upgradeDialog.level

        val paperPiece = piece.attachment<PaperPiece>()
        val paperProperty = property.attachment<PaperZoneProperty>()
        val channel = Channel<Boolean>()

        withContext(Dispatchers.Heartbeat) {
            if (paperPiece.player == null) {
                channel.trySend(false)
                return@withContext
            }

            newDialog(upgradeDialog.piece) {
                val info = when (val value = upgradeDialog.level.value) {
                    0 -> "땅" to "구입"
                    1 -> "빌라" to "건설"
                    2 -> "빌딩" to "건설"
                    3 -> "호텔" to "건설"
                    4 -> "랜드마크" to "건설"
                    else -> error("Unknown property level $value")
                }

                message {
                    Component.text("부동산을 클릭하여 업그레이드하세요")
                }
                actionMessage {
                    val text = Component.text()
                    text.append(Component.text(info.first))
                    text.append(Component.space())
                    text.append(Component.text(info.second))
                    text.append(Component.space())
                    text.append(Component.text("비용: "))
                    text.append(Component.text(level.costs))
                    text.build()
                }
                button(paperProperty.box) {
                    actionMessage {
                        Component.text("확인")
                    }
                    onClick { _, _, _ ->
                        channel.trySend(true)
                        disposeCurrentDialog()
                    }
                }
                button(PaperGameConfig.centerBox) {
                    actionMessage {
                        Component.text("취소")
                    }
                    onClick { _, _, _ ->
                        channel.trySend(false)
                        disposeCurrentDialog()
                    }
                }
                timeout(Component.text("부동산 업그레이드"), 15L * 1000L) {
                    channel.trySend(false)
                }
            }
        }

        return channel.receive()
    }
}