import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
//    `java-library`
//    kotlin("jvm")
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
}

//java {
//    sourceCompatibility = JavaVersion.VERSION_11
//    targetCompatibility = JavaVersion.VERSION_11
//}

//tasks.test {
//    useJUnitPlatform() {
//        excludeEngines("junit-vintage")
//    }
//}

android {
    compileSdk = 31

    defaultConfig {
        minSdk = 23
        targetSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
//    testOptions {
//        unitTests.all {
//            useJUnitPlatform()
//        }
//    }
}

kotlin {
    tasks.withType<KotlinCompile>() {
        kotlinOptions {
            sourceCompatibility = JavaVersion.VERSION_11.toString()
            targetCompatibility = JavaVersion.VERSION_11.toString()
            jvmTarget = JavaVersion.VERSION_11.toString()
            freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.time.ExperimentalTime"
        }
    }
}

////region IntegrationTest
//sourceSets {
//    create("intTest") {
//        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
//        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
//    }
//}
//
//val intTestImplementation: Configuration by configurations.getting {
//    extendsFrom(configurations.implementation.get())
//}
//
//configurations["intTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())
//
//val intTest = task<Test>("intTest") {
//    description = "Runs integration tests."
//    group = "verification"
//
//    testClassesDirs = sourceSets["intTest"].output.classesDirs
//    classpath = sourceSets["intTest"].runtimeClasspath
//    shouldRunAfter("test")
//}
//
//tasks.getByName<Test>("intTest") {
//    useJUnitPlatform() {
//        excludeEngines("junit-vintage")
//    }
//}
//
//tasks.check { dependsOn(intTest) }
////endregion

dependencies {
//    implementation(fileTree(include = listOf("*.jar", "*.aar"), org.gradle.internal.impldep.bsh.commands.dir: 'libs'))
    //todo extract to Dependencies
//    "implementation"( "org.whispersystems:curve25519-android:0.5.0")
//    implementation(files("libs/lazysodium-android-5.0.2.aar"))
//    implementation("com.goterl.lazycode:lazysodium-android:4.1.1@aar")

    lazySodium()
    coroutines()
    moshi()
    scarlet()
    json()

    jUnit5()
    mockk()
}