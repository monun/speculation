package io.github.monun.speculation.paper

import io.github.monun.speculation.game.zone.ZoneProperty
import io.github.monun.tap.fake.FakeEntity
import net.kyori.adventure.text.Component
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.ItemFrame

class PaperZoneProperty(
    override val zone: ZoneProperty
) : PaperZone() {
    var landmarkModelId: Int = -1
    lateinit var name: String
    lateinit var landmarkName: String
    lateinit var festivalName: String
    lateinit var author: String

    lateinit var nameTag: FakeEntity
    lateinit var priceTag: FakeEntity

    lateinit var slotLeft: ItemFrame
    lateinit var slotCenter: ItemFrame
    lateinit var slotRight: ItemFrame

    val slots: List<ItemFrame>
        get() = listOf(slotLeft, slotCenter, slotRight)

    override fun destroy() {
        nameTag.remove()
        priceTag.remove()
        slots.forEach { it.remove() }
    }
}