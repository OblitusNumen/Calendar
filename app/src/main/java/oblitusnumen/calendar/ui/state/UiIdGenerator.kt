package oblitusnumen.calendar.ui.state

import java.util.concurrent.atomic.AtomicInteger

class UiIdGenerator {
    private val counter = AtomicInteger(0)

    fun next(): String = "element_${counter.incrementAndGet()}"
}