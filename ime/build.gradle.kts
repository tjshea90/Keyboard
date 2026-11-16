import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// The release signing key is PRIVATE (plan 11.5): CI writes keystore.properties (gitignored) from the decrypted keystore right before
// `assembleRelease` and deletes it afterwards. Without it (any local build) the release APK is simply UNSIGNED, never signed with a shared key.
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
        versionCode = 1
        versionName = "0.1.0-alpha1"
    }

    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("swoop") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
                enableV2Signing = true
                enableV3Signing = true   // allows a later key rotation without uninstalling
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"   // installs BESIDE the release build (own data), so a test build never touches what the keyboard learned
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfigs.findByName("swoop")?.let { signingConfig = it }
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
