import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
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
    sourceSets {
        getByName("androidTest") {
            assets.srcDirs("$projectDir/schemas")
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
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
}

dependencies {
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
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.glide.transformations)
    implementation(libs.gson)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.annotation)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.room.testing)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:unchecked")
    options.isDeprecation = true
}
