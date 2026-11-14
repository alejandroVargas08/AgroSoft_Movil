import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.4.21"
}

val generarEntorno = tasks.register("generarEntorno") {
    val env = rootProject.file(".env")
    val salida = layout.buildDirectory.dir("generated/entorno/kotlin")
    inputs.files(env)
    outputs.dir(salida)
    doLast {
        val valores = Properties()
        if (env.exists()) env.inputStream().use { valores.load(it) }
        var apiUrl = valores.getProperty("API_URL")?.trim()?.trim('"') ?: "http://localhost:3000/api/"
        if (!apiUrl.endsWith("/")) apiUrl += "/"
        val destino = salida.get().file("co/edu/sena/agrosoft/Entorno.kt").asFile
        destino.parentFile.mkdirs()
        destino.writeText(
            """
            |package co.edu.sena.agrosoft
            |
            |object Entorno {
            |    const val API_URL: String = "$apiUrl"
            |}
            |""".trimMargin()
        )
    }
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    android {
        namespace = "co.edu.sena.agrosoft.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    sourceSets {
        commonMain {
            kotlin.srcDir(generarEntorno)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.androidx.activity.compose)
            implementation("io.ktor:ktor-client-okhttp:3.5.0")
            api("androidx.biometric:biometric:1.1.0")
            implementation("androidx.security:security-crypto:1.1.0-alpha06")
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:35.1.0"))
            implementation("com.google.firebase:firebase-messaging")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.5.0")
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("com.composables:icons-lucide-cmp:2.2.1")
            implementation("io.ktor:ktor-client-core:3.5.0")
            implementation("io.ktor:ktor-client-content-negotiation:3.5.0")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
            implementation("com.russhwolf:multiplatform-settings-no-arg:1.3.0")
            implementation("io.github.vinceglb:filekit-dialogs-compose:0.17.0")
            implementation("io.coil-kt.coil3:coil-compose:3.3.0")
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.3.0")
            implementation("dev.chrisbanes.haze:haze:2.1.0")
            implementation("dev.chrisbanes.haze:haze-glass:2.1.0")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.testRunner)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
