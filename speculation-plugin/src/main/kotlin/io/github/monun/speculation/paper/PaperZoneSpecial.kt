package io.github.monun.speculation.paper

import io.github.monun.speculation.game.zone.*
import org.bukkit.entity.ItemFrame

abstract class PaperZoneSpecial(
) : PaperZone() {
    var modelId = -1

    lateinit var slot: ItemFrame

    override fun destroy() {
        slot.remove()
    }


}

class PaperZoneStart(override val zone: ZoneStart): PaperZoneSpecial() {

}

class PaperZoneGamble(override val zone: ZoneGamble) : PaperZoneSpecial() {

}

class PaperZoneJail(override val zone: ZoneJail) : PaperZoneSpecial() {

}

class PaperZoneMagic(override val zone: ZoneMagic) : PaperZoneSpecial() {

}

class PaperZoneFestival(override val zone: ZoneFestival) : PaperZoneSpecial() {

}

class PaperZonePortal(override val zone: ZonePortal) : PaperZoneSpecial() {

}

class PaperZoneNTS(override val zone: ZoneNTS) : PaperZoneSpecial() {

}