package io.github.monun.speculation.paper

import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector

object PaperGameConfig {
    var boardY = 5.0
    var center: Vector = Vector(18.5, boardY, 18.5)
        get() = field.clone()
    var centerBox: BoundingBox = BoundingBox.of(center.apply { y += 0.5 }, 13.5, 0.5, 13.5)
}