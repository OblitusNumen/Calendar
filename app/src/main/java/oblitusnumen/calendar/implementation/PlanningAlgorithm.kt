package oblitusnumen.calendar.implementation

import oblitusnumen.calendar.implementation.data.tables.TaskLink
import java.util.*
import kotlin.math.max
import kotlin.math.min

class Task(val index: Int, val duration: Int, var startLimit: Int, var endLimit: Int) {
    var start: Int = -1
    var end: Int = -1
    var avg: Int = -1
}

fun planTasks(
    tasks: Array<oblitusnumen.calendar.implementation.data.tables.Task>,
    links: Collection<TaskLink>,
    now: Long
): Map<Int, Array<Int>> {
    if (tasks.isEmpty()) return emptyMap()
    // id translations
    val indexTranslation: Map<Int, Int> = tasks.withIndex().associateBy({ it.value.taskId!! }, { it.index })
    val indexTranslationReverse: Map<Int, Int> = tasks.withIndex().associateBy({ it.index }, { it.value.taskId!! })
    // FIXME: filtering links
    val links =
        links.filter { indexTranslation.containsKey(it.predecessor) && indexTranslation.containsKey(it.successor) }
            .map { TaskLink(indexTranslation[it.predecessor]!!, indexTranslation[it.successor]!!) }
    val tasks = Array(tasks.size) { idx ->
        val task = tasks[idx]
        Task(
            idx, task.timeRemaining,
            // FIXME: calculating limits
            task.startConstraintTimestamp?.let { (it - now + 86399) / 86400 }?.toInt()?.let { if (it < 0) 0 else it }
                ?: 0,
            ((task.deadlineTimestamp - now + 86399) / 86400).toInt()
        )
    }

    // initialization and checks
    val predecessorLinks: Array<MutableList<Int>> = Array(tasks.size) { mutableListOf() }
    val successorsLinks: Array<MutableList<Int>> = Array(tasks.size) { mutableListOf() }

    // link[] => link.a -> link.b[]
    links.forEach { link ->
        successorsLinks[link.predecessor].add(link.successor)
        predecessorLinks[link.successor].add(link.predecessor)
    }
    val predecessors = initLinks(tasks.size, predecessorLinks)
    checkRecursiveLinks(predecessors)
    val successors = initLinks(tasks.size, successorsLinks)
    sanitizeConstraints(tasks, predecessors, successors)
    checkIllegalLinks(tasks)
    // TODO: check for start > end

    // count average
    tasks.forEach { task ->
        task.avg = task.duration / (task.endLimit - task.startLimit + 1)
    }

    // algorithm initialization
    val dayCount = tasks.maxOf { it.endLimit } + 1
    val time: Array<Array<Int>> = Array(tasks.size) { Array(dayCount) { 0 } }
    // valid initial solution
    repeat(tasks.size) { index ->
        time[index][tasks[index].startLimit] = tasks[index].duration
        tasks[index].start = tasks[index].startLimit
        tasks[index].end = tasks[index].startLimit
    }

    // algorithm
    solveAlgo(tasks, time, dayCount, successorsLinks, predecessorLinks)

    // calculating real duration
    tasks.forEach { task ->
        var i = 0
        while (time[task.index][i] == 0) i++
        task.start = i

        i = dayCount - 1
        while (time[task.index][i] == 0) i--
        task.end = i
    }

    checkResult(tasks, links, time)

    // id translation
    return time.withIndex().associateBy({ indexTranslationReverse[it.index]!! }, { it.value })
}

// flatten tree
fun initLinks(
    tasksSize: Int,
    links: Array<out List<Int>>
): Array<MutableSet<Int>> {
    val linkSets: Array<MutableSet<Int>> = Array(tasksSize) { mutableSetOf() }
    repeat(tasksSize) { taskId ->
        val stack = Stack<MutableList<Int>>()
        val copy = mutableListOf<Int>()
        copy.addAll(links[taskId])
        stack.push(copy)
        while (stack.isNotEmpty()) {
            if (stack.peek().isEmpty()) {
                stack.pop()
                continue
            }
            val first = stack.peek().removeAt(0)
            linkSets[taskId].add(first)
            if (linkSets[first].isNotEmpty()) {
                linkSets[taskId].addAll(linkSets[first])
                continue
            }
            val copy = mutableListOf<Int>()
            copy.addAll(links[first])
            stack.push(copy)
        }
    }
    return linkSets
}

fun sanitizeConstraints(
    tasks: Array<Task>,
    predecessors: Array<MutableSet<Int>>,
    successors: Array<MutableSet<Int>>
) {
    tasks.forEach { task ->
        var maxStart = task.startLimit
        predecessors[task.index].forEach { predecessor ->
            if (maxStart < tasks[predecessor].startLimit)
                maxStart = tasks[predecessor].startLimit
        }
        task.startLimit = maxStart
        var minEnd = task.endLimit
        successors[task.index].forEach { successor ->
            if (minEnd > tasks[successor].endLimit)
                minEnd = tasks[successor].endLimit
        }
        task.endLimit = minEnd
    }
}

fun checkRecursiveLinks(predecessors: Array<MutableSet<Int>>) {
    predecessors.forEachIndexed { index, ints ->
        if (ints.contains(index))
            throw IllegalArgumentException("Recursive links detected")
    }
}

fun checkIllegalLinks(tasks: Array<Task>) {
    tasks.forEach {
        if (it.startLimit > it.endLimit)
            throw IllegalArgumentException("Illegal links detected")
    }
}

fun solveAlgo(
    tasks: Array<Task>,
    time: Array<Array<Int>>,
    dayCount: Int,
    successorsLinks: Array<MutableList<Int>>,
    predecessorLinks: Array<MutableList<Int>>,
) {
    val tasksByEndDate = tasks.sortedBy { -it.endLimit }
    val tasksByStartDate = tasks.sortedBy { it.startLimit }

    val avgMin: Array<Int> = Array(dayCount) { day ->
        Array(tasks.size) {
            val task = tasks[it]
            if (task.startLimit <= day && day <= task.endLimit)
                task.avg
            else
                0
        }.maxOf { it }
    }

    rangeOptimizingAlgorithm(
        0,
        dayCount - 1,
        dayCount,
        time,
        tasksByEndDate,
        tasksByStartDate,
        successorsLinks,
        predecessorLinks,
        tasks,
        avgMin,
    )

    val tasksByStartByEnd = tasks.groupBy { it.startLimit }
        .mapValues { (_, list) ->
            list.sortedBy { -it.endLimit }
        }
        .toSortedMap()
    for (tasksByEnd in tasksByStartByEnd) {
        val lastPeak = tasksByEnd.key
        var i = lastPeak
        while (i < dayCount - 2) {
            if (sumByDay(time, i) > sumByDay(time, i + 1) + i - lastPeak + 1) {
                for (task in tasksByEnd.value) {
                    rangeOptimizingAlgorithm(
                        lastPeak,
                        task.endLimit,
                        dayCount,
                        time,
                        tasksByEndDate,
                        tasksByStartDate,
                        successorsLinks,
                        predecessorLinks,
                        tasks,
                        avgMin,
                    )
                }
                break
            }
            if (!tasksByStartByEnd.containsKey(i))
                break
            i++
        }
    }
}

fun sumByDay(time: Array<Array<Int>>, day: Int): Int {
    var sum = 0
    time.forEach { sum += it[day] }
    return sum
}

fun rangeOptimizingAlgorithm(
    from: Int,
    to: Int,
    dayCount: Int,
    time: Array<Array<Int>>,
    tasksByEndDate: List<Task>,
    tasksByStartDate: List<Task>,
    successorsLinks: Array<MutableList<Int>>,
    predecessorLinks: Array<MutableList<Int>>,
    tasks: Array<Task>,
    avgMin: Array<Int>,
): Boolean {
    val count = to - from + 1
    val avg0 = Array(count) { sumByDay(time, it + from) }.sumOf { it } / count
    val avg = Array(count) { max(avg0, avgMin[it + from]) }

    var i = 0

    while (true) {
        var exit = true
        var day = from
        while (day < to) {
            var toMove = (sumByDay(time, day) - avg[day - from]) / 2
            if (toMove <= 0) {
                day++
                continue
            }
            for (task in tasksByEndDate) {
                if (day < task.startLimit || day >= task.endLimit) continue
                var locked = false
                for (successor in successorsLinks[task.index]) {
                    if (tasks[successor].start <= day) {
                        locked = true
                        break
                    }
                }
                if (locked) continue
                val move = min(toMove, time[task.index][day])
                if (move == 0) continue
                exit = false
                toMove -= move
                time[task.index][day] -= move
                time[task.index][day + 1] += move
                if (time[task.index][day] == 0 && task.start == day)
                    task.start++
                if (task.end == day)
                    task.end++
                if (toMove <= 0)
                    break
            }
            day++
        }

        i++
        if (exit)
            break
    }

    val direct = i
//    --------------------reverse------------------------

    i = 0
    while (true) {
        var exit = true
        var day = to
        while (day > from) {
            var toMove = (sumByDay(time, day) - avg[day - from]) / 2
            if (toMove <= 0) {
                day--
                continue
            }
            for (task in tasksByStartDate) {
                if (day <= task.startLimit || day > task.endLimit) continue
                var locked = false
                for (predecessor in predecessorLinks[task.index]) {
                    if (tasks[predecessor].end >= day) {
                        locked = true
                        break
                    }
                }
                if (locked) continue
                val move = min(toMove, time[task.index][day])
                if (move == 0) continue
                exit = false
                toMove -= move
                time[task.index][day] -= move
                time[task.index][day - 1] += move
                if (time[task.index][day] == 0 && task.end == day)
                    task.end--
                if (task.start == day)
                    task.start--
                if (toMove <= 0)
                    break
            }
            day--
        }

        i++
        if (exit)
            break
    }

    return direct + i > 2
}

fun checkResult(
    tasks: Array<Task>,
    links: List<TaskLink>,
    time: Array<Array<Int>>
) {
    for (task in tasks) {
        val actualSum = time[task.index].sliceArray(task.start..task.end).sum()
        if (actualSum != task.duration) throw RuntimeException("start and end are wrong: $task")
        if (time[task.index].sum() != task.duration) throw RuntimeException("duration unsatisfied: $task")
        if (time[task.index].any({ it < 0 })) throw RuntimeException("negative portions: $task")
        if (task.start < task.startLimit) throw RuntimeException("startLimit unsatisfied: $task")
        if (task.end > task.endLimit) throw RuntimeException("endLimit unsatisfied: $task")
    }
    for (link in links) {
        if (tasks[link.predecessor].end > tasks[link.successor].start) throw RuntimeException("link unsatisfied: $link")
    }
}