package io.github.monun.speculation.paper

import io.github.monun.tap.fake.FakeEntity
import io.github.monun.tap.fake.FakeEntityServer
import io.github.monun.tap.math.copy
import io.github.monun.tap.math.toRadians
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import org.bukkit.util.EulerAngle
import org.bukkit.util.Vector
import kotlin.math.round
import kotlin.random.Random

class Dice(fakeEntityServer: FakeEntityServer, loc: Location) {
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

    private val location: Location = loc.clone().apply { yaw = 0.0F; pitch = 0.0F }
    private val rotation: EulerAngle
    private var rotationSpeed = 1.0
    var isRolled = false
        private set
    var isOnGround = false
        private set
    var value = -1
        private set

    private lateinit var velocity: Vector

    init {
        val power = 1.0
        rotation = EulerAngle(Random.nextDouble(power) - power / 2, Random.nextDouble(power) - power / 2, 0.0)
    }

    fun roll(velocity: Vector) {
        isRolled = true
        this.velocity = velocity.clone()
    }

    fun moveTo(to: Location) {
        location.copy(to)
        stand.moveTo(to)
    }

    fun onUpdate() {
        if (!isRolled || isOnGround) return

        velocity.y -= 0.15
        location.add(velocity)

        if (location.y < PaperGameConfig.boardY) {
            val speed = velocity.length()

            if (speed > 0.2) {
                if (velocity.y < 0.0) {
                    velocity.multiply(0.4 + Random.nextDouble() * 0.2)
                    velocity.y *= -1.0
                    rotationSpeed *= 0.8
                }
            } else {
                isOnGround = true
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

        stand.moveTo(location)

        if (!isOnGround) {
            stand.updateMetadata<ArmorStand> {
                headPose =
                    headPose.add(rotation.x * rotationSpeed, rotation.y * rotationSpeed, rotation.z * rotationSpeed)
            }
        }
    }

    fun remove() {
        stand.remove()
    }

    companion object {
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
}

class DiceFace(val number: Int, v: Vector) {
    private val _vector = v

    val vector: Vector
        get() = _vector.clone()
}