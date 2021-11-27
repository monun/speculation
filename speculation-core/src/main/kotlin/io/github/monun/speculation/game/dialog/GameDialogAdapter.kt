package io.github.monun.speculation.game.dialog

class GameDialogAdapter {
    private val pipes = HashMap<Class<out GameDialog<*>>, suspend (GameDialog<*>) -> Any>()

    @Suppress("UNCHECKED_CAST")
    fun <R, T : GameDialog<R>> register(type: Class<T>, function: suspend T.() -> R) {
        pipes[type] = function as suspend (GameDialog<*>) -> Any
    }

    @Suppress("UNCHECKED_CAST")
    internal suspend fun <R> request(dialog: GameDialog<R>): R {
        val function = pipes[dialog::class.java] ?: return dialog.default()

        return function.invoke(dialog) as R
    }
}