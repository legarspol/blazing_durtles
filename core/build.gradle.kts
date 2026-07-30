import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    // Needed for the `api` configuration below — the Kotlin JVM plugin only applies `java`.
    `java-library`
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

    // `api`, not `implementation`: domain types expose Flow/StateFlow, suspend functions and
    // Instant/LocalDateTime in their public signatures, so :app needs both on its compile
    // classpath to consume them. (:app gets coroutines transitively from androidx lifecycle
    // today — do not rely on that.)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
