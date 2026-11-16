import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Desktop-side CLI: compiles word lists (docs/DICTIONARY.md) with the SAME reader/writer code the keyboard uses on the device.
// (The save-ladder shell scripts live in this directory too: tools/*.sh. Gradle only looks at build.gradle.kts and src/.)
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

application {
    mainClass.set("app.kestrel.tools.GlideLexiconCompilerKt")
}

dependencies {
    implementation(project(":core"))
    testImplementation(libs.junit)
}

// compile the glide-typing word list: ./gradlew :tools:glideLexicon --args="--out ... --words ... --pairs ... --extra ..."  (docs/DICTIONARY.md)
tasks.register<JavaExec>("glideLexicon") {
    group = "swoop"
    description = "Compile the glide-typing word list"
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.projectDir // paths in --args are relative to the repository root
    mainClass.set("app.kestrel.tools.GlideLexiconCompilerKt")
}
