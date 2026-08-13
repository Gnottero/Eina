plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.eina.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.eina.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // DECISIONE: the release build uses the debug signature. There is no distribution
            // keystore yet, and an unsigned APK cannot be installed to test R8 on a device.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        // Used by the About screen to show the installed version.
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // lifecycle-runtime-ktx comes with runtime-compose at the same version, and there is no direct
    // use (no lifecycleScope, no repeatOnLifecycle) to justify a line of its own.
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    // Needed for Outlined icons (FitnessCenter and friends) missing from the core set.
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Koin
    implementation("io.insert-koin:koin-android:3.5.6")
    implementation("io.insert-koin:koin-androidx-compose:3.5.6")

    // Coil
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")

    // No charting library: lines and bars are pure Canvas (ui/components/MiniLineChart,
    // MiniBarChart, HeatmapCalendar, ActivityRing).

    // Health Connect: heart rate and calories recorded by the smartwatch. It is the only way to
    // read that data without writing a companion app, and it stays local-first — the data is
    // already on the phone and the app only reads it.
    // DECISIONE: 1.1.0-alpha10 and not the stable 1.1.0. From beta01 the library requires
    // compileSdk 36 and AGP 8.9, a build upgrade unrelated to this work; alpha10 runs on
    // compileSdk 35 and exposes the same APIs used here (getSdkStatus, readRecords,
    // PermissionController).
    implementation("androidx.health.connect:connect-client:1.1.0-alpha10")

    // Custom Tabs (donation link)
    implementation("androidx.browser:browser:1.8.0")

    // Tests are plain unit tests (domain, converters, parsing) plus one instrumented DAO test on
    // Room.inMemoryDatabaseBuilder: no Espresso, no Compose ui-test, no room-testing (only needed
    // for MigrationTestHelper) and no coroutines-test — suspending tests run with runBlocking.
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
