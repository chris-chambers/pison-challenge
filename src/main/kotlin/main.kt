package com.peanutcode.pison_challenge

import java.io.File
import java.net.Socket
import java.util.Scanner
import kotlin.system.exitProcess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

data class Options(
    val host: String,
    val port: Int,
    val reorderWindowSize: Int,
    val classifierFactory: ClassifierFactory,
    val keepLateSamples: Boolean,
    val printSamples: Boolean,
    val printLateSamples: Boolean,
    val printActivations: Boolean,

    val alpha: AlphaOptions = AlphaOptions(),
    val beta: BetaOptions = BetaOptions(),
    val gamma: GammaOptions = GammaOptions()
)

@Serializable
data class AlphaOptions(
    val threshold: Double = 10000.0,
    val windowSize: Int = 5,
    val step: Int = 1)

@Serializable
data class BetaOptions(
    val threshold: Double = 10000.0,
    val windowSize: Int = 5,
    val step: Int = 1)

@Serializable
data class GammaOptions(
    val noiseFloor: Int = 10000,
    val windowSize: Int = 5,
    val crossCountThreshold: Int = 2,
    val jerkSizeThreshold: Int = 20000,
    val jerkCountThreshold: Int = 2)

@Serializable
data class Sample(val timeStamp: Long, val data: Int, val label: String)

typealias ClassifierFactory = (Options) -> Classifier

val classifiers = hashMapOf<String, ClassifierFactory>(
    "cheat-trailing" to {_ -> ::cheatTrailingEdge },
    "cheat-leading"  to {_ -> ::cheatLeadingEdge },
    "alpha" to { o ->
        val opts = o.alpha
        alpha(opts.threshold, opts.windowSize, opts.step)
    },
    "beta" to { o ->
        val opts = o.beta
        beta(opts.threshold, opts.windowSize, opts.step)
    },
    "gamma" to { o ->
        val opts = o.gamma
        gamma(
            opts.noiseFloor,
            opts.windowSize,
            opts.crossCountThreshold,
            opts.jerkSizeThreshold,
            opts.jerkCountThreshold)
    }
)

fun main(args: Array<String>) {
    try {
        run(parseOptions(args))
    } catch (e: Throwable) {
        println("fatal: ${e.message}")
        exitProcess(2)
    }
}

fun parseOptions(@Suppress("UNUSED_PARAMETER") args: Array<String>): Options {
    val portfile = File(".pison-challenge-port")
    val port = portfile.readText().trim().toInt()

    val classifierName = "gamma"

    return Options(
        host = "localhost",
        port = port,
        reorderWindowSize = 40,
        classifierFactory = classifiers[classifierName]
            ?: throw Exception("no such classifier: $classifierName"),
        keepLateSamples = false,
        printSamples = false,
        printLateSamples = false,
        printActivations = true)
}

fun run(options: Options) {
    val classifier = options.classifierFactory(options)

    val socket = Socket(options.host, options.port)
    val scanner = Scanner(socket.inputStream)
    val output = socket.outputStream.writer()
    val json = Json(JsonConfiguration.Stable)
    val serializer = Sample.serializer()

    scanner.asSequence()
        .map { json.parse(serializer, it) }
        .sortedByInWindow(options.reorderWindowSize, { it.timeStamp }) {
            if (options.printLateSamples) {
                println("discarded late sample: $it")
            }
            options.keepLateSamples
        }
        .let { seq ->
            if (options.printSamples) {
                seq.map { println(it); it }
            } else {
                seq
            }
        }
        .let(classifier)
        .forEach { _ ->
            if (options.printActivations) {
                println("ACTIVATION")
            }

            output.write("Activation classified\n")
            output.flush()
        }
}

