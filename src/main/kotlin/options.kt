package com.peanutcode.pison_challenge

import java.io.File
import kotlinx.cli.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.FileNotFoundException

data class Options(
    val host: String,
    val port: Int,
    val reorderWindowSize: Int,
    val classifierFactory: ClassifierFactory,
    val classifierOptions: String? = null,
    val describeClassifiers: Boolean,
    val keepLateSamples: Boolean,
    val printSamples: Boolean,
    val printLateSamples: Boolean,
    val printActivations: Boolean)

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

typealias ClassifierFactory = (Options) -> Classifier

val classifiers = hashMapOf<String, ClassifierFactory>(
    "cheat-trailing" to {_ -> ::cheatTrailingEdge },
    "cheat-leading"  to {_ -> ::cheatLeadingEdge },
    "alpha" to { o ->
        val opts = getClassifierOptions(AlphaOptions.serializer(), o)
        println(opts)
        alpha(opts.threshold, opts.windowSize, opts.step)
    },
    "beta" to { o ->
        val opts = getClassifierOptions(BetaOptions.serializer(), o)
        beta(opts.threshold, opts.windowSize, opts.step)
    },
    "gamma" to { o ->
        val opts = getClassifierOptions(GammaOptions.serializer(), o)
        gamma(
            opts.noiseFloor,
            opts.windowSize,
            opts.crossCountThreshold,
            opts.jerkSizeThreshold,
            opts.jerkCountThreshold)
    }
)

fun <T> getClassifierOptions(deserializer: DeserializationStrategy<T>, options: Options): T {
    val json = Json(JsonConfiguration.Stable)
    return json.parse(deserializer, options.classifierOptions ?: "{}")
}

fun parseOptions(@Suppress("UNUSED_PARAMETER") args: Array<String>): Options {
    val portFilePath = ".pison-challenge-port"

    val parser = ArgParser("response")
    val host by parser.option(
        ArgType.String, shortName = "H",
        description = "challenge server host")
        .default("localhost")
    val port by parser.option(
        ArgType.Int,
        shortName = "p",
        description = "challenge server port (can be auto-discovered in $portFilePath)")

    val reorderWindowSize by parser.option(
        ArgType.Int,
        fullName = "reorder-window",
        description = "size of window to use when reordering out-of-order samples")
        .default(40)

    val classifierName by parser.option(
        ArgType.Choice(classifiers.keys.toList().sorted()),
        fullName = "classifier",
        shortName = "c",
        description = "classifier algorithm to use")
        .default("gamma")
    val classifierOptions by parser.option(
        ArgType.String,
        fullName = "classifier-options",
        description = "JSON-encoded parameters to pass to the selected classifier (see --describe-classifiers)")
    val describeClassifiers by parser.option(
        ArgType.Boolean,
        fullName = "describe-classifiers",
        description = "show information about classifer parameters (to pass to --classifier-options)")
        .default(false)

    val keepLateSamples by parser.option(
        ArgType.Boolean,
        fullName = "keep-late-samples",
        description = "consider out-of-order samples that arrive outside the reordering window")
        .default(false)
    val printSamples by parser.option(
        ArgType.Boolean,
        fullName = "print-samples",
        description = "print all received samples (very noisy)")
        .default(false)
    val printLateSamples by parser.option(
        ArgType.Boolean,
        fullName = "print-late-samples",
        description = "print out-of-order samples that arrive outside the reordering window")
        .default(false)
    val hideActivations by parser.option(
        ArgType.Boolean,
        fullName = "hide-activations",
        description = "don't print a message when sending activations"
        ).default(false)

    parser.parse(args)

    if (describeClassifiers) {
        val json = Json(JsonConfiguration.Stable)
        print("""|alpha options:
                 |  ${json.stringify(AlphaOptions.serializer(), AlphaOptions())}
                 |
                 |beta options:
                 |  ${json.stringify(BetaOptions.serializer(), BetaOptions())}
                 |
                 |gamma options:
                 |  ${json.stringify(GammaOptions.serializer(), GammaOptions())}
                 |""".trimMargin())
        kotlin.system.exitProcess(0)
    }

    return Options(
        host = host,
        port = port ?: try {
            File(portFilePath).readText().trim().toInt()
        } catch (e: FileNotFoundException) {
            throw Exception("--port not specified, and no portfile found")
        },
        reorderWindowSize = reorderWindowSize,
        classifierFactory = classifiers[classifierName]
            ?: throw Exception("no such classifier: $classifierName"),
        classifierOptions = classifierOptions,
        describeClassifiers = describeClassifiers,
        keepLateSamples = keepLateSamples,
        printSamples = printSamples,
        printLateSamples = printLateSamples,
        printActivations = !hideActivations)
}

