package io.github.monun.speculation.game

import io.github.monun.speculation.game.zone.*
import io.github.monun.speculation.ref.upstream

class Board(game: Game) {
    internal val game = upstream(game)

    val zones = listOf(
        ZoneStart(),
        ZoneProperty(),
        ZoneProperty(),
        ZoneProperty(),

        ZoneGamble(),
        ZoneProperty(),
        ZoneProperty(),
        ZoneProperty(),


        ZoneJail(),
        ZoneProperty(),
        ZoneProperty(),
        ZoneProperty(),

        ZoneEvent(),
        ZoneProperty(),
        ZoneProperty(),
        ZoneProperty(),


        ZoneFestival(),
        ZoneProperty(),
        ZoneProperty(),
        ZoneProperty(),

        ZoneEvent(),
        ZoneProperty(),
        ZoneProperty(),
        ZoneProperty(),


        ZonePortal(),
        ZoneProperty(),
        ZoneProperty(),
        ZoneProperty(),

        ZoneEvent(),
        ZoneProperty(),
        ZoneNTS(),
        ZoneProperty()
    ).also { list ->
        val count = list.count()
        list.forEachIndexed { index, zone ->
            zone.apply {
                board = this@Board
                previous = list[(count + index - 1) % count]
                next = list[(count + index + 1) % count]
            }
        }
    }

    val zoneProperties: List<ZoneProperty> = zones.filterIsInstance<ZoneProperty>().apply {
        this[0].initLevels(10)
        this[1].initLevels(11)
        this[2].initLevels(12)

        this[3].initLevels(14)
        this[4].initLevels(16)
        this[5].initLevels(18)


        this[6].initLevels(20)
        this[7].initLevels(21)
        this[8].initLevels(22)

        this[9].initLevels(24)
        this[10].initLevels(26)
        this[11].initLevels(28)


        this[12].initLevels(30)
        this[13].initLevels(31)
        this[14].initLevels(32)

        this[15].initLevels(34)
        this[16].initLevels(36)
        this[17].initLevels(38)


        this[18].initLevels(40)
        this[19].initLevels(42)
        this[20].initLevels(44)

        this[21].initLevels(47)

        this[22].initLevels(50)
    }
    val zoneSpecials: List<Zone> = zones.filter { it !is ZoneProperty }

    private val _pieces = LinkedHashMap<String, Piece>()

    val pieces: Map<String, Piece>
        get() = _pieces

    private val _teams = LinkedHashMap<String, PieceTeam>()

    fun newPiece(name: String): Piece {
        game.checkState(GameState.NEW)
        require(name !in _pieces) { "Already registered piece name '$name'" }

        return Piece(this, name, zones.first()).also {
            _pieces[name] = it
        }
    }

    fun newTeam(name: String, members: Set<Piece>): PieceTeam {
        game.checkState(GameState.NEW)
        require(name !in _teams) { "Already registered team name '$name'" }
        require(members.all { it.team == null }) { "Already piece has team" }

        return PieceTeam(name, members.toList()).also {
            _teams[name] = it

            for (member in members) {
                member.team = it
            }
        }
    }
}