group = "com.example.universal_dockit"
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
    namespace = "com.example.universal_dockit"
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
            // Apache POI + PdfiumAndroid duplicate entries
            excludes += setOf(
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/DEPENDENCIES",
                "META-INF/*.kotlin_module",
                "META-INF/versions/**",
            )
            pickFirsts += setOf(
                "META-INF/services/org.apache.poi.ss.usermodel.WorkbookProvider",
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

    // -------------------------------------------------------------------------
    // PdfiumAndroid — open-source PDF rendering (via barteksc android-pdf-viewer)
    // https://github.com/barteksc/AndroidPdfViewer  (Apache 2.0)
    // PDFView widget backed by PdfiumAndroid native renderer
    // -------------------------------------------------------------------------
    // barteksc artifact is unavailable on JitPack; mhiew fork is published on Maven Central
    implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")

    // -------------------------------------------------------------------------
    // Apache POI 5.3.0 — open-source Office document parsing
    // https://poi.apache.org/  (Apache 2.0)
    //  poi       : DOC, XLS, PPT  (legacy binary BIFF/OLE2 formats)
    //  poi-ooxml : DOCX, XLSX, PPTX (OOXML/ZIP-based formats)
    // -------------------------------------------------------------------------
    implementation("org.apache.poi:poi-ooxml:5.3.0") {
        exclude(group = "xml-apis", module = "xml-apis")
        exclude(group = "stax", module = "stax-api")
    }
    implementation("org.apache.poi:poi:5.3.0")
    implementation("org.apache.poi:poi-scratchpad:5.3.0")
    implementation("org.apache.xmlbeans:xmlbeans:5.3.0")
    implementation("com.zaxxer:SparseBitSet:1.3")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("commons-codec:commons-codec:1.18.0")
    implementation("commons-io:commons-io:2.19.0")

    // -------------------------------------------------------------------------
    // OpenDocument (.odt/.ods/.odp) is parsed in-house via ZIP + XmlPullParser
    // (see OdfContentParser). We deliberately avoid:
    //   • org.odftoolkit:odfdom-java  — depends on org.w3c.dom.events.* /
    //                                   .traversal.* not present on Android
    //   • Apache POI slide rendering  — relies on java.awt.Graphics2D
    //   • io.github.nullpops:android-awt — only provides empty AWT stubs
    // PPT/PPTX text extraction uses poi-scratchpad / poi-ooxml directly.
    // -------------------------------------------------------------------------

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.mockito:mockito-core:5.0.0")
}
