import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val arrowVersion = "0.13.1"

plugins {
    kotlin("jvm") version "1.4.32"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    application
}

group = "io.deckers.smtpjer"
version = "1.0-SNAPSHOT"

kotlin {
    experimental.coroutines = org.jetbrains.kotlin.gradle.dsl.Coroutines.ENABLE
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.6")
    implementation("com.tinder.statemachine:statemachine:0.2.0")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    implementation("io.arrow-kt:arrow-fx-coroutines:$arrowVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.0")



    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.2")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClassName = "io.deckers.smtpjer.MainKt"
}

tasks.withType<ShadowJar> {

    manifest.attributes.apply {
        //put("Implementation-Version" version)
        put("Main-Class", "io.deckers.smtpjer.MainKt")
    }


    baseName = "KtSmtp"
    classifier = "SNAPSHOT"
    version = "1.0"
}
