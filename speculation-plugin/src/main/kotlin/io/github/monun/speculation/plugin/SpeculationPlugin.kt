package io.github.monun.speculation.plugin

import io.github.monun.kommand.getValue
import io.github.monun.kommand.kommand
import io.github.monun.speculation.paper.PaperGameProcess
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

// TODO 메인루틴 주사위까지 테스트
// TODO 땅 구매 업데이트와 매각까지 구현

class SpeculationPlugin : JavaPlugin() {
    var process: PaperGameProcess? = null
        private set

    override fun onEnable() {
        kommand {
            register("speculation") {
                then("start") {
                    then("world" to dimension(), "players" to players(), "teamMatch" to bool()) {
                        executes {
                            val world: World by it
                            val players: Collection<Player> by it
                            val teamMatch: Boolean by it

                            kotlin.runCatching {
                                startProcess(world, players.toSet(), teamMatch)
                            }.onFailure { exception ->
                                if (exception is IllegalStateException) {
                                    feedback(Component.text(exception.message ?: "").color(NamedTextColor.RED))
                                } else {
                                    throw exception
                                }
                            }
                        }
                    }
                }
                then("stop") {
                    executes {
                        kotlin.runCatching {
                            stopProcess()
                        }.onFailure { exception ->
                            if (exception is IllegalArgumentException) {
                                feedback(Component.text(exception.message ?: "").color(NamedTextColor.RED))
                            } else {
                                throw exception
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDisable() {
        runCatching { stopProcess() }
    }

    fun startProcess(world: World, players: Set<Player>, teamMatch: Boolean): PaperGameProcess {
        check(process == null) { "process already running" }

        return PaperGameProcess(this, world).apply {
            register(players, teamMatch)
        }.also {
            process = it
        }
    }

    fun stopProcess(): PaperGameProcess {
        val process = requireNotNull(process) { "process is not running" }
        process.unregister()
        this.process = null

        return process
    }

/*    private fun registerTest() {
        val fakeEntityServer = FakeEntityServer.create(this).apply {
            Bukkit.getOnlinePlayers().forEach { addPlayer(it) }
        }
        val dices = arrayListOf<Dice>()

        server.pluginManager.registerEvents(object : Listener {
            @EventHandler
            fun onPluginDisable(event: PluginDisableEvent) {
                if (this@SpeculationPlugin == event.plugin) {
                    fakeEntityServer.clear()
                }
            }

            @EventHandler
            fun onPlayerJoin(event: PlayerJoinEvent) {
                fakeEntityServer.addPlayer(event.player)
            }

            @EventHandler
            fun onPlayerInteract(event: PlayerInteractEvent) {
                val loc = event.player.eyeLocation.apply { y += 1.0 }
                val vec = loc.direction
                dices += Dice(fakeEntityServer, loc, vec)
            }
        }, this)

        val sus = Suspension()

        HeartbeatScope().launch {
            while (isActive) {
                sus.delay(50L)
                dices.forEach { it.onUpdate() }
                fakeEntityServer.update()
            }
        }
    }*/
}