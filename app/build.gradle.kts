import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
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
    compileSdk = 37

    defaultConfig {
        applicationId = "com.blazingdurtles.android"
        // Firebase (BOM 33+) requires API 23; every Firebase SDK declares minSdk 23.
        minSdk = 23
        targetSdk = 37
        versionCode = 86
        versionName = "1.2.5"
        vectorDrawables.useSupportLibrary = true
        vectorDrawables.generatedDensities()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testApplicationId = "com.blazingdurtles.android.test"
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

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

ksp {
    // Room exports app/schemas/…/68.json here, and Room verifies that schema's identityHash against
    // room_master_table on every open — so losing the export loses the safety net for the whole
    // data-layer port.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    ksp(libs.androidx.room.compiler)

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
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)

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
