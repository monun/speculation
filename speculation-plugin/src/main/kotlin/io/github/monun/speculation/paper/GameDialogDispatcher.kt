package io.github.monun.speculation.paper

import io.github.monun.heartbeat.coroutines.Heartbeat
import io.github.monun.heartbeat.coroutines.Suspension
import io.github.monun.speculation.game.Piece
import io.github.monun.speculation.game.dialog.GameDialogDice
import io.github.monun.speculation.game.message.GameMessage
import kotlinx.coroutines.Dispatchers
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
        }
    }

    private fun disposeCurrentDialog() {
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
            val paperPiece: PaperPiece = diceDialog.piece.attachment()
            val location =
                paperPiece.player?.location?.apply { y += 2.5 } ?: PaperGameConfig.center.toLocation(process.world)
            val dices = List(diceDialog.numberOfDice) {
                Dice(process.fakeEntityServer, location)
            }

            newDialog(diceDialog.piece) {
                message {
                    Component.text("보드를 클릭해 주사위를 굴리세요!")
                }
                button(PaperGameConfig.centerBox) { player, _, _ ->
                    val velocity = player.location.direction
                    dices.forEach { dice ->
                        dice.roll(velocity.clone().add(Vector.getRandom().normalize().multiply(0.2)))
                    }
                    disposeCurrentDialog()
                }
            }

            val sus = Suspension()

            while (isActive) {
                val player = paperPiece.player

                if (player == null) {
                    // 플레이어가 없을때 무작위 방향으로 모든 주사위를 굴림
                    dices.filter { !it.isRolled }
                        .forEach { it.roll(Vector.getRandom().subtract(Vector(0.5, 0.5, 0.5)).normalize()) }
                } else {
                    // 플레이어가 있을때 굴리지 않은 주사위를 플레이어 위로 이동
                    val movedLocation = player.location.apply { y += 2.5 }
                    dices.filter { !it.isRolled }.forEach { it.moveTo(movedLocation) }
                }

                // 모든 주사위가 완료되면 탈출
                if (dices.all { it.isOnGround }) break

                sus.delay(50L)
            }

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

}