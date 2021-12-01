package io.github.monun.speculation.paper

import io.github.monun.speculation.game.zone.*
import io.github.monun.speculation.paper.util.playSound
import kotlinx.coroutines.delay
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Firework
import org.bukkit.entity.ItemFrame
import kotlin.random.Random

abstract class PaperZoneSpecial(
) : PaperZone() {
    var modelId = -1

    lateinit var slot: ItemFrame

    override fun destroy() {
        slot.remove()
    }


}

class PaperZoneStart(override val zone: ZoneStart) : PaperZoneSpecial() {
    override suspend fun playPassEffect() {
        location.playSound(Sound.ENTITY_PLAYER_LEVELUP, 2.0F)
    }

    override suspend fun playArriveEffect() {
        location.run {
            y += 1.0
            playSound(Sound.ENTITY_PLAYER_LEVELUP, 2.0F)
            world.spawnParticle(Particle.TOTEM, this, 16, 0.5, 0.5, 0.5, 0.2, null, true)
        }
    }
}

class PaperZoneGamble(override val zone: ZoneGamble) : PaperZoneSpecial() {
    override suspend fun playArriveEffect() {
        location.playSound(Sound.BLOCK_CHAIN_PLACE, 0.1F)
    }
}

class PaperZoneJail(override val zone: ZoneJail) : PaperZoneSpecial() {
    override suspend fun playArriveEffect() {
        location.playSound(Sound.BLOCK_CHEST_CLOSE, 0.1F)
    }

    override suspend fun playLeaveEffect() {
        location.playSound(Sound.BLOCK_CHEST_OPEN, 0.5F)
    }
}

class PaperZoneMagic(override val zone: ZoneMagic) : PaperZoneSpecial() {
    override suspend fun playArriveEffect() {
        location.playSound(Sound.ITEM_BOOK_PAGE_TURN, 0.1F)
    }
}

class PaperZoneFestival(override val zone: ZoneFestival) : PaperZoneSpecial() {
    override suspend fun playArriveEffect() {
        repeat(5) {
            nextLocation().run {
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
            delay(149L)
        }
    }
}

class PaperZonePortal(override val zone: ZonePortal) : PaperZoneSpecial() {
    override suspend fun playArriveEffect() {
        location.playSound(Sound.ENTITY_SHULKER_TELEPORT, 0.1F)
    }
}

class PaperZoneNTS(override val zone: ZoneNTS) : PaperZoneSpecial() {
    override suspend fun playArriveEffect() {
        location.playSound(Sound.ENTITY_PIGLIN_JEALOUS, 1.0F)
    }
}