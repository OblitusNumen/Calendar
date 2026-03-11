package oblitusnumen.calendar.implementation.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ListModificationMutableState<T>(initialValue: List<T>) {
    private var cached: List<T> = initialValue
    private val mods: MutableList<T> = mutableListOf()
    private val modValues: MutableMap<T, Modification> = mutableMapOf()
    var value: List<T> by mutableStateOf(cached)
        private set

    val modifications: Map<T, Modification> = modValues

    private fun bake() {
        value = cached.toMutableList().apply {
            for (v in mods) {
                when (modValues[v]!!) {
                    Modification.ADD -> this.add(v)
                    Modification.DELETE -> this.remove(v)
                }
            }
        }
    }

    fun add(vararg vals: T) {
        var update = false
        for (v in vals) {
            if (modValues[v] == Modification.DELETE) {
                modValues.remove(v)
                mods.remove(v)
                update = true
            } else if (!cached.contains(v)) {
                modValues[v] = Modification.ADD
                mods.apply { if (!contains(v)) add(v) }
                update = true
            }
        }
        if (update)
            bake()
    }

    fun rm(vararg vals: T) {
        var update = false
        for (v in vals) {
            if (modValues[v] == Modification.ADD) {
                modValues.remove(v)
                mods.remove(v)
                update = true
            } else if (cached.contains(v)) {
                modValues[v] = Modification.DELETE
                mods.apply { if (!contains(v)) add(v) }
                update = true
            }
        }
        if (update)
            bake()
    }

    fun forEachModification(action: (T, Modification) -> Unit) {
        modValues.forEach { action(it.key, it.value) }
    }

    fun commit() {
        mods.clear()
        modValues.clear()
        cached = value
    }
}