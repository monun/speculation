package io.github.monun.speculation.paper

import io.github.monun.speculation.paper.util.playSound
import io.github.monun.tap.fake.FakeEntity
import io.github.monun.tap.fake.FakeEntityServer
import io.github.monun.tap.math.toRadians
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import org.bukkit.util.EulerAngle
import org.bukkit.util.Vector
import kotlin.math.round
import kotlin.random.Random

class Dice(fakeEntityServer: FakeEntityServer, loc: Location) {
    companion object {
        const val stateBeforeRoll = 0
        const val stateRolling = 1
        const val stateOnGround = 2

        private val faces = listOf(
            DiceFace(1, Vector(0.0, 1.0, 0.0)),
            DiceFace(2, Vector(-1.0, 0.0, 0.0)),
            DiceFace(3, Vector(0.0, 0.0, 1.0)),
            DiceFace(4, Vector(1.0, 0.0, 0.0)),
            DiceFace(5, Vector(0.0, 0.0, -1.0)),
            DiceFace(6, Vector(0.0, -1.0, 0.0))
        )

        fun calculateDiceValue(angle: EulerAngle): DiceFace {
            val rotX = angle.x
            val rotY = angle.y
            val rotZ = angle.z

            return faces.maxByOrNull { face ->
                face.vector.apply {
                    rotateAroundX(rotX)
                    rotateAroundY(-rotY)
                    rotateAroundZ(-rotZ)
                }.y
            }!!
        }
    }

    private val stand: FakeEntity = fakeEntityServer.spawnEntity(loc, ArmorStand::class.java).apply {
        updateMetadata<ArmorStand> {
            headPose = EulerAngle.ZERO
            isInvisible = true
            isMarker = true
        }
        updateEquipment {
            setHelmet(ItemStack(Material.PAPER).apply {
                editMeta {
                    it.setCustomModelData(1000)
                }
            }, true)
        }
    }

    var owner: Pair<PaperPiece, Vector>? = null
    private val location: Location = loc.clone().apply { yaw = 0.0F; pitch = 0.0F }
    private var rotation: EulerAngle = EulerAngle.ZERO
    private var rotationSpeed = 1.0
    private var state = stateBeforeRoll
    private var removeTicks = 0

    var value = -1
        private set

    private lateinit var velocity: Vector

    var isValid = true
        private set

    val isBeforeRoll
        get() = state == stateBeforeRoll
    val isOnGround
        get() = state == stateOnGround

    fun roll(velocity: Vector) {
        this.state = stateRolling
        this.velocity = velocity.clone()

        val power = 1.0
        rotation = EulerAngle(Random.nextDouble(power) - power / 2, Random.nextDouble(power) - power / 2, 0.0)
    }

    fun onUpdate() {
        if (removeTicks > 0 && --removeTicks <= 0) {
            remove()
            return
        }
        when (state) {
            stateBeforeRoll -> {
                val owner = owner ?: return
                val player = owner.first.player ?: return
                val location = player.location
                val offset = owner.second
                this.location.apply {
                    x = location.x
                    y = location.y
                    z = location.z
                    add(offset.clone().rotateAroundY(-location.yaw.toDouble().toRadians()))
                }
            }
            stateRolling -> {
                velocity.y -= 0.15
                location.add(velocity)
                stand.updateMetadata<ArmorStand> {
                    headPose =
                        headPose.add(rotation.x * rotationSpeed, rotation.y * rotationSpeed, rotation.z * rotationSpeed)
                }

                if (location.y < PaperGameConfig.boardY) {
                    val speed = velocity.length()

                    if (speed > 0.2) {
                        if (velocity.y < 0.0) {
                            velocity.multiply(0.4 + Random.nextDouble() * 0.2)
                            velocity.y *= -1.0
                            rotationSpeed *= 0.8

                            location.playSound(Sound.BLOCK_WOOD_STEP, 1.0F)
                        }
                    } else {
                        state = stateOnGround
                        location.y = PaperGameConfig.boardY
                        stand.updateMetadata<ArmorStand> {
                            val headPose = this.headPose
                            val ra = 90.0.toRadians()
                            val x = headPose.x / ra
                            val z = headPose.z / ra

                            val newAngle = EulerAngle(
                                round(x) * ra,
                                headPose.y,
                                round(z) * ra
                            )
                            value = calculateDiceValue(newAngle).number
                            this.headPose = newAngle
                        }
                    }
                }
            }
        }

        stand.moveTo(location)
    }

    fun remove(ticks: Int) {
        this.removeTicks = ticks
    }

    fun remove() {
        isValid = false
        stand.remove()
    }
}

class DiceFace(val number: Int, v: Vector) {
    private val _vector = v

    val vector: Vector
        get() = _vector.clone()
}