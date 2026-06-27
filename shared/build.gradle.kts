import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// :cmp 와 동일 패턴 — gitignored local.properties 에서 Android 빌드 설정 주입.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    // expect/actual classes 가 Kotlin 2.x 에서 아직 Beta — flag 로 명시 opt-in 해서
    // VibiDatabase / KSP-generated VibiDatabaseConstructor 등의 Beta 경고 suppress.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // 시뮬레이터 + Apple Silicon 실기기 (iPhone 16 등). Intel Mac 시뮬레이터는 미지원.
    // binaryOption("bundleId") 미지정 시 K/N 이 baseName ("Shared") 를 bundle ID 로
    // fallback → "Cannot infer bundle ID" 경고. iOS bundle ID 규약에 맞춰 명시.
    iosSimulatorArm64().binaries.framework {
        baseName = "Shared"
        isStatic = true
        binaryOption("bundleId", "com.vibi.shared")
    }
    iosArm64().binaries.framework {
        baseName = "Shared"
        isStatic = true
        binaryOption("bundleId", "com.vibi.shared")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.koin.core)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.multiplatform.settings)
            api(libs.androidx.lifecycle.viewmodel)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.play.billing.ktx)
            // Android Google 로그인 (Credential Manager + Sign in with Google)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
            implementation(libs.play.services.ads)
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.sqlite.jdbc)
            }
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.vibi.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 24

        // Android Google 로그인(Credential Manager)의 serverClientId. idToken 의 aud 가 되며
        // BFF GOOGLE_OAUTH_CLIENT_IDS 에 포함돼야 /auth/google 검증을 통과한다. local.properties
        // 에서 주입(gitignore). 미설정 시 빈 문자열 → 런타임에 missing_google_web_client_id 로 실패.
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\""
        )
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
}
