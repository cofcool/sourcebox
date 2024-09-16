import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("src/main/kotlin")
        }
    }
}

group = "net.cofcool.sourcebox.compose"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)

    implementation("io.ktor:ktor-client-core:${project.extra["ktor.version"]}")
    implementation("io.ktor:ktor-client-okhttp:${project.extra["ktor.version"]}")
    implementation("io.ktor:ktor-client-content-negotiation:${project.extra["ktor.version"]}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${project.extra["ktor.version"]}")
    implementation("io.ktor:ktor-client-auth:${project.extra["ktor.version"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${project.extra["coroutines.version"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${project.extra["json.version"]}")
    implementation("org.slf4j:slf4j-jdk14:${project.extra["slfj.version"]}")
    implementation("io.ktor:ktor-client-cio-jvm:2.3.11")

    testImplementation("org.junit.jupiter:junit-jupiter:${project.extra["junit5.version"]}")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SourceBox"
            packageVersion = "1.0.0"
        }
    }
}
