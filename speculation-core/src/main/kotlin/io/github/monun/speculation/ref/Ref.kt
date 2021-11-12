package io.github.monun.speculation.ref

import java.lang.ref.WeakReference
import kotlin.reflect.KProperty


fun <T> upstream(t: T): T {
    val wrapped: T by WeakReference(t)
    return wrapped
}

operator fun <T> WeakReference<T>.getValue(v: Any?, property: KProperty<*>): T {
    return requireNotNull(get()) { "Cannot get reference as it has already been Garbage Collected" }
}
