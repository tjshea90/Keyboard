pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Swoop"

// :core  pure Kotlin/JVM (no Android): decoders, language models, learning, the input state machine
// :ime   the Android app (package app.swoop.keyboard): the InputMethodService and everything drawn
// :tools Gradle JVM CLI (word-list compiler) - the shell tooling of the save ladder lives in tools/*.sh next to it
// :eval  the accuracy / replay lab (runs on the JVM, never shipped)
include(":core", ":ime", ":tools", ":eval")
