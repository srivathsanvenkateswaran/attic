plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/** Play's upload key, from Gradle properties or the environment. Null until all four are set. */
class UploadKey(val storeFile: File, val storePassword: String, val keyAlias: String, val keyPassword: String)

val uploadKey: UploadKey? = run {
    fun value(name: String): String? =
        (providers.gradleProperty(name).orNull ?: providers.environmentVariable(name).orNull)?.takeIf { it.isNotBlank() }
    val store = value("ATTIC_UPLOAD_STORE_FILE")?.let { file(it.replaceFirst(Regex("^~(?=/)"), System.getProperty("user.home"))) }
    val storePassword = value("ATTIC_UPLOAD_STORE_PASSWORD")
    val alias = value("ATTIC_UPLOAD_KEY_ALIAS")
    val keyPassword = value("ATTIC_UPLOAD_KEY_PASSWORD")
    if (store != null && storePassword != null && alias != null && keyPassword != null) {
        check(store.isFile) { "ATTIC_UPLOAD_STORE_FILE points at $store, which doesn't exist." }
        UploadKey(store, storePassword, alias, keyPassword)
    } else {
        null
    }
}

// Store screenshots render the real screens on the JVM with Robolectric and Roborazzi, from demo data
// in src/storeShots. They are off unless -PstoreShots is given, so the everyday test run neither
// compiles them nor pulls their dependencies. scripts/store_shots/run.sh runs them and frames the output.
val storeShots = providers.gradleProperty("storeShots").isPresent

android {
    namespace = "app.attic.player"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "app.attic.player"
        // 33 is the first release with READ_MEDIA_AUDIO, so there is no legacy storage path to carry.
        minSdk = 33
        targetSdk = 36
        versionCode = providers.gradleProperty("attic.versionCode").get().toInt()
        versionName = providers.gradleProperty("attic.versionName").get()
        // The song analysis model ships as native code for phones only, which keeps the APK from tripling.
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        if (uploadKey != null) {
            create("release") {
                storeFile = uploadKey.storeFile
                storePassword = uploadKey.storePassword
                keyAlias = uploadKey.keyAlias
                keyPassword = uploadKey.keyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (uploadKey != null) {
                signingConfigs.getByName("release")
            } else {
                if (gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }) {
                    logger.warn(
                        """
                        |
                        |WARNING: no Attic upload key, so this release build is signed with the DEBUG key.
                        |It is fine for a local install. Play rejects it. Set ATTIC_UPLOAD_STORE_FILE,
                        |ATTIC_UPLOAD_STORE_PASSWORD, ATTIC_UPLOAD_KEY_ALIAS and ATTIC_UPLOAD_KEY_PASSWORD
                        |in ~/.gradle/gradle.properties or the environment (scripts/make-upload-key.sh).
                        |
                        """.trimMargin(),
                    )
                }
                signingConfigs.getByName("debug")
            }
            // Compose only runs smoothly with R8 on.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        // Robolectric needs the merged resources (fonts, strings) to draw the screens.
        unitTests.isIncludeAndroidResources = storeShots
    }

    if (storeShots) {
        sourceSets.getByName("test").kotlin.directories.add("src/storeShots/kotlin")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

/**
 * Puts the licence texts in the app's assets under licences/, for Settings, About, Open-source licences.
 * The OFL, Apache, MIT and GPL all require the texts to travel with the binary. The files live in
 * third_party/ and the repository's LICENSE, which ships as LICENSE-attic.txt.
 */
abstract class LicenceAssets : DefaultTask() {
    @get:InputFile
    abstract val appLicence: RegularFileProperty

    @get:InputDirectory
    abstract val thirdParty: DirectoryProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun copy() {
        val dir = output.get().asFile.resolve("licences")
        dir.deleteRecursively()
        dir.mkdirs()
        appLicence.get().asFile.copyTo(dir.resolve("LICENSE-attic.txt"))
        thirdParty.get().asFile.listFiles { file -> file.isFile }!!.forEach { it.copyTo(dir.resolve(it.name)) }
    }
}

val licenceAssets = tasks.register<LicenceAssets>("licenceAssets") {
    appLicence = rootProject.layout.projectDirectory.file("LICENSE")
    thirdParty = rootProject.layout.projectDirectory.dir("third_party")
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(licenceAssets, LicenceAssets::output)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.compose)
    implementation(libs.core.ktx)
    implementation(libs.core.splash)
    implementation(libs.palette)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.coil.compose)
    implementation(libs.coroutines.guava)
    implementation(libs.reorderable)
    implementation(libs.onnxruntime.android)
    implementation(libs.work.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    if (storeShots) {
        testImplementation(platform(libs.compose.bom))
        testImplementation(libs.robolectric)
        testImplementation(libs.roborazzi)
        testImplementation(libs.roborazzi.compose)
        testImplementation(libs.compose.ui.test.junit4)
        testImplementation(libs.androidx.test.core)
    }
}

if (storeShots) {
    tasks.withType<Test>().configureEach {
        // Roborazzi only writes images in record mode.
        systemProperty("roborazzi.test.record", "true")
        systemProperty("storeShots.out", providers.gradleProperty("storeShotsOut").getOrElse("/tmp/attic-dev/store-shots/raw"))
        maxHeapSize = "4g"
        // Always render afresh: the output lives outside the build directory, where Gradle cannot see it.
        outputs.upToDateWhen { false }
    }
}
