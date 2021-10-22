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
    compileSdk = 30

    defaultConfig {
        minSdk = 21
        targetSdk = 30

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = jvmVersion
        targetCompatibility = jvmVersion
    }
    kotlinOptions {
        jvmTarget = jvmVersion.toString()
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
            sourceCompatibility = jvmVersion.toString()
            targetCompatibility = jvmVersion.toString()
            jvmTarget = jvmVersion.toString()
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
    okhttp()
    lazySodium()
    coroutines()
    moshi()
    scarlet()

    jUnit5()
    mockk()
}