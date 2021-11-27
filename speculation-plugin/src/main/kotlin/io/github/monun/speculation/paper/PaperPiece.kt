package io.github.monun.speculation.paper

import io.github.monun.speculation.game.Piece
import io.github.monun.tap.fake.FakeEntity
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

class PaperPiece(val process: PaperGameProcess, val piece: Piece, val color: PieceColor, player: Player) {
    val name: Component = Component.text().content(piece.name).color(color.textColor).build()

    var player: Player? = player

    lateinit var stand: FakeEntity
}