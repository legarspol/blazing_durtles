import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// compileJava has no sources here, but it still inherits the Gradle JVM and Kotlin
// validates the two targets match. Pin it to 17 like :app so a newer local JDK
// (e.g. 25) doesn't fail the build.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.kotlin.test)
}
