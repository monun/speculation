package io.github.monun.speculation.paper

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PaperGameListener(
    private val process: PaperGameProcess
) : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.player.let { player ->
            process.piece(player)?.let { piece ->
                piece.player = player
            }
            process.dialogDispatcher.currentDialog?.timeout?.showProgressBar(listOf(player))
            process.fakeEntityServer.addPlayer(player)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        event.player.let { player ->
            process.piece(player)?.let { piece ->
                piece.player = null
            }
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        process.piece(player)?.let { piece ->
            val dialog = process.dialogDispatcher.currentDialog ?: return@let
            if (dialog.piece == piece) {
                val location = player.eyeLocation
                val start = location.toVector()
                val direction = location.direction

                dialog.interact(player, start, direction)
            }
        }
    }
}