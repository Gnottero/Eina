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
            // DECISIONE: la release usa la firma di debug. Non esiste ancora un keystore di
            // distribuzione e senza firma l'APK non e' installabile per provare R8 sul device.
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
        // Serve alla schermata Info per mostrare la versione installata.
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
    // lifecycle-runtime-ktx arriva con runtime-compose alla stessa versione: nessun uso diretto
    // (niente lifecycleScope, niente repeatOnLifecycle) da giustificare una riga sua.
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    // Necessaria per icone Outlined (FitnessCenter, ecc.) non presenti nel set core
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

    // Niente libreria di grafici: dalla Fase 5b le spezzate e le barre sono Canvas puro
    // (ui/components/MiniLineChart, MiniBarChart, HeatmapCalendar, ActivityRing). Vico era
    // rimasta nel build senza un solo import.

    // Health Connect: frequenza cardiaca e calorie registrate dallo smartwatch. E' l'unico modo
    // di leggere quei dati senza scrivere un'app companion per l'orologio, e resta local-first —
    // i dati stanno gia' sul telefono, l'app li legge e basta.
    // DECISIONE: 1.1.0-alpha10 e non la 1.1.0 stabile. Dalla beta01 la libreria pretende
    // compileSdk 36 e AGP 8.9, cioe' un giro di aggiornamento del build che non c'entra niente
    // con questa fase. L'alpha10 sta su compileSdk 35 e usa le stesse API che servono qui
    // (getSdkStatus, readRecords, PermissionController).
    implementation("androidx.health.connect:connect-client:1.1.0-alpha10")

    // Custom Tabs (donation link)
    implementation("androidx.browser:browser:1.8.0")

    // I test sono unitari puri (dominio, converter, parsing) piu' un test DAO strumentato che usa
    // Room.inMemoryDatabaseBuilder: niente Espresso, niente ui-test di Compose, niente
    // room-testing (serve solo per MigrationTestHelper) e niente coroutines-test — le prove
    // sospese girano con runBlocking.
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
