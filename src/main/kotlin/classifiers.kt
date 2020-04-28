package com.peanutcode.pison_challenge

import kotlin.math.abs

/**
 * A [Classifier] transforms a sequence of [Sample]s into a sequence of [Unit]s
 * that represent activations.
 */
typealias Classifier = (Sequence<Sample>) -> Sequence<Unit>

/**
 * Classifies activations in a sequence of samples by examining the `label`
 * field (cheating). Emits an activation any time the sequence transitions from
 * REST->ACTIVATION.
 */
fun cheatLeadingEdge(samples: Sequence<Sample>): Sequence<Unit> {
    return samples
        .map { it.label }
        .leadingEdge()
        .filter { it == "ACTIVATION" }
        .map { Unit }
}

/**
 * Classifies activations in a sequence of samples by examining the `label`
 * field (cheating). Emits an activation any time the sequence transitions from
 * ACTIVATION->REST.
 */
fun cheatTrailingEdge(samples: Sequence<Sample>): Sequence<Unit> {
    return samples
        .map { it.label }
        .trailingEdge()
        .filter { it == "ACTIVATION" }
        .map { Unit }
}

/**
 * Classifies activations in a sequence of samples by comparing the rolling
 * average with [threshold] in a window given by [windowSize] and [step].
 */
fun alpha(threshold: Double, windowSize: Int, step: Int = 1): Classifier {
    return { seq ->
        seq
            .windowed(windowSize, step) { window ->
                window.map { abs(it.data) }.average() > threshold
            }
            .trailingEdge()
            .filter { it }
            .map { Unit }
    }
}

/**
 * Classifies activations in a sequence of samples by counting samples that
 * exceed [threshold] in a rolling window given by [windowSize] and [step].
 */
fun beta(threshold: Double, windowSize: Int, step: Int = 1): Classifier {
    return { seq ->
        seq
            .windowed(windowSize, step) { window ->
                window.map { abs(it.data) }.all { it > threshold }
            }
            .trailingEdge()
            .filter { it }
            .map { Unit }
    }
}

/**
 * Classifies activations in a sequence of samples by:
 *
 * First, discarding all values between -[noiseFloor]..[noiseFloor]
 * Then, in a window given by [windowSize]:
 * - counting the number of zero-crossings and comparing them against [crossCountThreshold]
 * - counting the number of "jerks" (movements at least as large as [jerkSizeThreshold])
 *   and comparing them against [jerkCountThreshold].
 */
fun gamma(
    noiseFloor: Int,
    windowSize: Int,
    crossCountThreshold: Int,
    jerkSizeThreshold: Int,
    jerkCountThreshold: Int
): Classifier {
    return { seq ->
        seq
            .map { it.data }
            .map { if (abs(it) < noiseFloor) { 0 } else { it } }
            .windowed(windowSize) {
                val zeroCrossings = it
                    .windowed(2)
                    .fold(0) { acc, x ->
                        acc + if (oppositeSigns(x[0], x[1])) { 1 } else { 0}
                    }
                val jerks = it
                    .windowed(2)
                    .fold(0) { acc, x ->
                        acc + if (abs(x[0] - x[1]) > jerkSizeThreshold) { 1 } else { 0}
                    }
                zeroCrossings >= crossCountThreshold && jerks >= jerkCountThreshold
            }
            .trailingEdge()
            .filter { it }
            .map { Unit }
    }
}

/**
 * Returns true when the signs of [a] and [b] differ.
 *
 * Zero never has the opposite sign of any other number.  That is:
 *
 * oppositeSigns( 1, -1) -> true
 * oppositeSigns( 1,  0) -> false
 * oppositeSigns(-1,  0) -> false
 * oppositeSigns( 0,  0) -> false
 */
fun oppositeSigns(a: Int, b: Int): Boolean {
    return (a * b) < 0
}
