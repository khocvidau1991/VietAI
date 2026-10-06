plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
}

allprojects {
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:1.7.20")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.20")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.20")
            force("org.xerial:sqlite-jdbc:3.46.1.3")

            // Fix MapLibre transitive deps cho AGP 7.4.2
            force("androidx.recyclerview:recyclerview:1.2.1")
            force("androidx.dynamicanimation:dynamicanimation:1.0.0")
            force("androidx.core:core-ktx:1.8.0")
            force("androidx.core:core:1.8.0")
        }
    }
}
