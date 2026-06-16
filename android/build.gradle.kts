group = "com.prathap021.universal_dockit"
version = "1.0-SNAPSHOT"

buildscript {
    val kotlinVersion = "2.3.20"
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.0.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android") version "2.3.20"
}

android {
    namespace = "com.prathap021.universal_dockit"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
        getByName("test") {
            java.srcDirs("src/test/kotlin")
        }
    }

    defaultConfig {
        minSdk = 26
        multiDexEnabled = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/NOTICE",
                "META-INF/NOTICE.md",
                "META-INF/LICENSE",
                "META-INF/LICENSE.md",
                "META-INF/DEPENDENCIES",
                "META-INF/*.kotlin_module",
                "META-INF/versions/**",
                "META-INF/INDEX.LIST",
            )
            pickFirsts += setOf(
                "META-INF/services/javax.xml.stream.XMLInputFactory",
                "META-INF/services/javax.xml.stream.XMLOutputFactory",
            )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
                it.outputs.upToDateWhen { false }
                it.testLogging {
                    events("passed", "skipped", "failed", "standardOut", "standardError")
                    showStandardStreams = true
                }
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Coroutines for async document loading
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Lifecycle aware ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.activity:activity-ktx:1.9.3")

    // -------------------------------------------------------------------------
    // PdfiumAndroid — open-source PDF rendering (via barteksc android-pdf-viewer)
    // https://github.com/barteksc/AndroidPdfViewer  (Apache 2.0)
    // PDFView widget backed by PdfiumAndroid native renderer
    // -------------------------------------------------------------------------
    // barteksc artifact is unavailable on JitPack; mhiew fork is published on Maven Central
    implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")

    // Microsoft Office parsing. poi-ooxml covers DOCX/XLSX/PPTX; scratchpad adds
    // binary DOC/PPT support via HWPF/HSLF.
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("org.apache.poi:poi-scratchpad:5.2.5")

    // -------------------------------------------------------------------------
    // OpenDocument (.odt/.ods/.odp) is parsed in-house via ZIP + XmlPullParser
    // (see OdfContentParser).
    // -------------------------------------------------------------------------

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.mockito:mockito-core:5.0.0")
}
