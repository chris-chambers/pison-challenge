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
    val classifier: Classifier,
    val keepLateSamples: Boolean,
    val printSamples: Boolean,
    val printLateSamples: Boolean,
    val printActivations: Boolean)

@Serializable
data class Sample(val timeStamp: Long, val data: Int, val label: String)

val classifiers = hashMapOf<String, Classifier>(
    "cheat-trailing-edge" to ::cheatTrailingEdge,
    "cheat-leading-edge"  to ::cheatLeadingEdge
);

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

    val classifierName = "cheat-trailing-edge"
    val classifier = classifiers[classifierName]
        ?: throw Exception("no such classifier: $classifierName")

    return Options(
        host = "localhost",
        port = port,
        reorderWindowSize = 30,
        classifier = classifier,
        keepLateSamples = false,
        printSamples = true,
        printLateSamples = true,
        printActivations = true)
}

fun run(options: Options) {
    val socket = Socket(options.host, options.port)
    val scanner = Scanner(socket.inputStream)
    val output = socket.outputStream.writer()
    val json = Json(JsonConfiguration.Stable)
    val serializer = Sample.serializer()
    scanner.asSequence()
        .map { json.parse(serializer, it) }
        .let { seq ->
            if (options.reorderWindowSize > 0) {
                seq.sortedByInWindow(options.reorderWindowSize, { it.timeStamp }) {
                    if (options.printLateSamples) {
                        println("discarded late sample: $it")
                    }
                    options.keepLateSamples
                }
            } else {
                seq
            }
        }
        .let { seq ->
            if (options.printSamples) {
                seq.map { println(it); it }
            } else {
                seq
            }
        }
        .let(options.classifier)
        .forEach { _ ->
            if (options.printActivations) {
                println("ACTIVATION")
            }

            output.write("Activation classified\n")
            output.flush()
        }
}

