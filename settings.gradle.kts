// R39 (owner 2026-11-27: "the Gradle downloads hit the Maven rate limit (HTTP 429) ... figure out the best way to permanently fix this"): the cloud sandboxes share a few addresses, so repo.maven.apache.org answers
// "429 Too Many Requests" to whole downloads (kotlin-stdlib, kotlin-scripting-compiler, robolectric's 70 MB native jar ...) and a build stops. Google keeps a full copy of Maven Central in a public bucket that is not rate limited
// the same way; it stands BETWEEN google() and mavenCentral() here, so every artifact that exists on Central comes from it and Central is only asked for what the copy lacks (a 404). Nothing about WHAT is downloaded
// changes (same coordinates, same checksums). GitHub's own runners ("CI" is set there) keep asking Central: they are not throttled and the release build stays exactly what it was. SWOOP_NO_MIRROR=1 turns it off anywhere.
// (The block is written out twice on purpose: pluginManagement is read before the rest of this file, so it cannot share a variable with the block below.)
pluginManagement {
    repositories {
        google()
        if (System.getenv("CI") != "true" && System.getenv("SWOOP_NO_MIRROR") == null) maven { name = "centralMirror"; url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        if (System.getenv("CI") != "true" && System.getenv("SWOOP_NO_MIRROR") == null) maven { name = "centralMirror"; url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        mavenCentral()
    }
}

rootProject.name = "Swoop"

// :core  pure Kotlin/JVM (no Android): decoders, language models, learning, the input state machine
// :ime   the Android app (package app.swoop.keyboard): the InputMethodService and everything drawn
// :tools Gradle JVM CLI (word-list compiler) - the shell tooling of the save ladder lives in tools/*.sh next to it
// :eval  the accuracy / replay lab (runs on the JVM, never shipped)
include(":core", ":ime", ":tools", ":eval")
