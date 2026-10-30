buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // The R8 bundled with AGP 8.5.2 (8.5.35) crashes in its tree shaker on this project
        // (ConcurrentModificationException in minifyReleaseWithR8). A newer R8 fixes it.
        classpath("com.android.tools:r8:8.7.18")
    }
}

plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
    id("com.google.devtools.ksp") version "2.0.0-1.0.22" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
