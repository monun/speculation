package io.github.monun.speculation.paper

import io.github.monun.heartbeat.coroutines.Heartbeat
import io.github.monun.heartbeat.coroutines.Suspension
import io.github.monun.speculation.game.Piece
import io.github.monun.speculation.game.dialog.*
import io.github.monun.speculation.game.message.GameMessage
import io.github.monun.speculation.game.zone.Magic
import io.github.monun.speculation.game.zone.Zone
import io.github.monun.speculation.game.zone.ZoneProperty
import io.github.monun.speculation.paper.util.playSound
import io.github.monun.tap.math.toRadians
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.util.Vector
import java.time.Duration
import kotlin.math.max
import kotlin.math.roundToInt

class GameDialogDispatcher {
    private lateinit var process: PaperGameProcess

    var currentDialog: Dialog? = null
        get() = field?.takeIf { !it.isDisposed }
        private set

    fun register(process: PaperGameProcess) {
        this.process = process
        process.game.dialogAdapter.apply {
            register(GameDialogDice::class.java, ::dice)
            register(GameDialogUpgrade::class.java, ::upgrade)
            register(GameDialogAcquisition::class.java, ::acquisition)
            register(GameDialogSeizure::class.java, ::seizure)
            register(GameDialogBetting::class.java, ::betting)
            register(GameDialogTax::class.java, ::tax)
            register(GameDialogTargetZone::class.java, ::targetZone)
            register(GameDialogTargetPiece::class.java, ::targetPiece)
            register(GameDialogMagic::class.java, ::magic)
        }
    }

    fun disposeCurrentDialog() {
        currentDialog?.run {
            dispose()
            currentDialog = null
        }
    }

    // TODO 지우고 리팩토링 필요
    private fun newDialog(piece: Piece, init: Dialog.() -> Unit): Dialog {
        disposeCurrentDialog()

        return Dialog(piece.attachment()).apply(init).also {
            currentDialog = it
        }
    }

    private fun newDialog(dialog: GameDialog<*>, init: Dialog.() -> Unit): Dialog {
        disposeCurrentDialog()

        return Dialog(dialog.piece.attachment()).apply {
            val messageText = messageOf(dialog.message)
            val messageComponent = Component.text(messageText.first)

            message { messageComponent }
            init()
            currentDialog = this
        }
    }

    private suspend fun dice(diceDialog: GameDialogDice): List<Int> {
        return withContext(Dispatchers.Heartbeat) {
            process.clearDices()

            val paperPiece: PaperPiece = diceDialog.piece.attachment()
            val location =
                paperPiece.player?.location?.apply { y += 2.5 } ?: PaperGameConfig.center.toLocation(process.world)
            val numberOfDice = diceDialog.numberOfDice
            val offset = Vector(0.0, 2.5, 0.0).apply {
                if (diceDialog.numberOfDice > 1) x = 2.5
            }
            val dices = List(diceDialog.numberOfDice) {
                process.spawnDice(
                    location,
                    paperPiece,
                    offset.clone().rotateAroundY((360.0 / numberOfDice).toRadians() * it)
                )
            }

            newDialog(diceDialog.piece) {
                message {
                    Component.text("보드를 클릭해 주사위를 굴리세요!")
                }
                button(PaperGameConfig.centerBox) {
                    onClick { player, _, _ ->
                        val velocity = player.location.direction
                        dices.forEach { dice ->
                            dice.roll(
                                velocity.clone()
                                    .add(Vector.getRandom().subtract(Vector(0.5, 0.5, 0.5)).normalize().multiply(0.2))
                            )
                        }
                        disposeCurrentDialog()
                    }
                }
                timeout(Component.text("주사위"), 15L * 1000L) {
                    dices.filter { it.isBeforeRoll }.forEach { it.roll(Vector(0.0, 1.0, 0.0)) }
                }
            }

            val sus = Suspension()

            while (isActive) {
                val player = paperPiece.player

                if (player == null) {
                    // 플레이어가 없을때 위로 모든 주사위를 굴림
                    dices.filter { it.isBeforeRoll }.forEach { it.roll(Vector(0.0, 1.0, 0.0)) }
                }
                // 모든 주사위가 완료되면 탈출
                if (dices.all { it.isOnGround }) break
                sus.delay(50L)
            }

            dices.forEach { it.remove(60) }

            val title = Component.text()
            if (dices.count() > 1 && dices.all { it.value == dices.first().value } && diceDialog.message == GameMessage.ROLL_THE_DICE) {
                title.append(Component.text("더블! ").decorate(TextDecoration.BOLD))
            }
            dices.forEachIndexed { index, dice ->
                if (index != 0) title.append(Component.text("+"))
                title.append(Component.text(dice.value))
            }
            // 주사위를 2개 이상 요청할때 결과값 출력
            if (numberOfDice > 2) title.append(Component.text("=").append(Component.text(dices.sumOf { it.value }).color(NamedTextColor.AQUA)))

            process.world.showTitle(
                Title.title(
                    title.build(),
                    paperPiece.name,
                    Title.Times.of(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(200))
                )
            )
            sus.delay(1000L)
            dices.map { it.value }
        }
    }

    private suspend fun upgrade(upgradeDialog: GameDialogUpgrade): Boolean {
        val piece = upgradeDialog.piece
        val property = upgradeDialog.property
        val level = upgradeDialog.level

        val paperPiece = piece.attachment<PaperPiece>()
        val paperProperty = property.attachment<PaperZoneProperty>()
        val channel = Channel<Boolean>()

        withContext(Dispatchers.Heartbeat) {
            if (paperPiece.player == null) {
                channel.trySend(false)
                return@withContext
            }

            newDialog(piece) {
                val info = when (val value = upgradeDialog.level.value) {
                    0 -> "땅" to "구입"
                    1 -> "빌라" to "건설"
                    2 -> "빌딩" to "건설"
                    3 -> "호텔" to "건설"
                    4 -> "랜드마크" to "건설"
                    else -> error("Unknown property level $value")
                }
                message {
                    Component.text("부동산을 클릭하여 업그레이드하세요")
                }
                actionMessage {
                    val text = Component.text()
                    text.append(Component.text(info.first))
                    text.append(Component.space())
                    text.append(Component.text(info.second))
                    text.append(Component.space())
                    text.append(Component.text("비용: "))
                    text.append(Component.text(level.costs).color(NamedTextColor.DARK_GREEN))
                    text.build()
                }
                button(paperProperty.box) {
                    actionMessage {
                        Component.text("확인").color(NamedTextColor.DARK_AQUA)
                    }
                    onClick { _, _, _ ->
                        channel.trySend(true)
                        disposeCurrentDialog()
                    }
                }
                button(PaperGameConfig.centerBox) {
                    actionMessage {
                        Component.text("취소").color(NamedTextColor.RED)
                    }
                    onClick { _, _, _ ->
                        channel.trySend(false)
                        disposeCurrentDialog()
                    }
                }
                timeout(Component.text("부동산 업그레이드"), 15L * 1000L) {
                    channel.trySend(false)
                }
            }
        }

        return channel.receive()
    }

    private suspend fun acquisition(acqDialog: GameDialogAcquisition): Boolean {
        val piece = acqDialog.piece
        val property = acqDialog.property

        val paperPiece = piece.attachment<PaperPiece>()
        val paperProperty = property.attachment<PaperZoneProperty>()
        val channel = Channel<Boolean>()

        withContext(Dispatchers.Heartbeat) {
            if (paperPiece.player == null) {
                channel.trySend(false)
                return@withContext
            }

            newDialog(piece) {
                message {
                    Component.text("부동산을 클릭하여 인수하세요")
                }
                actionMessage {
                    val text = Component.text()
                    text.append(Component.text("인수 비용: "))
                    text.append(Component.text(acqDialog.costs).color(NamedTextColor.DARK_GREEN))
                    text.build()
                }
                button(paperProperty.box) {
                    actionMessage {
                        Component.text("확인")
                    }
                    onClick { _, _, _ ->
                        channel.trySend(true)
                        disposeCurrentDialog()
                    }
                }
                button(PaperGameConfig.centerBox) {
                    actionMessage {
                        Component.text("취소")
                    }
                    onClick { _, _, _ ->
                        channel.trySend(false)
                        disposeCurrentDialog()
                    }
                }
                timeout(Component.text("부동산 인수"), 10L * 1000L) {
                    channel.trySend(false)
                }
            }
        }

        return channel.receive()
    }

    private suspend fun seizure(seizureDialog: GameDialogSeizure): List<ZoneProperty> {
        val piece = seizureDialog.piece
        val properties = piece.properties

        val paperProperties = properties.map { it.attachment<PaperZoneProperty>() }

        val required = seizureDialog.requiredAmount

        return withContext(Dispatchers.Heartbeat) {
            val defaultSelected = seizureDialog.default()
            val selected = mutableSetOf<PaperZoneProperty>()
            var confirm = false

            newDialog(piece) {
                message {
                    Component.text("매각할 부동산을 선택하세요")
                }
                actionMessage {
                    val text = Component.text()
                    text.append(Component.text("필요금액 "))
                    text.append(
                        Component.text(piece.balance + selected.sumOf { it.zone.assets })
                            .color(NamedTextColor.DARK_GREEN)
                    )
                    text.append(Component.text("/"))
                    text.append(Component.text(required).color(NamedTextColor.DARK_RED))
                    text.build()
                }

                for (paperProperty in paperProperties) {
                    button(paperProperty.box) {
                        display(
                            paperProperty.tollsTag.location.apply { y -= 0.25 },
                            Component.text("매각금액: ${paperProperty.zone.assets}").color(NamedTextColor.RED)
                        )

                        actionMessage {
                            val text = Component.text()
                            text.content(paperProperty.name)

                            if (paperProperty !in selected) {
                                text.append(Component.text(" 선택 "))
                                    .append(
                                        Component.text("+${paperProperty.zone.assets}").color(NamedTextColor.DARK_AQUA)
                                    )
                            } else {
                                text.append(Component.text(" 선택취소 "))
                                    .append(Component.text("-${paperProperty.zone.assets}").color(NamedTextColor.RED))
                            }
                            text.build()
                        }

                        onClick { _, _, _ ->
                            if (selected.add(paperProperty)) {
                                if (piece.balance + selected.sumOf { it.zone.assets } >= required) {
                                    confirm = true
                                    disposeCurrentDialog()
                                }
                            } else {
                                selected.remove(paperProperty)
                            }
                        }
                    }
                }

                button(PaperGameConfig.centerBox) {
                    actionMessage {
                        val listString = buildString {
                            defaultSelected.forEach {
                                if (isNotEmpty()) append(", ")
                                append(it.attachment<PaperZoneProperty>().name)
                            }
                        }

                        Component.text().content("자동선택($listString) ")
                            .append(
                                Component.text("+${defaultSelected.sumOf { it.assets }}")
                                    .color(NamedTextColor.DARK_AQUA)
                            )
                            .build()
                    }

                    onClick { _, _, _ ->
                        selected.apply {
                            clear()
                            addAll(defaultSelected.map { it.attachment() })
                        }
                        confirm = true
                        disposeCurrentDialog()
                    }
                }

                timeout(Component.text("강제 매각"), 60L * 1000L) {
                    selected.apply {
                        clear()
                        addAll(defaultSelected.map { it.attachment() })
                    }
                    confirm = true
                    disposeCurrentDialog()
                }
            }

            while (isActive && !confirm) {
                paperProperties.forEach {
                    it.playSelectionEffect(it in selected)
                }
                delay(1L)
            }

            selected.map { it.zone }
        }
    }

    private suspend fun betting(bettingDialog: GameDialogBetting): Int {
        val max = bettingDialog.max
        val piece = bettingDialog.piece

        return withContext(Dispatchers.Heartbeat) {
            val channel = Channel<Int>()

            newDialog(piece) {
                message {
                    Component.text("채팅창에 배팅 금액을 입력하세요")
                }
                actionMessage {
                    Component.text(("최대 배팅 가능 금액: $max"))
                }
                terminal { text ->
                    text.toIntOrNull()?.let { value ->
                        channel.trySend(value.coerceIn(1, max))
                        disposeCurrentDialog()
                    }
                }
                button(PaperGameConfig.centerBox) {
                    actionMessage {
                        Component.text("취소")
                    }
                    onClick { _, _, _ ->
                        channel.trySend(0)
                        disposeCurrentDialog()
                    }
                }
                timeout(Component.text("배팅"), 15L * 1000L) {
                    channel.trySend(0)
                    disposeCurrentDialog()
                }
            }

            channel.receive()
        }
    }

    private suspend fun magic(magicDialog: GameDialogMagic): Magic {
        val magics = magicDialog.magics
        val paperPiece = magicDialog.piece.attachment<PaperPiece>()

        return withContext(Dispatchers.Heartbeat) {
            val loc = paperPiece.stand.location
            val infoMap = magics.associateWith {
                when(it) {
                    Magic.Angel -> "천사" to "통행료 1회 면제"
                    Magic.Arrest -> "긴급체포" to "즉시 감옥으로 이동"
                    Magic.Earthquake -> "지진" to "지정한 부동산 등급 하락"
                    Magic.GiftProperty -> "마음의 선물" to "자신의 부동산 하나 증여"
                    Magic.Moonwalk -> "문워크" to "주사위를 던져 뒤로 이동"
                    Magic.MoveToSeoul -> "서울구경" to "서울로 즉시 이동"
                    Magic.MoveToStart -> "초심찾기" to "시작지점으로 이동"
                    Magic.Overprice -> "바가지 요금" to "지정한 부동산 통행료 x2"
                    Magic.Pickpocket -> "소매치기" to "주사위를 하나 던져 지나가며 소매치기"
                    Magic.Punishment -> "천벌" to "감옥에 있는 대상을 자신의 부동산으로 소환"
                    Magic.Push -> "밀치기" to "지정한 대상을 뒤로 한칸 이동"
                    Magic.QuadrupleDice -> "쿼드러플 주사위" to "다음 주사위는 4개"
                    Magic.SingleDice -> "싱글 주사위" to "다음 주사위는 1개"
                    Magic.Storm -> "폭풍우" to "무작위 땅으로 즉시 이동"
                    Magic.TripleDice -> "트리플 주사위" to "다음 주사위는 3개"
                }
            }
            var currentMagic: Magic = magics.random()
            var stop = false

            newDialog(magicDialog.piece) {
                button(PaperGameConfig.centerBox) {
                    onClick { _, _, _ ->
                        stop = true
                        disposeCurrentDialog()
                    }
                }
                timeout(Component.text("마법"), 5L * 1000L) {
                    stop = true
                }
            }

            do {
                delay(1L)
                val info = infoMap[currentMagic] ?: "null" to "null"

                Bukkit.getServer().showTitle(
                    Title.title(
                        Component.text(info.first).color(NamedTextColor.GOLD),
                        Component.text("보드를 클릭하여 마법을 선택하세요"),
                        Title.Times.of(
                            Duration.ofMillis(0),
                            Duration.ofSeconds(1),
                            Duration.ofMillis(250)
                        )
                    )
                )
                loc.playSound(Sound.ENTITY_ARROW_HIT_PLAYER, 1.0F)

                if (stop) break

                currentMagic = magics.random()
            } while (true)

            val info = infoMap[currentMagic] ?: "null" to "null"
            Bukkit.getServer().showTitle(
                Title.title(
                    Component.text(info.first).color(NamedTextColor.GOLD),
                    Component.text(info.second),
                    Title.Times.of(
                        Duration.ofMillis(0),
                        Duration.ofSeconds(2),
                        Duration.ofMillis(250)
                    )
                )
            )

            delay(2000L)

            currentMagic
        }
    }

    private suspend fun targetZone(targetZoneDialog: GameDialogTargetZone): Zone {
        val piece = targetZoneDialog.piece
        val channel = Channel<Zone>()
        val messageText = messageOf(targetZoneDialog.message)

        withContext(Dispatchers.Heartbeat) {
            newDialog(piece) {
                message {
                    Component.text(messageText.first)
                }
                for (target in targetZoneDialog.candidates) {
                    val paperZone = target.attachment<PaperZone>()

                    button(paperZone.box) {
                        actionMessage { Component.text(paperZone.name) }

                        onClick { _, _, _ ->
                            channel.trySend(target)
                            disposeCurrentDialog()
                        }
                    }
                }
                timeout(Component.text(messageText.second), 10L * 1000L) {
                    channel.trySend(targetZoneDialog.default())
                }
            }
        }

        return channel.receive()
    }

    private suspend fun targetPiece(targetPieceDialog: GameDialogTargetPiece): Piece {
        val piece = targetPieceDialog.piece
        val channel = Channel<Piece>()
        val messageText = messageOf(targetPieceDialog.message)

        withContext(Dispatchers.Heartbeat) {
            newDialog(piece) {
                message {
                    Component.text(messageText.first)
                }
                for (target in targetPieceDialog.candidates) {
                    val paperPiece = target.attachment<PaperPiece>()

                    button(paperPiece.stand.bukkitEntity.boundingBox.expand(0.5)) {
                        actionMessage { paperPiece.name }

                        onClick { _, _, _ ->
                            channel.trySend(target)
                            disposeCurrentDialog()
                        }
                    }
                }
                timeout(Component.text(messageText.second), 10L * 1000L) {
                    channel.trySend(targetPieceDialog.default())
                }
            }
        }

        return channel.receive()
    }
    
    private fun messageOf(message: GameMessage) = when(message) {
        GameMessage.ROLL_THE_DICE -> "보드를 클릭하여 주사위를 굴리세요" to "주사위"
        GameMessage.ACQUISITION -> "부동산을 클릭하여 인수하세요" to "인수"
        GameMessage.UPGRADE -> "부동산을 클릭하여 업그레이드하세요" to "부동산 업그레이드"
        GameMessage.SEIZURE -> "매각할 부동산을 선택하세요" to "부동산 매각"
        GameMessage.BETTING -> "배팅할 금액을 채팅창에 입력하세요" to "금액 배팅"
        GameMessage.ROLL_THE_DICE_FOR_GAMBLE -> "보드를 클릭하여 운명의 주사위를 굴리세요" to "운명의 주사위"
        GameMessage.ZONE_FOR_PORTAL -> "이동할 위치를 선택해주세요" to "포탈"
        GameMessage.TAX -> "국세청을 클릭하여 납부할 금액을 설정하세요" to "국세청"
        GameMessage.PIECE_FOR_PUSH -> "밀쳐낼 대상을 선택하세요" to "대상 선택"
        GameMessage.ZONE_FOR_OVERPRICE -> "바가지 요금을 적용할 부동산을 선택하세요" to "부동산 선택"
        GameMessage.PIECE_FOR_GIFT_PROPERTY -> "증여할 대상을 선택하세요" to "대상 선택"
        GameMessage.ZONE_FOR_GIFT_PROPERTY -> "증여할 부동산을 선택하세요" to "자신의 부동산 선택"
        GameMessage.ROLL_THE_DICE_FOR_MOONWALK -> "보드를 클릭하여 문워크 주사위를 굴리세요" to "문워크 주사위"
        GameMessage.ZONE_FOR_EARTHQUAKE -> "지진을 일으킬 부동산을 선택하세요" to "부동산 선택"
        GameMessage.MAGIC -> "보드를 클릭하여 마법을 선택하세요" to "마법"
    }

    private suspend fun tax(taxDialog: GameDialogTax): Int {
        val piece = taxDialog.piece
        val paperPiece = piece.attachment<PaperPiece>()
        val zone = taxDialog.zone
        val paperZone = zone.attachment<PaperZoneNTS>()

        return withContext(Dispatchers.Heartbeat) {
            val location = paperPiece.stand.location
            var value = taxDialog.max.toDouble()
            var speed = 0.0
            var lastAmount = 0

            var amount = value.roundToInt()
            var stop = false

            newDialog(piece) {
                button(paperZone.box) {
                    onClick { _, _, _ ->
                        stop = true
                        disposeCurrentDialog()
                    }
                }
            }

            while (amount > 0) {
                delay(1L)

                value -= speed
                speed += 0.04

                amount = max(0, value.roundToInt())

                Bukkit.getServer().showTitle(
                    Title.title(
                        Component.text(amount).color(NamedTextColor.DARK_GREEN).decorate(TextDecoration.BOLD),
                        Component.text("국세청을 클릭하여 납부액을 결정하세요"),
                        Title.Times.of(
                            Duration.ofMillis(0),
                            Duration.ofSeconds(1),
                            Duration.ofMillis(250)
                        )
                    )
                )

                if (amount != lastAmount) {
                    lastAmount = amount
                    location.playSound(Sound.BLOCK_NOTE_BLOCK_SNARE, 0.1F)
                }

                if (stop) break
            }

            disposeCurrentDialog()

            if (!stop) {
                amount = taxDialog.max
                location.playSound(Sound.ENTITY_PIG_DEATH, 1.0F)
            }

            Bukkit.getServer().showTitle(
                Title.title(
                    Component.text(amount).color(NamedTextColor.DARK_GREEN).decorate(TextDecoration.BOLD),
                    Component.empty(),
                    Title.Times.of(
                        Duration.ofMillis(0),
                        Duration.ofSeconds(2),
                        Duration.ofMillis(250)
                    )
                )
            )
            delay(2000L)
            amount
        }
    }
}