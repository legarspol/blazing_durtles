import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

// Firebase config: real values are kept out of git (app/google-services.json is
// gitignored). So the project still builds on a fresh checkout, fall back to the
// committed placeholder when no real config is present. Drop a real
// app/google-services.json in to send diagnostics to an actual Firebase project.
val googleServicesJson = file("google-services.json")
if (!googleServicesJson.exists()) {
    file("google-services-placeholder.json").copyTo(googleServicesJson)
}

android {
    namespace = "com.smouldering_durtles.wk"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.blazingdurtles.android"
        // Firebase (BOM 33+) requires API 23; every Firebase SDK declares minSdk 23.
        minSdk = 23
        targetSdk = 35
        versionCode = 86
        versionName = "1.2.5"
        vectorDrawables.useSupportLibrary = true
        vectorDrawables.generatedDensities()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testApplicationId = "com.blazingdurtles.android.test"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            resValue("string", "fileprovider_authority", "com.blazingdurtles.android.fileprovider")
            resValue(
                "string",
                "searchprovider_authority",
                "com.blazingdurtles.android.db.SubjectContentProvider"
            )
            resValue("string", "applabel", "@string/label")
            buildConfigField(
                "String",
                "FILEPROVIDER_AUTHORITY",
                "\"com.blazingdurtles.android.fileprovider\""
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            resValue(
                "string",
                "fileprovider_authority",
                "com.blazingdurtles.android.debug.fileprovider"
            )
            resValue(
                "string",
                "searchprovider_authority",
                "com.blazingdurtles.android.debug.db.SubjectContentProvider"
            )
            resValue("string", "applabel", "@string/labelDebug")
            buildConfigField(
                "String",
                "FILEPROVIDER_AUTHORITY",
                "\"com.blazingdurtles.android.debug.fileprovider\""
            )
        }
    }
    testOptions {
        reportDir = "$rootDir/test-reports"
        resultsDir = "$rootDir/test-results"
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    buildFeatures {
        buildConfig = true
        resValues = true
        compose = true
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    annotationProcessor(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.annotation)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.room.runtime)
    implementation(libs.jackson.databind)
    implementation(libs.androidx.legacy.support.core.utils)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.security.crypto)
    implementation(libs.lottie)
    implementation(libs.androidx.work.runtime)
    implementation(libs.jsoup)
    implementation(libs.okhttp)
    implementation(libs.okhttp.urlconnection)
    implementation(libs.pikolo)
    implementation(libs.androidx.recyclerview)
    implementation(libs.gson)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-okhttp:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation ("com.google.firebase:firebase-analytics")
    implementation ("com.google.firebase:firebase-crashlytics")

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.annotation)
    androidTestImplementation(libs.androidx.test.ext.junit)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:unchecked")
    options.isDeprecation = true
}
