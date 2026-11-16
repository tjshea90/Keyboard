import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The accuracy / replay lab. It runs on the build machine's JVM and is NEVER shipped. For now it drives the simulators that live with the
// core tests (GestureSim, GlideEval, GlideHabitEval); KB-400 adds the replay runner and the parameter sweeps here.
//   ./gradlew :eval:glideEval --args="noise=normal n=3000 pop=freq"        (arguments are documented in GlideEval's header)
//   ./gradlew :eval:glideHabitEval --args="..."
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

evaluationDependsOn(":core")

fun lab(taskName: String, main: String, what: String) = tasks.register<JavaExec>(taskName) {
    group = "swoop"
    description = what
    val coreTest = project(":core").extensions.getByType<SourceSetContainer>()["test"]
    dependsOn(project(":core").tasks.named("testClasses"))
    classpath = coreTest.runtimeClasspath
    workingDir = rootProject.projectDir // the word list path in GlideEval is relative to the repository root
    mainClass.set(main)
}
lab("glideEval", "app.kestrel.core.input.glide.GlideEval", "Glide accuracy on a simulated finger (seeded)")
lab("glideHabitEval", "app.kestrel.core.input.glide.GlideHabitEval", "Learning curve of a simulated person with a habitual sloppy shape")
