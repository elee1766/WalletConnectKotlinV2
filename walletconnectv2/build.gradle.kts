import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    `maven-publish`
}

group = "com.github.WalletConnect-Labs"

tasks.withType<Test> {
    useJUnitPlatform()
}

android {
    compileSdk = 30

    defaultConfig {
        minSdk = 21
        targetSdk = 30

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = jvmVersion
        targetCompatibility = jvmVersion
    }

    kotlinOptions {
        jvmTarget = jvmVersion.toString()
    }

    sourceSets {
//        getByName("main").jniLibs.srcDir("src/main/jniLibs")
//        getByName("debug").jniLibs.srcDir("src/main/jniLibs")
//        getByName("release").jniLibs.srcDir("src/main/jniLibs")
    }

}

kotlin {
    tasks.withType<KotlinCompile>() {
        kotlinOptions {
            sourceCompatibility = jvmVersion.toString()
            targetCompatibility = jvmVersion.toString()
            jvmTarget = jvmVersion.toString()
            freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.time.ExperimentalTime"
        }
    }
}

dependencies {
//    implementation(fileTree(mapOf("dir" to "src/main/jniLibs", "include" to listOf("*.jar"))))
//    implementation(fileTree(mapOf("dir" to "src/main/libs", "include" to listOf("*.jar"))))

    okhttp()
    lazySodium()
    coroutines()
    moshi()
    scarlet()
    jUnit5()
    mockk()
    timber()
}

afterEvaluate {
    publishing {
        publications {
            // Creates a Maven publication called "release".
            create("release", MavenPublication::class) {
                // Applies the component for the release build variant.
                from(components.getByName("release"))
                // You can then customize attributes of the publication as shown below.
                groupId = "com.walletconnect"
                artifactId = "walletconnectv2"
                version = "1.0.0-alpha01"
            }
        }
    }
}