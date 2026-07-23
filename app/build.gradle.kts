import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.smouldering_durtles.wk"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.blazingdurtles.android"
        minSdk = 21
        targetSdk = 35
        versionCode = 85
        versionName = "1.2.4"
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
            resValue("string", "searchprovider_authority", "com.blazingdurtles.android.db.SubjectContentProvider")
            resValue("string", "applabel", "@string/label")
            buildConfigField("String", "FILEPROVIDER_AUTHORITY", "\"com.blazingdurtles.android.fileprovider\"")
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "fileprovider_authority", "com.blazingdurtles.android.debug.fileprovider")
            resValue("string", "searchprovider_authority", "com.blazingdurtles.android.debug.db.SubjectContentProvider")
            resValue("string", "applabel", "@string/labelDebug")
            buildConfigField("String", "FILEPROVIDER_AUTHORITY", "\"com.blazingdurtles.android.debug.fileprovider\"")
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
