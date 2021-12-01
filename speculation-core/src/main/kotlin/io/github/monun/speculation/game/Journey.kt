package io.github.monun.speculation.game

import io.github.monun.speculation.game.zone.Zone

class Journey(
    val piece: Piece,
    val from: Zone,
    to: Zone,
    val movement: Movement,
    val cause: MovementCause,
    val source: Piece
) {
    lateinit var path: List<Zone>
        private set

    private val _passed = arrayListOf<Zone>()

    val passed: List<Zone>
        get() = _passed.toList()

    private var state = 0

    internal suspend fun pathfinding() {
        val path = arrayListOf<Zone>()

        if (movement == Movement.TELEPORT) {

        } else {
            var zone = from.shift(movement)

            while (zone != destination) {
                if (!zone.navigate(this)) {
                    destination = zone
                    break
                }
                path += zone
                zone = zone.shift(movement)
            }
        }
        this.path = path.toList()
    }

    internal fun pass(zone: Zone) {
        _passed += zone
    }

    var destination: Zone = to
        private set(value) {
            require(state == 0) { "Immutable journey state" }
            field = value
        }
}