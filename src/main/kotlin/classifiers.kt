package com.peanutcode.pison_challenge

/**
 * A [Classifier] transforms a sequence of [Sample]s into a sequence of [Unit]s
 * that represent activations.
 */
typealias Classifier = (Sequence<Sample>) -> Sequence<Unit>

/**
 * Classifies a sequence of samples by examining the `label` field (cheating).
 * Emits an activation any time the sequence transitions from REST->ACTIVATION.
 */
fun cheatLeadingEdge(samples: Sequence<Sample>): Sequence<Unit> {
    var lastWasRest = false
    return samples.filter {
        val emit = lastWasRest && it.label == "ACTIVATION"
        lastWasRest = it.label == "REST"
        emit
    }.map { _ -> Unit }
}

/**
 * Classifies a sequence of samples by examining the `label` field (cheating).
 * Emits an activation any time the sequence transitions from ACTIVATION->REST.
 */
fun cheatTrailingEdge(samples: Sequence<Sample>): Sequence<Unit> {
    var lastWasActivation = false
    return samples.filter {
        val emit = lastWasActivation && it.label == "REST"
        lastWasActivation = it.label == "ACTIVATION"
        emit
    }.map { _ -> Unit }
}
