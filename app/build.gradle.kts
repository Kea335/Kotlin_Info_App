import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// İmza acarları repoda saxlanılmır — keystore.properties .gitignore-dadır.
val imzaFayli = rootProject.file("keystore.properties")
val imza = Properties().apply {
    if (imzaFayli.exists()) FileInputStream(imzaFayli).use { load(it) }
}
val imzaVar = imzaFayli.exists() && imza.getProperty("storeFile") != null

// Versiya tək yerdədir — gradle.properties-dəki kotlinaz.versiya (T5.7).
// versionCode ondan hesablanır, əl ilə yazılmır və geri getmir:
// 1.2.3 → 10203. Artırmaq: node tools/versiya.js patch|minor|major
val versiyaAdi = providers.gradleProperty("kotlinaz.versiya").get()
val versiyaKodu = versiyaAdi.split('.').map(String::toInt).let { (x, y, z) ->
    require(y in 0..99 && z in 0..99) { "kotlinaz.versiya: minor və patch 0–99 olmalıdır ($versiyaAdi)" }
    x * 10_000 + y * 100 + z
}

android {
    namespace = "az.kotlinaz.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "az.kotlinaz.app"
        minSdk = 24
        targetSdk = 37
        versionCode = versiyaKodu
        versionName = versiyaAdi

        // Cihaz testləri (Room DAO və s.) — T5.1.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (imzaVar) {
            create("release") {
                storeFile = rootProject.file(imza.getProperty("storeFile"))
                storePassword = imza.getProperty("storePassword")
                keyAlias = imza.getProperty("keyAlias")
                keyPassword = imza.getProperty("keyPassword")

                // minSdk 24 olduğu üçün köhnə JAR imzasına ehtiyac yoxdur;
                // v3 açar rotasiyasını mümkün edir.
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (imzaVar) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // Tənzimləmələrdəki versiya sətri BuildConfig.VERSION_NAME-dən gəlir
        // (AGP 9-da defolt söndürülüb).
        buildConfig = true
    }

    lint {
        // Mövcud xəbərdarlıqlar baseline-dadır; yalnız YENİ tapıntılar build-i sındırır.
        // Baseline-ı yeniləmək: ./gradlew updateLintBaseline
        baseline = file("lint-baseline.xml")
        abortOnError = true
        warningsAsErrors = true
        checkDependencies = true

        // «Yeni versiya var» yoxlamaları vaxta bağlıdır: hər yeni release-də
        // mesaj dəyişir və baseline köhnəlir. Asılılıqlar bilərəkdən,
        // ayrıca PR-la yenilənir — bu qaydalar CI qapısına aid deyil.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")

        // Yarımçıq dilin strings.xml-i src/debug/res-dədir və debug-ın
        // locales_config.xml-ində elan olunub; release-ə düşmür (T4.7).
        // Lint isə manifestdəki @xml/locales_config-i main-in faylı ilə
        // tutuşdurur — yanlış pozitiv. Qapını validate-locales.js yoxlayır.
        disable += "UnusedTranslation"
    }

    testOptions {
        unitTests {
            // Testlər saf JVM kodunu yoxlayır — Android resursları lazım deyil.
            isIncludeAndroidResources = false
        }
    }

    sourceSets {
        // Aktivlər (content.json, exercises.json…) unit testdən oxunsun deyə (T1.2).
        getByName("test") {
            resources.srcDir("src/main/assets")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Room sxemi commit olunur — gələcək miqrasiyalar ondan qurulur (T5.1).
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    // Per-app locale API (T3.3): AppCompatDelegate.setApplicationLocales.
    implementation(libs.androidx.appcompat)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    // Öyrənmə tarixçəsi: səhvlər, bookmark, qeydlər, quiz turları (T5.1).
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.kotlin.test.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
