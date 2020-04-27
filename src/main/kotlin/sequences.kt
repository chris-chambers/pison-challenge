package com.peanutcode.pison_challenge

/**
 * Returns a sequence of results sorted within a given window.  Sorting is
 * performed by comparing the results of applying [selector] to items.  Items
 * that are out of position by more than [windowSize] slots are passed to the
 * [late] callback, if supplied.  The [late] callback can perform whatever
 * side effect desired, then return true to emit the item out of order or false
 * to elide it.
 */
fun <T, R : Comparable<R>> Sequence<T>.sortedByInWindow(
    windowSize: Int,
    selector: (T) -> R,
    late: ((T) -> Boolean)? = null) : Sequence<T>
{
    if (windowSize < 0) {
        throw IllegalArgumentException("windowSize must be non-negative")
    } else if (windowSize == 0) {
        return this
    }

    val iter = this.iterator()
    if (!iter.hasNext()) {
        return emptySequence()
    }

    val buf = mutableListOf<T>()
    while (buf.size < windowSize && iter.hasNext()) {
        buf.add(iter.next())
    }

    buf.sortBy(selector)
    if (!iter.hasNext()) {
        return buf.asSequence()
    }

    var maxSeen = selector(buf[buf.size - 1])
    return sequence {
        for (s in iter) {
            val value = selector(s)
            if (value >= maxSeen) {
                maxSeen = value
                yield(buf.removeAt(0))
                buf.add(s)
            } else {
                val result = buf.binarySearch { selector(it).compareTo(value) }
                val index = if (result >= 0) result else result.inv()
                if (index == 0) {
                    if (late != null && late(s)) {
                        yield(s)
                    }
                } else {
                    yield(buf.removeAt(0))
                    buf.add(index - 1, s)
                }
            }
        }

        yieldAll(buf)
    }
}
