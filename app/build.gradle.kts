import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// نشانی مخزن گیت‌هاب: در GitHub Actions خودکار از GITHUB_REPOSITORY خوانده می‌شود
val githubRepo: String = (System.getenv("GITHUB_REPOSITORY") ?: localProps.getProperty("github.repo", "")).trim()
val catalogUrls: String = buildList {
    localProps.getProperty("catalog.url", "").trim().takeIf { it.startsWith("https://") }?.let { add(it) }
    System.getenv("NAVA_CATALOG_URL")?.trim()?.takeIf { it.startsWith("https://") }?.let { add(it) }
    if (githubRepo.contains('/')) {
        add("https://raw.githubusercontent.com/$githubRepo/main/catalog/catalog.json")
        add("https://cdn.jsdelivr.net/gh/$githubRepo@main/catalog/catalog.json")
    }
}.distinct().joinToString(",")
val repoUrl: String = if (githubRepo.contains('/')) "https://github.com/$githubRepo" else ""

fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val keystorePath: String? = System.getenv("NAVA_KEYSTORE_FILE")?.takeIf { file(it).exists() }

android {
    namespace = "ir.navaasemani.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.navaasemani.app"
        minSdk = 29
        targetSdk = 35
        versionCode = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0) + 100
        versionName = "1.0." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
        buildConfigField("String", "CATALOG_URLS", quoted(catalogUrls))
        buildConfigField("String", "REPO_URL", quoted(repoUrl))
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // کلید ثابت داخل مخزن: همه نسخه‌ها با یک امضا ساخته می‌شوند و روی هم نصب می‌شوند
        getByName("debug") {
            storeFile = file("nava-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("NAVA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("NAVA_KEY_ALIAS")
                keyPassword = System.getenv("NAVA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keystorePath != null) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    // پوشه catalog/ مخزن مستقیم داخل APK قرار می‌گیرد تا اپ بدون اینترنت هم کار کند
    sourceSets {
        getByName("main") {
            assets.srcDir(rootProject.file("catalog"))
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}


dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-session:1.6.1")
    implementation("androidx.media3:media3-common:1.6.1")

    implementation("io.coil-kt:coil-compose:2.7.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
