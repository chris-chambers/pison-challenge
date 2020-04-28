import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    val kotlinVersion = "1.3.72"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "com.peanutcode"
version = "1.0-SNAPSHOT"

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.peanutcode.pison_challenge.MainKt"
    }
}

repositories {
    mavenCentral()
    maven("https://kotlin.bintray.com/kotlinx")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.2.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.allWarningsAsErrors = true
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("response")
    archiveClassifier.set("")
    archiveVersion.set("")
}
