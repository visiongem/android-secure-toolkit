plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

android {
    namespace = "com.securetoolkit.biometricvault"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.visiongem.android-secure-toolkit"
                artifactId = "biometricvault"
                version = providers.gradleProperty("version").getOrElse("0.1.0")
            }
        }
    }
}

dependencies {
    // 公开 API 暴露 FragmentActivity / Cipher，需要传递给调用方
    api(libs.androidx.biometric)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
}
