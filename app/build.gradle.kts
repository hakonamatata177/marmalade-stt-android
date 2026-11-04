import java.util.Properties
import java.util.zip.ZipFile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

// Release signing: keystore.properties at the repo root (gitignored). When
// the file is absent — fresh clones, F-Droid's buildserver — release builds
// are simply unsigned, which is also correct for F-Droid (they sign their
// own). Same mechanism as marmalade-tts-android.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.isFile) f.inputStream().use { load(it) }
}

// Reproducible builds: pin the entire resolved dependency graph in
// app/gradle.lockfile so a rebuild weeks later (F-Droid's verification
// server) cannot pick up a newer transitive than the CI build did.
// After any dependency change, regenerate with
//   ./gradlew :app:dependencies --write-locks
dependencyLocking {
    lockAllConfigurations()
}

// ONNX Runtime, from Maven only (D3). One version feeds both the runtime
// dependency (Java API + libonnxruntime.so packaged in the APK) and the
// headers/.so that libsherpa-onnx-jni.so is compiled and linked against, so
// the two can never drift apart. sherpa-onnx v1.13.8 is tested upstream
// against ORT 1.28.x.
val onnxRuntimeVersion = "1.28.0"

// ABI → the versionCode's last digit (0 is the base / AAB code). The keys are
// every ABI the build knows; which ones a variant ships is decided below.
val abiVersionDigits = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86_64" to 4)

// Real-device ABIs, and the emulator ABI that debug APKs add.
val releaseAbis = setOf("arm64-v8a", "armeabi-v7a")
val emulatorAbi = "x86_64"

// Optional per-ABI knob for test builds (#31): -PabiFilters=arm64-v8a limits
// both the native CMake build and the APK splits to the listed ABIs, so a
// phone build compiles sherpa-onnx once instead of three times. Without the
// property every variant builds its usual ABIs (release/F-Droid unchanged).
// Never use it for published release APKs: a one-ABI build's arm64 APK is
// not byte-identical to the two-ABI build's (AGP's per-split manifest keeps
// an extra blank line, so aapt2's line numbers differ; #56), and F-Droid's
// reproducible-build check compares bytes.
val abiOverride: Set<String>? = (findProperty("abiFilters") as String?)
    ?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()
    ?.also { requested ->
        val unknown = requested - abiVersionDigits.keys
        require(unknown.isEmpty()) { "-PabiFilters: unknown ABI(s) $unknown; known: ${abiVersionDigits.keys}" }
    }

/** [abis] narrowed to [abiOverride] when it is set. */
fun withAbiOverride(abis: Set<String>): Set<String> = abiOverride?.let { abis intersect it } ?: abis

// App bundles ignore ABI splits and ignore packaging.jniLibs.excludes, so
// ndk.abiFilters is the only way to keep ONNX Runtime's x86/x86_64 libraries
// (no sherpa-onnx build next to them) out of an AAB. AGP refuses
// ndk.abiFilters while ABI splits are on, so a Gradle run that builds a
// bundle turns the splits off and filters instead. Build APKs and bundles
// in separate Gradle runs.
val buildingBundle = gradle.startParameter.taskNames.any {
    it.substringAfterLast(':').startsWith("bundle")
}

android {
    namespace = "app.marmalade.stt"
    compileSdk = 36

    // No Google-encrypted dependency blob in the APK signing block:
    // F-Droid's scanner rejects it as an opaque extra signing block,
    // and only Google can decrypt it anyway. The AAB keeps it for Play
    // Console's dependency insights.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }

    defaultConfig {
        applicationId = "app.marmalade.stt"
        minSdk = 28
        targetSdk = 36
        // Versioning (same scheme as marmalade-tts-android):
        // versionCode = MAJOR*10_000_000 + MINOR*10_000 + PATCH*10 + ABI.
        // This literal is the base (ABI digit 0). Each per-ABI APK adds its
        // digit (1=armv7, 2=arm64, 4=x86_64; see abiVersionDigits below), so
        // arm64 outranks armv7 on a device that runs both. The Play AAB
        // keeps the base code. Safe headroom: 32-bit int caps at ~214.7.0.0.
        // Written as a plain literal (not the formula as arithmetic):
        // F-Droid's checkupdates parses this line with a regex and only
        // sees the first number of an expression.
        versionCode = 10020030
        versionName = "1.2.3"

        vectorDrawables {
            useSupportLibrary = true
        }

        if (buildingBundle) ndk { abiFilters += withAbiOverride(releaseAbis) }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // sherpa-onnx built from the pinned submodules under third_party/
        // with TTS off, so no espeak-ng/piper (GPL) code can reach the APK.
        // See src/main/cpp/CMakeLists.txt and the native-license guard below.
        externalNativeBuild {
            cmake {
                // Reproducible builds (same flags as marmalade-tts-android):
                //   -ffile-prefix-map strips the absolute checkout path out of
                //     __FILE__ strings, so two checkout paths give the same bytes.
                //   --build-id=none drops the GNU build-id note, which the NDK
                //     derives from something path-sensitive.
                val prefixMapRoot = rootProject.layout.projectDirectory.asFile.absolutePath
                cFlags += listOf("-ffile-prefix-map=$prefixMapRoot=.")
                cppFlags += listOf("-ffile-prefix-map=$prefixMapRoot=.")
                arguments += listOf(
                    "-DTHIRD_PARTY_DIR=${rootProject.file("third_party").absolutePath}",
                    "-DORT_DIR=${layout.buildDirectory.dir("onnxruntime").get().asFile.absolutePath}",
                    // Same C++ runtime model as the ORT AAR (no libc++_shared.so).
                    "-DANDROID_STL=c++_static",
                    // Optimised native code in debug builds too: at -O0 the
                    // feature extraction and decoding glue is too slow to judge
                    // dictation latency on a debug install.
                    "-DCMAKE_BUILD_TYPE=Release",
                    // 16 KB page-size alignment (Play requirement for SDK 35+;
                    // NDK r26 doesn't default to it). Verify with readelf -lW.
                    "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384 -Wl,--build-id=none",
                )
                // Optional cap on parallel C++ compiles per ABI, e.g.
                // -PnativeJobs=4 on a low-memory machine (Eigen/OpenFst units
                // take several hundred MB each). Default: ninja's own choice.
                (project.findProperty("nativeJobs") as String?)?.let { jobs ->
                    arguments += listOf("-DCMAKE_JOB_POOLS=compile=$jobs", "-DCMAKE_JOB_POOL_COMPILE=compile")
                }
                targets += listOf("sherpa-onnx-jni", "marmalade-cpu")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    ndkVersion = "26.3.11579264"

    // Two distribution flavors sharing one applicationId, signing config and
    // feature set (D9). Kept for parity with marmalade-tts-android so store-
    // specific differences have a home if one ever appears; today the two
    // flavors build identical apps.
    flavorDimensions += "distribution"
    productFlavors {
        create("fdroid") { dimension = "distribution" }
        create("play") { dimension = "distribution" }
        // This fork's build (hakonamatata177/marmalade-stt-android, branch norsk): installs next to
        // the F-Droid app, and lists the Norwegian engines from ExtraEngines' manifest. Signed on
        // Rafiki (tools/norsk/signer.sh), not by Gradle.
        create("norsk") {
            dimension = "distribution"
            applicationIdSuffix = ".norsk"
            versionNameSuffix = "-norsk"
            resValue("string", "app_name", "Marmalade Norsk")
            resValue("string", "ime_name", "Marmalade tale (norsk)")
            resValue("string", "recognizer_label", "Marmalade Norsk")
        }
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
        // Dedicated distribution key for F-Droid/GitHub releases: the
        // permanent F-Droid signing identity must not share fate with the
        // Play upload key, which Google can reset. Optional dist* quartet in
        // the same keystore.properties; absent → the fdroid flavor falls
        // back to "release" (or unsigned), which is fine everywhere except a
        // real distribution build.
        if (keystoreProps.getProperty("distStoreFile") != null) {
            create("dist") {
                storeFile = file(keystoreProps.getProperty("distStoreFile"))
                storePassword = keystoreProps.getProperty("distStorePassword")
                keyAlias = keystoreProps.getProperty("distKeyAlias")
                keyPassword = keystoreProps.getProperty("distKeyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            // A second debug install next to another session's .debug on a
            // shared test phone (#91): `-PdebugIdSuffix=.speedaudit` gives it
            // its own package and its own name in the keyboard list.
            (project.findProperty("debugIdSuffix") as String?)?.let { suffix ->
                applicationIdSuffix = suffix
                resValue("string", "app_name", "STT $suffix")
                resValue("string", "ime_name", "STT $suffix")
            }
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Reproducible builds: AGP embeds META-INF/version-control-info
            // whose content depends on the checkout's shape (git worktrees
            // yield NO_VALID_GIT_FOUND, real clones the revision) — an entry
            // that differs between an fdroid-build and a host build of the
            // same commit. Omit it entirely.
            vcsInfo { include = false }
            signingConfig = signingConfigs.findByName("release")
            // On-device R8 smoke testing: `-PsmokeRelease` builds this same
            // minified release variant but installable side-by-side as
            // app.marmalade.stt.rc, signed with the debug key so adb can
            // install it without keystore.properties. Never distribute a
            // smokeRelease artifact.
            if (project.hasProperty("smokeRelease")) {
                applicationIdSuffix = ".rc"
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        // Shipped UI languages (#51, #69; keep in sync with res/xml/locales_config.xml
        // and LOCALES in TranslationsTest). Also strips unused locales from
        // library resources.
        localeFilters += listOf(
            "en", "de", "es", "fr", "hi", "it", "ja", "pt-rBR", "zh-rCN",
            "ar", "cs", "in", "ko", "nl", "pl", "ru", "sv", "tr", "uk", "vi", "zh-rTW",
        )
    }
    // One APK per ABI (#9). ONNX Runtime alone is 28.6 MB (arm64) + 20.4 MB
    // (armv7), so a universal APK made every user download both. Real
    // devices are arm64-v8a (modern) or armeabi-v7a (older 32-bit ARM);
    // x86_64 is for the emulator and only debug builds keep it (see
    // androidComponents below). For bundles see buildingBundle: Play makes
    // per-ABI APKs from the AAB itself.
    splits {
        abi {
            isEnable = !buildingBundle
            reset()
            include(*withAbiOverride(abiVersionDigits.keys).toTypedArray())
            isUniversalApk = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
        // libonnxruntime.so reaches the build twice: from the ORT AAR (the
        // copy that ships) and as an IMPORTED library of the sherpa-onnx CMake
        // project. Both are the same file from the same Maven artifact.
        jniLibs {
            pickFirsts += "lib/*/libonnxruntime.so"
            // libonnxruntime4j_jni.so (ONNX Runtime's Java binding, 112 KB
            // arm64) ships since #32: the Granite engine runs on the
            // ai.onnxruntime Java API (GraniteTranscriber.kt).
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        // The Compose accessibility tests (src/testDebug, #68) render real
        // composables under Robolectric, which needs the merged resources.
        unitTests.isIncludeAndroidResources = true
    }
}

// Per variant: which ABIs to compile and ship, and each per-ABI APK's
// versionCode (the base plus its digit, see defaultConfig).
androidComponents {
    onVariants { variant ->
        val shipped = if (variant.buildType == "debug") releaseAbis + emulatorAbi else releaseAbis
        val abis = withAbiOverride(shipped)
        // With no ABI left, AGP would ship every ABI instead, so packaging such
        // a variant (release with -PabiFilters=x86_64) fails. Not at configure
        // time, so the run's other variants still work, and not at preBuild:
        // the CMake tasks are shared between variants and depend on one
        // variant's preBuild (playRelease's, for a debug build too).
        // merge<Variant>NativeLibs is the variant's own and precedes its APK
        // and AAB; unit tests don't need it.
        if (abis.isEmpty()) {
            val message = "-PabiFilters=${abiOverride!!.joinToString(",")} leaves ${variant.name} no ABI " +
                "(it ships ${shipped.joinToString(", ")}): build another variant or add one of its ABIs"
            val mergeNativeLibs = "merge${variant.name.replaceFirstChar { it.uppercase() }}NativeLibs"
            project.tasks.matching { it.name == mergeNativeLibs }.configureEach { doFirst { throw GradleException(message) } }
        }
        // Compile sherpa-onnx only for the ABIs this variant ships.
        variant.externalNativeBuild?.abiFilters?.set(abis)
        // F-Droid and GitHub serve the APK file as is, and every update is a
        // full download, so the fdroid flavor compresses its native libraries:
        // the arm64 release APK drops from 35.8 to 15.5 MB. The cost is about
        // 12 MB more on the device, because Android unpacks the libraries at
        // install time; that is small next to the 299 MB speech model. Play
        // (the AAB) keeps them uncompressed: Play compresses downloads itself,
        // and uncompressed libraries install smaller. Numbers in #9.
        if (variant.flavorName == "fdroid") variant.packaging.jniLibs.useLegacyPackaging.set(true)
        for (output in variant.outputs) {
            val abi = output.filters
                .find { it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI }
                ?.identifier ?: continue
            if (abi !in abis) {
                output.enable.set(false)
            } else {
                output.versionCode.set(output.versionCode.get() + abiVersionDigits.getValue(abi))
            }
        }
    }
}

configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    exclude(group = "com.microsoft.onnxruntime", module = "onnxruntime-android")
}

// Force transitive dependencies to versions available in the offline Gradle
// cache (the same set marmalade-tts-android pins).
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
        force("androidx.lifecycle:lifecycle-livedata:2.8.7")
        force("androidx.lifecycle:lifecycle-livedata-core:2.8.7")
        force("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
        force("androidx.fragment:fragment:1.5.4")
        force("androidx.appcompat:appcompat:1.7.0")
        force("androidx.core:core:1.15.0")
        force("androidx.core:core-ktx:1.15.0")
        force("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
        force("com.google.guava:guava:33.0.0-jre")
        force("com.google.devtools.ksp:symbol-processing-api:2.1.0-1.0.29")
    }
}

// The fdroid release variant signs with the dedicated distribution key
// when one is configured (see the signingConfigs comment). Variant API
// because signingConfig set on a buildType can't differ per flavor.
// smokeRelease keeps its debug-key override — never ship that variant.
androidComponents {
    onVariants(selector().withFlavor("distribution" to "fdroid").withBuildType("release")) { variant ->
        // Single line on purpose: F-Droid's remove_signing_keys() strips
        // gradle lines matching `android.signingConfigs.` with no `{` on
        // the line. A multi-line form leaves a dangling reference behind
        // and the stripped script no longer compiles on their builder.
        if (!project.hasProperty("smokeRelease")) {
            android.signingConfigs.findByName("dist")?.let { variant.signingConfig.setConfig(it) }
        }
    }
}

// Reproducible builds (#86): AGP's compiled ART baseline profile
// (assets/dexopt/baseline.prof and .profm) is not deterministic; it changes
// with the build machine's core count. F-Droid's armv7 build of 1.0.0 failed
// its byte comparison on exactly those two entries. Disabling the profile
// tasks drops both files from the fdroid release APKs, as F-Droid's
// Reproducible Builds docs recommend. The cost: ART compiles the app's hot
// code from on-device profiling instead, so the first launches after an
// install or update are a little slower. Play keeps its profile (the AAB
// isn't byte-compared, and Play uses the profile for cloud profiles).
tasks.matching { it.name.contains("FdroidRelease") && it.name.contains("ArtProfile") }
    .configureEach { enabled = false }

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.54")
    ksp("com.google.dagger:hilt-android-compiler:2.54")

    // DataStore (Preferences)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // JSON for the replacement dictionary and snippets in DataStore (the
    // JsonElement API only, so no serialization compiler plugin). 1.6.3 matches
    // the serialization-core that Navigation already brings in.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Rule-based transcript cleanup (D7), applied to every final utterance.
    implementation(project(":textproc"))
    implementation(project(":marmalade-ui"))

    // Microsoft ONNX Runtime (MIT): the inference runtime under sherpa-onnx,
    // and the Granite engine's through its Java API (#32).
    implementation("com.microsoft.onnxruntime:onnxruntime-android:$onnxRuntimeVersion")

    // Testing — JVM
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    // The same ONNX Runtime Java API with desktop natives, for GraniteParityTest
    // on the host. The Android AAR is kept off the unit-test runtime classpath
    // (below) so its identical classes can't shadow this jar's native loader.
    testImplementation("com.microsoft.onnxruntime:onnxruntime:$onnxRuntimeVersion")

    // Testing — Compose semantics under Robolectric (#68): the accessibility
    // checks in src/testDebug. Debug only, because createComposeRule needs
    // ui-test-manifest's activity in the merged manifest.
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing — instrumented (versions available in the offline Gradle cache)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.0")
}

// -----------------------------------------------------------------------------
// ONNX Runtime headers + libs for the native build
// -----------------------------------------------------------------------------
// The Maven AAR carries headers/ and jni/<abi>/libonnxruntime.so. Unpack them
// to build/onnxruntime/ (the ORT_DIR CMake argument) before any CMake task, so
// the native build never downloads ONNX Runtime itself (D3).
run {
    val onnxRuntimeAar = configurations.create("onnxRuntimeAar") {
        isCanBeConsumed = false
        isTransitive = false
    }
    dependencies.add(onnxRuntimeAar.name, "com.microsoft.onnxruntime:onnxruntime-android:$onnxRuntimeVersion@aar")
    val extractOnnxRuntime = tasks.register<Sync>("extractOnnxRuntime") {
        group = "build"
        description = "Unpack ONNX Runtime headers and libonnxruntime.so from the Maven AAR for the CMake build."
        from({ zipTree(onnxRuntimeAar.singleFile) }) {
            include("headers/**", "jni/*/libonnxruntime.so")
        }
        into(layout.buildDirectory.dir("onnxruntime"))
    }
    tasks.matching { it.name.startsWith("configureCMake") || it.name.startsWith("buildCMake") }
        .configureEach { dependsOn(extractOnnxRuntime) }
}

// -----------------------------------------------------------------------------
// Native-license guard: no espeak-ng / piper code in any packaged .so
// -----------------------------------------------------------------------------
// The prebuilt sherpa-onnx AAR statically links espeak-ng (GPL-3.0); D3 forbids
// it. This scans every lib/**.so inside each built APK and fails the build on
// an "espeak" or "piper" string. "espeak" must not be preceded by a letter:
// sherpa-onnx's speaker-embedding code legitimately says "wespeaker".
// Runs after every assemble<Variant> and as part of `check` (debug variants).
abstract class NativeLicenseGuard : DefaultTask() {
    @get:InputFiles
    abstract val apkDir: DirectoryProperty

    @TaskAction
    fun scan() {
        val forbidden = Regex("(?i)(?<![a-z])espeak|piper")
        val apks = apkDir.get().asFile.walk().filter { it.extension == "apk" }.toList()
        if (apks.isEmpty()) throw GradleException("No APK found in ${apkDir.get().asFile}")
        val hits = mutableListOf<String>()
        var scanned = 0
        for (apk in apks) {
            ZipFile(apk).use { zip ->
                for (entry in zip.entries().asSequence().filter { it.name.endsWith(".so") }) {
                    scanned++
                    // ISO-8859-1 maps every byte to one char, so the regex sees raw bytes.
                    val text = zip.getInputStream(entry).use { String(it.readBytes(), Charsets.ISO_8859_1) }
                    forbidden.findAll(text).take(3).forEach { m ->
                        val context = text.substring(maxOf(0, m.range.first - 20), minOf(text.length, m.range.last + 20))
                            .replace(Regex("[^\\x20-\\x7e]"), ".")
                        hits += "${apk.name}!${entry.name}: \"$context\""
                    }
                }
            }
        }
        if (hits.isNotEmpty()) {
            throw GradleException(
                "espeak/piper strings found in packaged native libraries (GPL; see docs/ARCHITECTURE.md D3):\n" +
                    hits.joinToString("\n"),
            )
        }
        logger.lifecycle("Native-license guard: $scanned .so file(s) in ${apks.size} APK(s), no espeak/piper strings.")
    }
}

androidComponents {
    onVariants { variant ->
        val cap = variant.name.replaceFirstChar { it.uppercase() }
        val guard = tasks.register<NativeLicenseGuard>("check${cap}NativeLicenses") {
            group = "verification"
            description = "Fail if any .so in the ${variant.name} APK contains espeak/piper strings."
            apkDir.set(variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.APK))
        }
        tasks.matching { it.name == "assemble$cap" }.configureEach { finalizedBy(guard) }
        if (variant.buildType == "debug") {
            tasks.matching { it.name == "check" }.configureEach { dependsOn(guard) }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnit()
    // TranslationsTest reads src/main/res through java.io.File, which Gradle
    // doesn't track: without this, a strings-only change leaves the test task
    // UP-TO-DATE and the translations gate silently skipped.
    inputs.dir("src/main/res").withPathSensitivity(PathSensitivity.RELATIVE)
}

// -----------------------------------------------------------------------------
// In-app privacy policy
// -----------------------------------------------------------------------------
// The canonical policy is the repo-root PRIVACY.md. It's copied into the APK
// assets at build (build/generated/privacyAssets/PRIVACY.md, gitignored under
// build/) and rendered by PrivacyPolicyScreen — one source of truth, no
// committed duplicate to drift.
run {
    val privacyAssetsDir = layout.buildDirectory.dir("generated/privacyAssets")
    val copyPrivacyPolicy = tasks.register<Copy>("copyPrivacyPolicy") {
        group = "build"
        description = "Bundle the canonical repo-root PRIVACY.md into app assets for the in-app privacy screen."
        from(rootProject.file("PRIVACY.md"))
        into(privacyAssetsDir)
    }
    android.sourceSets.getByName("main").assets.srcDir(privacyAssetsDir)
    tasks.withType<com.android.build.gradle.tasks.MergeSourceSetFolders>().configureEach {
        dependsOn(copyPrivacyPolicy)
    }
    // Lint reads the asset source roots too, so its model/analysis tasks
    // consume this task's output. Without the edge, `assembleFdroidRelease`
    // fails Gradle's implicit-dependency validation before it ever reaches
    // packaging. Matched by name rather than by type: the task class
    // (com.android.build.gradle.internal.lint.LintModelWriterTask) is
    // AGP-internal.
    tasks.matching { it.name.contains("lint", ignoreCase = true) }.configureEach {
        dependsOn(copyPrivacyPolicy)
    }
}
