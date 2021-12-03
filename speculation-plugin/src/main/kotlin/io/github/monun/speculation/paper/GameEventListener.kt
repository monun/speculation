package io.github.monun.speculation.paper

import io.github.monun.heartbeat.coroutines.Heartbeat
import io.github.monun.heartbeat.coroutines.Suspension
import io.github.monun.speculation.game.MovementCause
import io.github.monun.speculation.game.event.*
import io.github.monun.speculation.game.zone.ZoneFestival
import io.github.monun.speculation.game.zone.ZoneJail
import io.github.monun.speculation.paper.util.broadcast
import io.github.monun.speculation.paper.util.playSound
import io.github.monun.tap.effect.playFirework
import io.github.monun.tap.protocol.PacketSupport
import io.github.monun.tap.trail.TrailSupport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Firework
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import java.time.Duration
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class GameEventListener(private val process: PaperGameProcess) {
    init {
        process.game.eventAdapter.apply {
            register(PieceMoveEvent::class.java, ::onPieceMove)
            register(PropertyUpgradeEvent::class.java, ::onPropertyUpgrade)
            register(PieceWithdrawEvent::class.java, ::onPieceWithdraw)
            register(PieceDepositEvent::class.java, ::onPieceDeposit)
            register(PieceTransferEvent::class.java, ::onPieceTransfer)
            register(PropertyAcquisitionEvent::class.java, ::onPropertyAcquisition)
            register(PropertyClearEvent::class.java, ::onPropertyClear)
            register(PieceGambleStartEvent::class.java, ::onPieceGambleStart)
            register(PieceGambleEndEvent::class.java, ::onPieceGambleEnd)
            register(PieceLeaveEvent::class.java, ::onPieceLeave)
            register(PieceArriveEvent::class.java, ::onPieceArrive)
            register(PieceTakeTurnEvent::class.java, ::onPieceTakeTurn)
            register(PieceJailbreakEvent::class.java, ::onPieceTryEscape)
            register(PropertyAddAmplifierEvent::class.java, ::onPropertyAddAmplifier)
            register(PropertyRemoveAmplifierEvent::class.java, ::onPropertyRemoveAmplifier)
            register(PropertyUpdateEvent::class.java, ::onPropertyUpdate)
            register(PieceTurnOverEvent::class.java, ::onPieceTurnOver)
            register(GameOverEvent::class.java, ::onGameOver)
        }
    }

    private suspend fun onPieceMove(event: PieceMoveEvent) {
        val piece = event.piece
        val zoneTo = event.to

        val journey = event.journey

        val paperPiece = piece.attachment<PaperPiece>()
        val paperZoneTo = zoneTo.attachment<PaperZone>()

        withContext(Dispatchers.Heartbeat) {
            val from = paperPiece.stand.location
            val to = paperZoneTo.nextPieceLocation()
            val distance = from.distance(to)
            var ticks = (distance * 1.5).toInt()
            if (distance < 8.0)
                ticks = 5
            if (journey.cause != MovementCause.DICE) ticks = ticks.shr(1)

            val vector = to.clone().subtract(from).toVector()
            val sus = Suspension()
            for (tick in 1..ticks) {
                sus.delay(50L)
                val parabolaX = tick.toDouble() / ticks
                val parabolaY = 4.0 * parabolaX - 4.0 * parabolaX.pow(2)

                val location = from.clone().apply {
                    add(vector.clone().multiply(parabolaX))
                    y += parabolaY * distance / 3.0
                }
                paperPiece.stand.moveTo(location)
            }
            paperPiece.playStepSound()

            if (zoneTo != journey.destination) {
                paperZoneTo.playPassEffect()
            }
        }
    }

    private suspend fun onPieceLeave(event: PieceLeaveEvent) {
        val zone = event.destination
        val paperZone = zone.attachment<PaperZone>()

        withContext(Dispatchers.Heartbeat) {
            paperZone.playLeaveEffect(event.piece.attachment())
        }
    }

    private suspend fun onPieceArrive(event: PieceArriveEvent) {
        val zone = event.from
        val paperZone = zone.attachment<PaperZone>()

        withContext(Dispatchers.Heartbeat) {
            paperZone.playArriveEffect(event.piece.attachment())
        }
    }

    private suspend fun onPropertyUpgrade(event: PropertyUpgradeEvent) {
        val property = event.property
        val owner = event.owner

        val paperProperty = property.attachment<PaperZoneProperty>()
        val paperOwner = owner.attachment<PaperPiece>()

        withContext(Dispatchers.Heartbeat) {
            paperProperty.playUpgradeEffect(paperOwner, event.level)
            paperProperty.updateSlots()
            paperProperty.updateTolls()
        }
    }

    private suspend fun onPropertyUpdate(event: PropertyUpdateEvent) {
        val property = event.property

        val paperProperty = property.attachment<PaperZoneProperty>()

        withContext(Dispatchers.Heartbeat) {
            paperProperty.updateSlots()
            paperProperty.updateTolls()

            event.oldInfo?.let { (oldOwner, _) ->
                event.newInfo?.let { (newOwner, _) ->
                    val paperNewOwner = newOwner.attachment<PaperPiece>()

                    oldOwner.attachment<PaperPiece>().broadcast(paperProperty, Component.text().also { text ->
                        text.append(Component.text("소유주 변경 -> "))
                        text.append(newOwner.attachment<PaperPiece>().name)
                    }.build())
                    paperProperty.location.run {
                        val effect = FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(paperNewOwner.color.color).build()
                        world.playFirework(this, effect)
                    }
                }
            }
        }
    }

    private suspend fun onPropertyAcquisition(event: PropertyAcquisitionEvent) {
        val property = event.property
        val paperProperty = property.attachment<PaperZoneProperty>()

        withContext(Dispatchers.Heartbeat) {
            paperProperty.updateSlots()
            paperProperty.updateTolls()
        }
    }

    private suspend fun onPieceDeposit(event: PieceDepositEvent) {
        val piece = event.piece
        val zone = event.zone
        val amount = event.amount
        val count = max(1, sqrt(amount.toDouble()).toInt())

        val paperPiece = piece.attachment<PaperPiece>()
        val paperZone = zone.attachment<PaperZone>()

        withContext(Dispatchers.Heartbeat) {
            broadcastTransfer(Component.text("[${paperZone.name}]"), paperPiece.name, event.amount)

            val box = paperZone.box
            val point = paperZone.box.min.toLocation(process.world).apply { y = box.maxY + 0.25 }
            val xWidth = box.widthX
            val zWidth = box.widthZ

            val fes = process.fakeEntityServer
            val emeralds = List(count) {
                val loc = point.clone().apply {
                    x += Random.nextDouble() * xWidth
                    z += Random.nextDouble() * zWidth
                }
                fes.spawnItem(loc, ItemStack(Material.EMERALD)).apply {
                    broadcast(
                        PacketSupport.entityVelocity(
                            bukkitEntity.entityId,
                            Vector(0.0, 0.25 + Random.nextDouble() * 0.5, 0.0)
                        )
                    )
                }
            }

            delay(1000L)

            val standId = paperPiece.stand.bukkitEntity.entityId
            val standLoc = paperPiece.stand.location
            val prevScore = paperPiece.score
            emeralds.forEachIndexed { index, emerald ->
                emerald.broadcastImmediately(PacketSupport.takeItem(emerald.bukkitEntity.entityId, standId, 1))
                standLoc.playSound(Sound.ENTITY_ITEM_PICKUP, 1.0F)
                emerald.remove()
                paperPiece.score = prevScore + amount * (index + 1) / count

                delay(1L) // 1 tick
            }
        }
    }

    private suspend fun onPieceWithdraw(event: PieceWithdrawEvent) {
        val piece = event.piece
        val paperPiece = piece.attachment<PaperPiece>()
        val count = max(1, sqrt(event.amount.toDouble()).toInt())

        withContext(Dispatchers.Heartbeat) {
            paperPiece.updateScore(piece.balance)
            broadcastTransfer(
                paperPiece.name,
                Component.text("[${event.zone.attachment<PaperZone>().name}]"),
                event.amount
            )

            val location = paperPiece.stand.location.apply { y += 1.0 }

            location.playSound(Sound.BLOCK_CHAIN_PLACE, 0.1F)

            val fes = process.fakeEntityServer
            val emeralds = List(count) {
                fes.spawnItem(location, ItemStack(Material.EMERALD)).apply {
                    broadcast {
                        PacketSupport.entityVelocity(bukkitEntity.entityId, bukkitEntity.velocity)
                    }
                }
            }

            process.scope.launch {
                delay(1000L)
                emeralds.forEach { it.remove() }
            }
        }
    }

    private fun broadcastTransfer(from: Component, to: Component, amount: Int) {
        Component.text().also { text ->
            text.append(from)
            text.append(Component.text(" - ").color(NamedTextColor.RED))
            text.append(Component.text(amount).color(NamedTextColor.DARK_GREEN))
            text.append(Component.text(" + ").color(NamedTextColor.AQUA))
            text.append(to)
        }.build().let {
            Bukkit.broadcast(it)
        }
    }

    private suspend fun onPieceTransfer(event: PieceTransferEvent) {
        val piece = event.piece
        val receiver = event.receiver
        val amount = event.amount
        val count = max(1, sqrt(amount.toDouble()).toInt())

        val paperPiece = piece.attachment<PaperPiece>()
        val paperReceiver = receiver.attachment<PaperPiece>()

        withContext(Dispatchers.Heartbeat) {
            paperPiece.updateScore(piece.balance)
            broadcastTransfer(paperPiece.name, paperReceiver.name, amount)

            val location = paperPiece.stand.location.apply { y += 1.0 }
            location.playSound(Sound.BLOCK_CHAIN_PLACE, 0.1F)

            val fes = process.fakeEntityServer
            val emeralds = List(count) {
                fes.spawnItem(location, ItemStack(Material.EMERALD)).apply {
                    broadcast {
                        PacketSupport.entityVelocity(bukkitEntity.entityId, bukkitEntity.velocity)
                    }
                }
            }

            delay(1000L)

            val standId = paperReceiver.stand.bukkitEntity.entityId
            val standLoc = paperReceiver.stand.location
            val prevScore = paperReceiver.score
            emeralds.forEachIndexed { index, emerald ->
                emerald.broadcastImmediately(PacketSupport.takeItem(emerald.bukkitEntity.entityId, standId, 1))
                standLoc.playSound(Sound.ENTITY_ITEM_PICKUP, 1.0F)
                emerald.remove()
                paperReceiver.score = prevScore + amount * (index + 1) / count
                delay(1L) // 1 tick
            }
        }
    }

    private suspend fun onPropertyClear(event: PropertyClearEvent) {
        val property = event.property
        val paperProperty = property.attachment<PaperZoneProperty>()

        withContext(Dispatchers.Heartbeat) {
            paperProperty.playClearEffect()
            paperProperty.updateSlots()
        }
    }

    private suspend fun onPieceGambleStart(event: PieceGambleStartEvent) {
        val piece = event.piece
        val betting = event.betting

        val paperPiece = piece.attachment<PaperPiece>()

        withContext(Dispatchers.Heartbeat) {
            Bukkit.getServer().showTitle(
                Title.title(
                    paperPiece.name,
                    Component.text("겜블: ").append(
                        Component.text(betting)
                            .color(NamedTextColor.DARK_GREEN)
                    ),
                    Title.Times.of(
                        Duration.ofMillis(250),
                        Duration.ofSeconds(2),
                        Duration.ofMillis(250)
                    )
                )
            )
            delay(2500L)
        }
    }

    private suspend fun onPieceGambleEnd(event: PieceGambleEndEvent) {
        val winners = event.winners
        val losers = event.losers

        val subtitleText = Component.text().also {
            winners.forEachIndexed { index, piece ->
                if (index > 0) it.append(Component.space())
                it.append(piece.attachment<PaperPiece>().name.decorate(TextDecoration.BOLD))
            }
            if (losers.isNotEmpty()) {
                it.append(Component.text(" / "))

                losers.forEachIndexed { index, piece ->
                    if (index > 0) it.append(Component.space())
                    it.append(piece.attachment<PaperPiece>().name.decorate(TextDecoration.STRIKETHROUGH))
                }
            }
        }.build()

        val times = Title.Times.of(
            Duration.ofMillis(250),
            Duration.ofSeconds(2),
            Duration.ofMillis(250)
        )

        val titleForWinner = Title.title(
            Component.text("성공!").color(NamedTextColor.GOLD),
            subtitleText,
            times
        )
        val titleForLoser = Title.title(
            Component.text("실패!").color(NamedTextColor.RED),
            subtitleText,
            times
        )

        withContext(Dispatchers.Heartbeat) {
            Bukkit.getOnlinePlayers().forEach { player ->
                val playerPiece = process.piece(player)

                if (playerPiece != null && playerPiece.piece in losers) {
                    player.showTitle(titleForLoser)
                } else {
                    player.showTitle(titleForWinner)
                }
            }
        }
        delay(2500L)
    }

    private suspend fun onPieceTakeTurn(event: PieceTakeTurnEvent) {
        val piece = event.piece
        val zone = piece.zone

        val paperPiece = piece.attachment<PaperPiece>()

        withContext(Dispatchers.Heartbeat) {
            val titleText = paperPiece.name.append(Component.text("의 차례"))
            var subtitleText = Component.empty()

            if (zone is ZoneJail) {
                val jailCount = piece.jailCount

                if (jailCount > 0) {
                    subtitleText = Component.text("남은 감옥 턴: $jailCount")
                }
            }

            Bukkit.getServer().showTitle(
                Title.title(
                    titleText,
                    subtitleText,
                    Title.Times.of(
                        Duration.ofMillis(250),
                        Duration.ofSeconds(1),
                        Duration.ofMillis(250)
                    )
                )
            )

            paperPiece.stand.updateMetadata<ArmorStand> {
                isGlowing = true
                isSmall = false
            }
        }

        delay(1500L)
    }

    private suspend fun onPieceTurnOver(event: PieceTurnOverEvent) {
        val piece = event.piece

        val paperPiece = piece.attachment<PaperPiece>()

        withContext(Dispatchers.Heartbeat) {
            paperPiece.stand.updateMetadata<ArmorStand> {
                isGlowing = false
                isSmall = true
            }
        }

        delay(1500L)
    }

    private suspend fun onPieceTryEscape(event: PieceJailbreakEvent) {
        val piece = event.piece
        val success = event.success

        withContext(Dispatchers.Heartbeat) {
            if (success) {
                Bukkit.getServer().showTitle(
                    Title.title(
                        Component.text("탈출 성공!").color(NamedTextColor.AQUA),
                        Component.empty(),
                        Title.Times.of(
                            Duration.ofMillis(0),
                            Duration.ofSeconds(1),
                            Duration.ofMillis(250)
                        )
                    )
                )
            } else {
                Bukkit.getServer().showTitle(
                    Title.title(
                        Component.text("탈출 실패").color(NamedTextColor.RED),
                        Component.text("남은 감옥 턴: ${piece.jailCount} / ${ZoneJail.count}"),
                        Title.Times.of(
                            Duration.ofMillis(0),
                            Duration.ofSeconds(1),
                            Duration.ofMillis(250)
                        )
                    )
                )
            }
        }

        delay(1250L)
    }

    private suspend fun onPropertyAddAmplifier(event: PropertyAddAmplifierEvent) {
        val property = event.property
        val paperProperty = property.attachment<PaperZoneProperty>()

        withContext(Dispatchers.Heartbeat) {
            if (event.owner is ZoneFestival) {
                val locOfFestival = process.zoneFestival.location
                val locOfProperty = paperProperty.location
                val distance = locOfFestival.distance(locOfProperty)
                val ticks = (distance).toInt()

                val vector = locOfProperty.clone().subtract(locOfFestival).toVector()

                var prevLocation = locOfFestival.clone()

                delay(1500L)

                for (tick in 1..ticks) {
                    delay(1L)
                    val parabolaX = tick.toDouble() / ticks
                    val parabolaY = 4.0 * parabolaX - 4.0 * parabolaX.pow(2)

                    val location = locOfFestival.clone().apply {
                        add(vector.clone().multiply(parabolaX))
                        y += parabolaY * distance / 3.0
                    }

                    TrailSupport.trail(prevLocation, location, 0.2) { world, x, y, z ->
                        world.spawnParticle(Particle.FIREWORKS_SPARK, x, y, z, 1, 0.0, 0.0, 0.0, 0.1, null, true)
                    }

                    prevLocation = location
                }
            }

            paperProperty.playFestivalEffect(event.piece.attachment())
            paperProperty.updateTolls()
        }
    }

    private suspend fun onPropertyRemoveAmplifier(event: PropertyRemoveAmplifierEvent) {
        val property = event.property
        val paperProperty = property.attachment<PaperZoneProperty>()

        withContext(Dispatchers.Heartbeat) {
            paperProperty.updateTolls()
        }
    }

    private suspend fun onBankrupt(event: PieceBankruptEvent) {

    }

    private suspend fun onGameOver(event: GameOverEvent) {
        withContext(Dispatchers.Heartbeat) {
            Bukkit.getServer().showTitle(
                Title.title(
                    Component.text("게임종료").color(NamedTextColor.RED),
                    event.winner?.let { Component.text("우승: ").append(it.attachment<PaperPiece>().name) }
                        ?: Component.empty(),
                    Title.Times.of(
                        Duration.ofMillis(250),
                        Duration.ofSeconds(5),
                        Duration.ofMillis(250)
                    )
                )
            )

            for (zone in process.game.board.zones) {
                val paperZone = zone.attachment<PaperZone>()
                paperZone.nextPieceLocation().run {
                    world.spawn(this, Firework::class.java).apply {
                        fireworkMeta = fireworkMeta.also { meta ->
                            meta.addEffect(
                                FireworkEffect.builder().with(FireworkEffect.Type.STAR)
                                    .withColor(Color.fromRGB(Random.nextInt(0xFFFFFF)))
                                    .build()
                            )
                            meta.power = 0
                        }
                    }
                }
                delay(199L)
            }

            val credits = listOf(
                "" to ""
            )

            //TODO 크레딧 출력
        }
    }
}