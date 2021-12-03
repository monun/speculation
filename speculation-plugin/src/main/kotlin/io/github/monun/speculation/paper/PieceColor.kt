package io.github.monun.speculation.paper

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Color
import org.bukkit.Material

enum class PieceColor(val group: PieceGroup, val textColor: NamedTextColor, val material: Material) {
    WHITE(PieceGroup.WHITE, NamedTextColor.WHITE, Material.WHITE_DYE),
    GRAY(PieceGroup.WHITE, NamedTextColor.GRAY, Material.LIGHT_GRAY_DYE),
    RED(PieceGroup.RED, NamedTextColor.RED, Material.PINK_DYE),
    DARK_RED(PieceGroup.RED, NamedTextColor.DARK_RED, Material.RED_DYE),
    YELLOW(PieceGroup.YELLOW, NamedTextColor.YELLOW, Material.YELLOW_DYE),
    GOLD(PieceGroup.YELLOW, NamedTextColor.GOLD, Material.ORANGE_DYE),
    GREEN(PieceGroup.GREEN, NamedTextColor.GREEN, Material.LIME_DYE),
    DARK_GREEN(PieceGroup.GREEN, NamedTextColor.DARK_GREEN, Material.GREEN_DYE),
    AQUA(PieceGroup.AQUA, NamedTextColor.AQUA, Material.LIGHT_BLUE_DYE),
    DARK_AQUA(PieceGroup.AQUA, NamedTextColor.DARK_AQUA, Material.BLUE_DYE),
    LIGHT_PURPLE(PieceGroup.PURPLE, NamedTextColor.LIGHT_PURPLE, Material.MAGENTA_DYE),
    DARK_PURPLE(PieceGroup.PURPLE, NamedTextColor.DARK_PURPLE, Material.PURPLE_DYE);

    val color = Color.fromRGB(textColor.value())

    fun isFriendly(other: PieceColor) = group == other.group

    companion object {
        fun textColorOf(textColor: NamedTextColor): PieceColor? {
            return values().find { it.textColor == textColor }
        }
    }
}

enum class PieceGroup(val barColor: BossBar.Color) {
    WHITE(BossBar.Color.WHITE),
    RED(BossBar.Color.RED),
    YELLOW(BossBar.Color.YELLOW),
    GREEN(BossBar.Color.GREEN),
    AQUA(BossBar.Color.BLUE),
    PURPLE(BossBar.Color.PURPLE)
}