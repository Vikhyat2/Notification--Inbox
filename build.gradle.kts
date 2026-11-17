plugins {
    alias(libs.plugins.android.application) apply false
    // Never applied in a module (AGP 9 has built-in Kotlin); listed only to pin the Kotlin version.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
}