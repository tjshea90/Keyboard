# R39 - the Maven Central "429 Too Many Requests" that stops Gradle in a fresh cloud session (owner 2026-11-27)

Owner, his words: "the Gradle downloads hit the Maven rate limit (HTTP 429) ... This happens a lot and i thought i already fixed it with a setup script which gets a Google mirror. Figure out the best way to permanently fix this"

## What was found (this session, evidence)

* The saved environment ("Default", `env_01HRuRz2TRnj71ERt2YDuJDH`, the only one on the account) has a setup script that installs the Android SDK and then runs `tools/prewarm.sh` of every repo. It contains NO mirror: no `~/.gradle/init.d`, no `~/.m2/settings.xml`, no `GRADLE_*` / `MAVEN_*` variable. The environment named in R35 (`env_017Jf5U...`) no longer exists, so a mirror script written for it is gone (R35 said "nothing was changed yet").
* The only "Google mirror" in the project was Robolectric's OWN downloader (`robolectric.dependency.repo.url` in `ime/build.gradle.kts`): it fetches the Android framework jar at test time. Everything Gradle itself resolves (`kotlin-stdlib`, `kotlin-scripting-compiler-embeddable`, `nativeruntime-dist-compat-1.0.16.jar`, lint's `kotlin-stdlib-2.2.0.jar` ...) came from `https://repo.maven.apache.org/maven2/`, which throttles the shared addresses of the cloud sandboxes. Seen in this session: the setup's own warm-up failed all three parts (:core:test, :ime tests, lint) with 429, and 5 test runs + one 7-minute lint run were lost to it.
* `prewarm.sh` made it worse: it ran with Gradle's default parallel downloads and gave up on the first 429 (the saved script only prints "warm-up failed: sessions still start").

## The fix (three layers, all tested)

1. **`settings.gradle.kts`** (works in any container, no setup needed): `google()`, then Google's public copy of Maven Central, then `mavenCentral()` in BOTH repository lists (plugins and libraries). Central is only asked for what the copy lacks. Same coordinates, same checksums, same files. `CI=true` (GitHub's runners, which are not throttled) and `SWOOP_NO_MIRROR=1` keep the old behaviour, so the release build is unchanged.
   * Proof, a COLD run (`GRADLE_USER_HOME` = an empty directory, so every dependency is downloaded again): `:core:compileKotlin :ime:testDebugUnitTest --tests DiagnosticsTest :ime:lintDebug` = BUILD SUCCESSFUL in 1 min, 497 MB of dependencies, 384 downloads from the Google copy, 121 from dl.google.com (Android / AndroidX), **0 from repo.maven.apache.org, 0 HTTP errors**.
2. **`tools/gradle-central-mirror.init.gradle`** = the same idea for EVERY repo on the account (Browser, LinkMeter, ...): copied to `~/.gradle/init.d/` by the environment setup script, it puts the mirror in front of Central in the settings' repository lists. Swallows its own errors. Proof on two scratch projects with empty Gradle homes: libraries 58 / 58 downloads from the copy (0 from Central); a Kotlin-plugin project 58 from the copy, 0 from the plugin portal and Central (without the script: 50 from the portal, 8 from Central).
3. **`tools/prewarm.sh`** retries a part that stops on a 429 (up to 4 attempts, 40 s apart, fewer parallel downloads from the second attempt), so even the rare artifact the copy lacks does not end a warm-up.
4. A self-test check (`tools/test_handoff.sh`) fails if the mirror is no longer in front of `mavenCentral()` in both lists of `settings.gradle.kts`.

## What is NOT covered / honest limits

* A repo whose Gradle files declare repositories in `build.gradle` (project level, not in settings) is not changed by the init script; its own settings need the two lines.
* If the Google copy lags behind Central for a brand-new release, that one artifact comes from Central (a 404 on the copy falls through) and could still meet a 429; pinned versions are old enough.
* Not tested on a truly fresh container: this container already had the SDK and Gradle wrapper. The Gradle caches WERE cold.
