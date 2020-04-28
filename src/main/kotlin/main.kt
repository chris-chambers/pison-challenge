package com.peanutcode.pison_challenge

import java.net.Socket
import java.util.Scanner
import kotlin.system.exitProcess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

@Serializable
data class Sample(val timeStamp: Long, val data: Int, val label: String)

fun main(args: Array<String>) {
    try {
        run(parseOptions(args))
    } catch (e: Throwable) {
        println("fatal: ${e.message}")
        exitProcess(2)
    }
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

