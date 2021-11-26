package io.github.monun.speculation.game.event

class GameEventAdapter {
    private val pipes = HashMap<Class<out GameEvent>, suspend (GameEvent) -> Unit>()

    @Suppress("UNCHECKED_CAST")
    fun <T : GameEvent> register(type: Class<T>, action: suspend (T) -> Unit) {
        pipes[type] = action as suspend (GameEvent) -> Unit
    }

    @Suppress("UNCHECKED_CAST")
    internal suspend fun call(event: GameEvent) {
        pipes[event.javaClass]?.invoke(event)
    }
}