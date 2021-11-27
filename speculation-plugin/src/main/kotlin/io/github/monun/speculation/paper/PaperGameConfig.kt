package io.github.monun.speculation.paper

import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector

object PaperGameConfig {
    var boardY = 5.0
    var centerBox: BoundingBox = BoundingBox()
    var center: Vector = Vector(18.5, boardY, 18.5)
        get() = field.clone()
}