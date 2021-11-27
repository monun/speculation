package io.github.monun.speculation.paper

import org.bukkit.block.BlockFace

enum class Face(blockFace: BlockFace) {
    SOUTH(BlockFace.SOUTH),
    WEST(BlockFace.WEST),
    NORTH(BlockFace.NORTH),
    EAST(BlockFace.EAST);

    val modX = blockFace.modX
    val modZ = blockFace.modZ

    fun rotate(rotation: Int): Face {
        val values = values()
        val count = values.count()
        val index = ordinal + count + rotation

        return values[index % count]
    }
}