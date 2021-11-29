package io.github.monun.speculation.paper

import io.github.monun.heartbeat.coroutines.HeartbeatScope
import io.github.monun.speculation.game.Game
import io.github.monun.speculation.plugin.SpeculationPlugin
import io.github.monun.tap.fake.FakeEntityServer
import io.github.monun.tap.fake.invisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import java.util.*

class PaperGameProcess(
    val plugin: SpeculationPlugin,
    val world: World
) {
    val center: Location = PaperGameConfig.center.toLocation(world)
        get() = field.clone()

    private var state = 0

    lateinit var game: Game
        private set

    lateinit var fakeEntityServer: FakeEntityServer
        private set

    lateinit var scope: CoroutineScope
        private set

    lateinit var pieceByIds: Map<UUID, PaperPiece>
    private lateinit var dices: MutableList<Dice>

    lateinit var dialogDispatcher: GameDialogDispatcher
    private lateinit var bukkitTask: BukkitTask
    private lateinit var listener: PaperGameListener

    fun register(players: Set<Player>, teamMatch: Boolean) {
        check(state == 0) { "Invalid state $state" }
        state = 1

        game = Game()
        fakeEntityServer = FakeEntityServer.create(plugin).apply {
            Bukkit.getOnlinePlayers().forEach { addPlayer(it) }
        }
        scope = HeartbeatScope()
        listener = PaperGameListener(this).also { plugin.server.pluginManager.registerEvents(it, plugin) }
        dialogDispatcher = GameDialogDispatcher().apply { register(this@PaperGameProcess) }
        bukkitTask = plugin.server.scheduler.runTaskTimer(plugin, ::onUpdate, 0L, 1L)
        dices = arrayListOf()

        initializeZones()
        initializeZoneProperties()
        initializeZoneSpecials()

        registerPlayers(players)
        if (teamMatch) registerTeams()

        state = 2

        game.launch(scope)
    }

    private fun initializeZones() {
        val zones = game.board.zones
        val faces = Face.values()

        var x = 1
        val y = PaperGameConfig.boardY
        var z = 1

        zones.forEachIndexed { index, zone ->
            val line = index / 8
            val number = index % 8
            val innerFace = faces[line]
            val outerFace = innerFace.rotate(2)
            val forwardFace = innerFace.rotate(-1)
            val reverseFace = innerFace.rotate(1)

            val width = 3

            var box = BoundingBox(
                x.toDouble(), y, z.toDouble(),
                (x + width).toDouble(), (y - 1.0), (z + width).toDouble()
            ).expandDirectional(
                -innerFace.modX.toDouble(),
                0.0,
                -innerFace.modZ.toDouble()
            )
            if (number == 0) {
                box = box.expandDirectional(
                    -forwardFace.modX.toDouble(),
                    0.0,
                    -forwardFace.modZ.toDouble()
                )
            }

            zone.attach(PaperZone.of(zone).apply {
                this.process = this@PaperGameProcess
                this.box = box
                this.isCorner = number == 0
                this.forwardFace = forwardFace
                this.reverseFace = reverseFace
                this.innerFace = innerFace
                this.outerFace = outerFace
                this.rotation = when (innerFace) {
                    Face.SOUTH -> Rotation.FLIPPED
                    Face.WEST -> Rotation.COUNTER_CLOCKWISE
                    Face.NORTH -> Rotation.NONE
                    Face.EAST -> Rotation.CLOCKWISE
                }.let {
                    if (this.isCorner) it.rotateCounterClockwise()
                    else it
                }
            })

            x += forwardFace.modX * 4
            z += forwardFace.modZ * 4
        }
    }

    private fun initializeZoneProperties() {
        val properties = game.board.zoneProperties.map { it.attachment<PaperZoneProperty>() }

        fun PaperZoneProperty.initResources(
            resourceModelId: Int,
            name: String,
            landmarkName: String,
            festivalName: String,
            author: String
        ) {
            this.landmarkModelId = resourceModelId
            this.author = author
            this.name = name
            this.landmarkName = landmarkName
            this.festivalName = festivalName
        }

        properties[0].initResources(10100, "봉화", "봉화군청", "은어축제", "monun")
        properties[1].initResources(12600, "영덕", "대게", "대게축제", "adfgdfg")
        properties[2].initResources(12400, "의성", "숭의문", "슈퍼푸드마늘축제", "adfgdfg")
        properties[3].initResources(11400, "임실", "치즈", "N치즈축제", "app6460")
        properties[4].initResources(10000, "안동", "하회탈", "국제탈춤페스티벌", "adfgdfg")
        properties[5].initResources(11902, "평창", "팔각구층석탑", "백일홍축제", "jhhan611")
        properties[6].initResources(11301, "횡성", "한우동상", "한우축제", "sgkill6")
        properties[7].initResources(12300, "태백", "강원랜드", "태백산눈축제", "adfgdfg")
        properties[8].initResources(10800, "동해", "바다열차", "무릉제", "zlfn")
        properties[9].initResources(11700, "나주", "배", "마한문화축제", "degree2121")
        properties[10].initResources(10501, "춘천", "소양강스카이워크", "마임축제", "pumkins2")
        properties[11].initResources(11200, "경주", "황룡사9층목탑", "신라문화제", "adfgdfg")
        properties[12].initResources(11800, "포항", "상생의손", "국제불빛축제", "adfgdfg")
        properties[13].initResources(12201, "속초", "영금정", "해맞이축제", "coroskai")
        properties[14].initResources(10701, "당진", "왜목마을", "기지시줄다리기축제", "piese1028")
        properties[15].initResources(11600, "제주", "슈슉", "들불축제", "menil")
        properties[16].initResources(11000, "김포", "김포공항", "평화축제", "pparru")
        properties[17].initResources(10400, "창원", "벚꽃열차", "진해군항제", "ikmyung")
        properties[18].initResources(12000, "세종", "밀마루전망타워", "세종축제", "adfgdfg")
        properties[19].initResources(11500, "인천", "인천국제공항", "송도불빛축제", "degree2121")
        properties[20].initResources(10601, "대전", "대동하늘공원", "사이언스페스티벌", "jjoa")
        properties[21].initResources(10301, "부산", "광안대교", "불꽃축제", "piese1028")
        properties[22].initResources(12100, "서울", "광화문", "벚꽃축제", "megat88")

        properties.forEach { property ->
            property.apply {
                val slotLocation = location.apply {
                    val face = outerFace
                    add(face.modX * 1.5, 0.0, face.modZ * 1.5)
                }
                val rot = rotation

                fun Location.spawnSlot() = world.spawn(this, ItemFrame::class.java).apply {
                    invisible = true
                    rotation = rot
                    setFacingDirection(BlockFace.UP)
                }

                fun Location.spawnTag(text: Component) =
                    fakeEntityServer.spawnEntity(this, ArmorStand::class.java).apply {
                        updateMetadata<ArmorStand> {
                            isInvisible = true
                            isMarker = true
                            isCustomNameVisible = true
                            customName(text)
                        }
                    }

                slotCenter = slotLocation.spawnSlot()
                slotLeft =
                    slotLocation.clone().add(reverseFace.modX.toDouble(), 0.0, reverseFace.modZ.toDouble()).spawnSlot()
                slotRight =
                    slotLocation.clone().add(forwardFace.modX.toDouble(), 0.0, forwardFace.modZ.toDouble()).spawnSlot()

                val tagLocation = this.location.add(innerFace.modX * 1.5, 1.25, innerFace.modZ * 1.5)
                nameTag =
                    tagLocation.spawnTag(Component.text(name).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                priceTag = tagLocation.apply { y -= 0.3 }.spawnTag(Component.text(0))
            }
        }
    }

    private fun initializeZoneSpecials() {
        val specials = game.board.zoneSpecials.map { it.attachment<PaperZoneSpecial>() }

        fun PaperZoneSpecial.initResources(modelId: Int, name: String, author: String) {
            this.name = name
            this.author = author
        }

        specials[0].initResources(0, "출발지", "null")
        specials[1].initResources(0, "카지노", "null")
        specials[2].initResources(0, "감옥", "null")
        specials[3].initResources(0, "이벤트", "null")
        specials[4].initResources(0, "축제", "null")
        specials[5].initResources(0, "이벤트", "null")
        specials[6].initResources(0, "포탈", "null")
        specials[7].initResources(0, "이벤트", "null")
        specials[8].initResources(0, "국세청", "null")

        specials.forEach { special ->
            special.apply {
                val rot = rotation
                val location = location.add(outerFace.modX * 1.5, 0.0, outerFace.modZ * 1.5)
                if (isCorner) location.add(reverseFace.modX * 1.5, 0.0, reverseFace.modZ * 1.5)

                slot = location.world.spawn(location, ItemFrame::class.java).apply {
                    invisible = true
                    rotation = rot
                    setFacingDirection(BlockFace.DOWN)
                }
            }
        }
    }

    private fun registerPlayers(players: Set<Player>) {
        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        val colors = HashSet<PieceColor>()
        val pieceByIds = hashMapOf<UUID, PaperPiece>()

        players.map { player ->
            game.board.newPiece(player.name).also { piece ->
                val team = scoreboard.getEntryTeam(player.name)
                val teamColor = team?.color()
                var pieceColor: PieceColor? = null

                if (teamColor is NamedTextColor) {
                    pieceColor = PieceColor.textColorOf(teamColor)
                }

                if (pieceColor == null) {
                    if (colors.isEmpty()) colors.addAll(PieceColor.values())

                    pieceColor = colors.random()
                    colors.remove(pieceColor)
                }

                PaperPiece(this, piece, pieceColor, player).apply {
                    val zone = piece.zone.attachment<PaperZone>()

                    stand = fakeEntityServer.spawnEntity(zone.nextLocation(), ArmorStand::class.java).apply {
                        updateMetadata<ArmorStand> {
                            isMarker = true
                            setBasePlate(false)
                            customName(Component.text(player.name))
                            isCustomNameVisible = true
                        }

                        updateEquipment {
                            val color = pieceColor.color

                            fun ItemStack.leatherMeta() {
                                editMeta {
                                    val meta = it as LeatherArmorMeta
                                    meta.setColor(color)
                                }
                            }

                            helmet = ItemStack(Material.PLAYER_HEAD).apply {
                                editMeta {
                                    val meta = it as SkullMeta
                                    meta.playerProfile = player.playerProfile
                                }
                            }
                            chestplate = ItemStack(Material.LEATHER_CHESTPLATE).apply { leatherMeta() }
                            leggings = ItemStack(Material.LEATHER_LEGGINGS).apply { leatherMeta() }
                            boots = ItemStack(Material.LEATHER_BOOTS).apply { leatherMeta() }
                        }
                    }
                }.also { paperPiece ->
                    piece.attach(paperPiece)
                    pieceByIds[player.uniqueId] = paperPiece
                }
            }
        }

        this.pieceByIds = pieceByIds.toMap()
    }

    private fun registerTeams() {
        val pieces: List<PaperPiece> = game.board.pieces.values.map { it.attachment() }
        val groups = pieces.groupBy { it.color.group }

        for ((group, piecesByGroup) in groups) {
            game.board.newTeam(group.name, piecesByGroup.map { it.piece }.toSet())
        }
    }

    private fun registerEvents() {
        // TODO
    }

    fun spawnDice(location: Location, piece: PaperPiece, offset: Vector): Dice {
        checkState()
        return Dice(fakeEntityServer, location).apply {
            owner = piece to offset
        }.also {
            dices += it
        }
    }

    fun piece(uniqueId: UUID) = pieceByIds[uniqueId]

    fun piece(player: Player) = piece(player.uniqueId)

    private fun onUpdate() {
        dialogDispatcher.currentDialog?.onUpdate()
        dices.removeIf {
            it.onUpdate()
            !it.isValid
        }

        fakeEntityServer.update()
    }

    fun unregister() {
        checkState()

        dialogDispatcher.disposeCurrentDialog()
        game.board.zones.map { it.attachment<PaperZone>() }.forEach { it.destroy() }
        fakeEntityServer.clear()
        HandlerList.unregisterAll(listener)
        bukkitTask.cancel()
        scope.cancel()
        state = 3
    }

    private fun checkState() {
        check(state != 3)
    }
}