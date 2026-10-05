plugins { id("com.android.application") }
android {
    namespace = "de.haberland.meitueftler"
    compileSdk = 37
    defaultConfig {
        applicationId = "de.haberland.meitueftler"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0"
    }
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
    testImplementation("junit:junit:4.13.2")
}
