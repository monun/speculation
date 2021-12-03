package io.github.monun.speculation.paper

import io.github.monun.speculation.paper.util.setSpeculationResourcePack
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.launch
import net.kyori.adventure.text.TextComponent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.inventory.InventoryInteractEvent
import org.bukkit.event.player.*

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
            player.setSpeculationResourcePack()
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

    @EventHandler
    fun onAsyncChat(event: AsyncChatEvent) {
        val player = event.player
        val piece = process.piece(player) ?: return
        val message = event.message()

        if (message is TextComponent) {
            val text = message.content()

            process.scope.launch {
                val dialog = process.dialogDispatcher.currentDialog ?: return@launch

                if (dialog.piece == piece) {
                    process.dialogDispatcher.currentDialog?.interact(text)
                }
            }
        }
    }

    @EventHandler
    fun onHangingBreak(event: HangingBreakEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onInventoryInteract(event: InventoryInteractEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onBucketEmpty(event: PlayerBucketEmptyEvent) {
        event.isCancelled = true
    }
}