package io.github.monun.speculation.game

abstract class Attachable {
    private var attachment: Any? = null

    fun <T : Any> attach(instance: T) {
        this.attachment = instance
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> attachment(): T {
        return attachment as T
    }
}