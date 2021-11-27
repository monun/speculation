package io.github.monun.speculation.paper

import io.github.monun.tap.fake.FakeEntity
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import java.time.Duration

class Dialog(val piece: PaperPiece) {
    private var message: (() -> Component)? = null

    private val buttons = arrayListOf<Button>()

    var timeout: Timeout? = null
        private set

    var isDisposed = false
        private set

    fun message(message: () -> Component) {
        if (isDisposed) return
        this.message = message
    }

    fun button(box: BoundingBox, display: Pair<Component, Location>? = null, action: (Player, Dialog, Button) -> Unit) {
        if (isDisposed) return

        buttons += Button(box, display?.let { (text, location) ->
            piece.process.fakeEntityServer.spawnEntity(location, ArmorStand::class.java).apply {
                updateMetadata<ArmorStand> {
                    customName(text)
                    isCustomNameVisible = true
                    isMarker = true
                    isInvisible = true
                }
            }
        }, action)
    }

    fun timeout(message: Component, duration: Long, action: (Dialog) -> Unit) {
        if (isDisposed) return
        this.timeout = Timeout(piece, message, duration, action)
    }

    fun interact(player: Player, start: Vector, direction: Vector, maxDistance: Double = 96.0) {
        if (isDisposed) return

        var found: Button? = null
        var foundDistance = 0.0

        for (button in buttons.toList()) {
            if (button.isDeposed) continue

            button.box.rayTrace(start, direction, maxDistance)?.let { result ->
                val distance = start.distance(result.hitPosition)

                if (found == null || distance < foundDistance) {
                    found = button
                    foundDistance = distance
                }
            }
        }

        found?.let { button ->
            button.action(player, this, button)
            buttons.removeIf { it.isDeposed }
        }
    }

    fun dispose() {
        if (isDisposed) return

        isDisposed = true
        buttons.onEach { it.dispose() }.clear()
    }

    fun onUpdate() {
        piece.player?.let { player ->
            message?.invoke()?.let { message ->
                player.showTitle(
                    Title.title(
                        Component.empty(),
                        message,
                        Title.Times.of(Duration.ofMillis(0), Duration.ofSeconds(1), Duration.ofMillis(100))
                    )
                )
            }
        }
        timeout?.let {
            // 반환값: 유효할때
            if (!it.update()) {
                dispose()
            }
        }
    }

    class Button(val box: BoundingBox, private val display: FakeEntity?, val action: (Player, Dialog, Button) -> Unit) {
        var isDeposed = false

        fun dispose() {
            if (isDeposed) return

            isDeposed = true
            display?.remove()
        }
    }

    inner class Timeout(
        piece: PaperPiece,
        message: Component,
        val duration: Long,
        val action: (Dialog) -> Unit
    ) {
        fun currentTime() = System.nanoTime()

        private val time: Long = currentTime() + duration * 1000000L

        private val progressBar =
            BossBar.bossBar(piece.name.append(message), 1.0F, piece.color.group.barColor, BossBar.Overlay.PROGRESS)

        fun showProgressBar(players: Collection<Player> = Bukkit.getOnlinePlayers()) {
            players.forEach { it.showBossBar(progressBar) }
        }

        fun hideProgressBar() {
            Bukkit.getOnlinePlayers().forEach { it.hideBossBar(progressBar) }
        }

        fun update(): Boolean {
            val currentTime = currentTime()

            return if (currentTime < time) {
                val remaining = time - currentTime
                val progress = (remaining.toDouble() / remaining.toDouble()).toFloat()
                progressBar.progress(progress)
                true
            } else {
                // timeout
                action(this@Dialog)
                false
            }
        }


    }
}
