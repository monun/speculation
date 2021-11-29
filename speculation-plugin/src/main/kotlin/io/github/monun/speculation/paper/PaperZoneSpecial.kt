package io.github.monun.speculation.paper

import io.github.monun.speculation.game.zone.Zone
import org.bukkit.entity.ItemFrame

class PaperZoneSpecial(
    override val zone: Zone
) : PaperZone() {
    var modelId = -1

    lateinit var slot: ItemFrame

    override fun destroy() {
        slot.remove()
    }
}