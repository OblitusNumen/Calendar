package oblitusnumen.calendar.implementation.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class SetModificationMutableState<T>(initialValue: Set<T>) {
    private var cached: Set<T> = initialValue
    private val mods: MutableMap<T, Modification> = mutableMapOf()
    var value: MutableState<Set<T>> = mutableStateOf(cached)
        private set

    val modifications: Map<T, Modification> = mods

    private fun bake() {
        value.value = cached.toMutableSet().apply {
            for (m in mods) {
                when (m.value) {
                    Modification.ADD -> this.add(m.key)
                    Modification.DELETE -> this.remove(m.key)
                }
            }
        }
    }

    fun add(vararg vals: T) {
        var update = false
        for (v in vals) {
            if (mods[v] == Modification.DELETE) {
                mods.remove(v)
                update = true
            } else if (!cached.contains(v)) {
                mods[v] = Modification.ADD
                update = true
            }
        }
        if (update)
            bake()
    }

    fun rm(vararg vals: T) {
        var update = false
        for (v in vals) {
            if (mods[v] == Modification.ADD) {
                mods.remove(v)
                update = true
            } else if (cached.contains(v)) {
                mods[v] = Modification.DELETE
                update = true
            }
        }
        if (update)
            bake()
    }

    fun forEachModification(action: (T, Modification) -> Unit) {
        mods.forEach { action(it.key, it.value) }
    }

    fun commit() {
        mods.clear()
        cached = value.value
    }
}