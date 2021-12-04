package io.github.monun.speculation.paper.util

import io.github.monun.speculation.paper.PaperPiece
import io.github.monun.speculation.paper.PaperZone
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit

fun PaperPiece.broadcast(message: Component) {
    Bukkit.broadcast(
        Component.text().append(this.name)
            .append(Component.text(": "))
            .append(message.colorIfAbsent(NamedTextColor.WHITE))
            .build()
    )
}

fun PaperPiece.broadcast(zone: PaperZone, message: Component) {
    Bukkit.broadcast(
        Component.text().append(this.name)
            .append(Component.text(": "))
            .append(Component.text(zone.name))
            .append(Component.text(" - "))
            .append(message.colorIfAbsent(NamedTextColor.WHITE))
            .build()
    )
}