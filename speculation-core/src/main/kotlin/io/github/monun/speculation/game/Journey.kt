package io.github.monun.speculation.game

import io.github.monun.speculation.game.zone.Zone

class Journey(
    val piece: Piece,
    val from: Zone,
    to: Zone,
    val movement: Movement,
    val cause: MovementCause,
    val source: Piece? = null
) {
    private val _passed = arrayListOf<Zone>()

    val passed: List<Zone>
        get() = _passed.toList()

    internal fun pass(zone: Zone) {
        _passed += zone
    }

    var destination: Zone = to
        internal set
}