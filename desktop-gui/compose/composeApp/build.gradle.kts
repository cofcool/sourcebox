import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
//    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.serialization)
}

kotlin {
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.json)
            implementation(libs.coroitines)
            implementation(libs.kdatetime)
            implementation(libs.ktor.cn)
            implementation(libs.ktor.json)
            implementation(libs.ktor.auth)
            implementation(libs.ktor.core)
            implementation(libs.ktor.okhttp)
            implementation(libs.ktor.ciojvm)
            implementation(libs.slf4j)

            implementation(compose.components.resources)

            implementation(files("libs/sourcebox-fat.jar"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.junit5)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}


compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TheSourceBox"
            packageVersion = "1.0.0"
            description = "The Source Box Client"
            copyright = "2025 CofCool.net"
            windows.iconFile.set(file("src/jvmMain/composeResources/files/icons/icon.ico"))
            linux.iconFile.set(file("src/jvmMain/composeResources/files/icons/icon.png"))
            macOS.iconFile.set(file("src/jvmMain/composeResources/files/icons/icon.icns"))
            modules("java.sql", "java.naming", "java.management", "java.net.http")
        }
    }
}
