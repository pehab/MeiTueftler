plugins { id("com.android.application") }
val firebaseConfigured = file("google-services.json").exists()
if (firebaseConfigured) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "de.haberland.meitueftler"
    compileSdk = 37
    defaultConfig {
        applicationId = "de.haberland.meitueftler"
        minSdk = 26
        targetSdk = 37
        versionCode = 17
        versionName = "0.11.1"
        buildConfigField("boolean", "FIREBASE_CONFIGURED", firebaseConfigured.toString())
    }
    buildFeatures { buildConfig = true }
    lint { warningsAsErrors = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    signingConfigs {
        getByName("debug") {
            // CI can supply a persistent private test key through a GitHub Actions secret.
            System.getenv("MEITUEFTLER_SIGNING_STORE")?.let { path ->
                storeFile = file(path)
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }
    buildTypes { release { isMinifyEnabled = false } }
}
dependencies {
    implementation("androidx.activity:activity:1.13.0")
    implementation("com.google.android.play:app-update:2.1.0")
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    constraints {
        implementation("androidx.fragment:fragment:1.9.1") {
            because("Update the transitive Fragment SDK flagged as outdated by Google Play")
        }
    }
    testImplementation("junit:junit:4.13.2")
    // Real JSON implementation for portable file tests; Android supplies org.json at runtime.
    testImplementation("org.json:json:20260814")
}

// Fail CI when Java APIs or compiler diagnostics regress.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked", "-Werror"))
}

// Optional existing APK signing identity restored by CI; never commit a keystore.
android {
    val githubStore = System.getenv("MEI_GITHUB_STORE_FILE")
    if (!githubStore.isNullOrBlank()) {
        signingConfigs {
            create("githubUpdates") {
                storeFile = file(githubStore)
                storePassword = System.getenv("MEI_GITHUB_STORE_PASSWORD")
                keyAlias = System.getenv("MEI_GITHUB_KEY_ALIAS")
                keyPassword = System.getenv("MEI_GITHUB_KEY_PASSWORD")
            }
        }
        buildTypes.getByName("debug").signingConfig = signingConfigs.getByName("githubUpdates")
    }
}
