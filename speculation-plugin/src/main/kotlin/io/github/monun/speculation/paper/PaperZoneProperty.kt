package io.github.monun.speculation.paper

import io.github.monun.speculation.game.zone.ZoneProperty
import io.github.monun.speculation.paper.util.broadcast
import io.github.monun.speculation.paper.util.playSound
import io.github.monun.tap.effect.playFirework
import io.github.monun.tap.fake.FakeEntity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Firework
import org.bukkit.entity.ItemFrame
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

class PaperZoneProperty(
    override val zone: ZoneProperty
) : PaperZone() {
    var landmarkModelId: Int = -1
    lateinit var landmarkName: String
    lateinit var festivalName: String

    lateinit var nameTag: FakeEntity
    lateinit var tollsTag: FakeEntity

    lateinit var slotLeft: ItemFrame
    lateinit var slotCenter: ItemFrame
    lateinit var slotRight: ItemFrame

    val slots: List<ItemFrame>
        get() = listOf(slotLeft, slotCenter, slotRight)

    override fun destroy() {
        nameTag.remove()
        tollsTag.remove()
        slots.forEach { it.remove() }
    }

    fun updateSlots() {
        slotLeft.setItem(null, false)
        slotCenter.setItem(null, false)
        slotRight.setItem(null, false)

        val owner = zone.owner

        if (owner != null) {
            val paperPiece = owner.attachment<PaperPiece>()
            val material = paperPiece.color.material
            val level = zone.level

            fun update(slot: ItemFrame, modelId: Int) {
                slot.setItem(ItemStack(material).apply {
                    editMeta {
                        it.setCustomModelData(modelId)
                    }
                }, false)
            }

            if (level == zone.levelLandmark.value) {
                update(slotCenter, landmarkModelId)
            } else {
                if (level == zone.levelFlag.value) {
                    update(slotCenter, 1000)
                } else {
                    if (level >= zone.levelVilla.value) update(slotRight, 1001)
                    if (level >= zone.levelBuilding.value) update(slotCenter, 1002)
                    if (level >= zone.levelHotel.value) update(slotLeft, 1003)
                }
            }
        }
    }

    fun updateTolls() {
        val text = Component.text()
        text.content(zone.tolls.toString()).color(NamedTextColor.GREEN)

        tollsTag.updateMetadata<ArmorStand> {
            customName(text.build())
        }
    }

    fun playUpgradeEffect(owner: PaperPiece, level: ZoneProperty.Level) {
        if (level == zone.levelLandmark) {
            val location = slotCenter.location
            location.world.spawn(location, Firework::class.java).apply {
                fireworkMeta = fireworkMeta.also { meta ->
                    meta.addEffect(
                        FireworkEffect.builder().with(FireworkEffect.Type.STAR).withColor(owner.color.color)
                            .build()
                    )
                    meta.power = 0
                }
            }
            owner.broadcast(this, Component.text("${this.landmarkName} 건설 완료! by ${this.author}"))
        } else {
            val slot = when (level) {
                zone.levelFlag -> slotCenter
                zone.levelVilla -> slotRight
                zone.levelBuilding -> slotCenter
                zone.levelHotel -> slotLeft
                else -> error("impossible")
            }

            val location = slot.location.apply { y += 1.0 }
            val world = location.world
            world.spawnParticle(Particle.FLASH, location, 0)
            location.playSound(Sound.BLOCK_ANVIL_USE, 1.5F)
        }
    }

    fun playClearEffect() {
        val effect = FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(Color.WHITE).build()
        location.world.playFirework(location, effect, 128.0)
    }

    fun playSelectionEffect(selected: Boolean) {
        val pos = box.min.apply { y = box.maxY + 0.2 }
        val widthX = box.widthX
        val widthZ = box.widthZ
        val world = process.world
        val particle = if (selected) {
            Particle.SOUL
        } else {
            Particle.FLAME
        }

        repeat(4) {
            world.spawnParticle(
                particle,
                pos.x + Random.nextDouble() * widthX,
                pos.y,
                pos.z + Random.nextDouble() * widthZ,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                null,
                true
            )
        }
    }
}