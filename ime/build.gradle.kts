import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Signing (owner decision 2026-11-17, same as the Kestrel browser repo): GitHub Actions signs every release with the PUBLIC dev key committed in signing/dev/, so any
// session or CI run produces an APK that updates the installed one. A private keystore.properties (gitignored) would win if the owner ever adds one; the certificate pinned in
// signing/CERT_SHA256 must then be re-pinned on purpose (an APK with another certificate cannot update the installed keyboard).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "app.swoop.keyboard"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.swoop.keyboard"
        minSdk = 29            // the tablet is Android 10 (API 29); anything newer must be guarded with Build.VERSION.SDK_INT
        targetSdk = 36         // also runs on the owner's Android 16 phone (edge-to-edge IME insets: KB-107)
        versionCode = 25
        versionName = "0.7.0-alpha14"
    }

    signingConfigs {
        create("swoop") {
            storeFile = rootProject.file(keystoreProps.getProperty("storeFile") ?: "signing/dev/swoop-dev.p12")
            storeType = keystoreProps.getProperty("storeType") ?: "pkcs12"
            storePassword = keystoreProps.getProperty("storePassword") ?: "swoop-dev"
            keyAlias = keystoreProps.getProperty("keyAlias") ?: "swoop-dev"
            keyPassword = keystoreProps.getProperty("keyPassword") ?: "swoop-dev"
            enableV2Signing = true
            enableV3Signing = true   // allows a later key rotation without uninstalling
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"   // installs BESIDE the release build (own data), so a test build never touches what the keyboard learned
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("swoop")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += setOf(
                "kotlin/**",
                "META-INF/*.kotlin_module",
                "META-INF/*.version",
                "META-INF/com.android.tools/**",
                "DebugProbesKt.bin",
            )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        lintConfig = file("lint.xml")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.addAll(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions",
        )
    }
}

dependencies {
    implementation(project(":core"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}

// Robolectric downloads its Android framework jar at test time; the sandbox's Maven Central route is the Google-hosted mirror.
tasks.withType<Test>().configureEach {
    systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2/")
    systemProperty("robolectric.dependency.repo.id", "central")
    jvmArgs("-Xmx2g")
}
