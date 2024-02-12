import com.google.protobuf.gradle.proto

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.protobuf")
    id("org.lsposed.lsplugin.jgit") version "1.1"
}

val commitCount = if (System.getenv("CI") != null) {
    System.getenv("CODE")?.toInt() ?: 0
} else {
    jgit.repo()?.commitCount("refs/remotes/origin/main") ?: 0
}

android {
    namespace = "xyz.hyli.connect"
    compileSdk = 34

    defaultConfig {
        applicationId = "xyz.hyli.connect"
        minSdk = 26
        targetSdk = 33
        val majorCode = 1
        versionCode = majorCode * 10000 + commitCount
        val releaseVersionName = "1.0.0"
        versionName = if ( System.getenv("VERSION") != null ) {
            System.getenv("VERSION")
        } else {
            releaseVersionName
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            // 设置支持的SO库架构
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug-key.jks")
            storePassword = "androiddebug"
            keyAlias = "key0"
            keyPassword = "androiddebug"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
        if (System.getenv("CI") != null) {
            create("release") {
                storeFile = file(System.getenv("ANDROID_KEYSTORE_FILE"))
                storePassword = System.getenv("RELEASE_KEY_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }
    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("CI") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        aidl = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    protobuf {
        protoc {
            artifact = "com.google.protobuf:protoc:3.25.2"
        }
        generateProtoTasks {
            all().forEach { task ->
                task.builtins {
                    register("java")
                }
            }
        }
    }
    sourceSets {
        getByName("main") {
            proto {
                srcDir("src/main/proto")
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.22"))
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3-window-size-class-android:1.2.0-beta02")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.0-rc01")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    //noinspection GradleDependency
    implementation("io.github.biezhi:TinyPinyin:2.0.3.RELEASE")
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:2.0.45")
    implementation("com.google.android.material:material:1.11.0")
    implementation("br.com.devsrsouza.compose.icons:font-awesome:1.1.0")
    implementation("br.com.devsrsouza.compose.icons:line-awesome:1.1.0")
    implementation("br.com.devsrsouza.compose.icons:css-gg:1.1.0")
    implementation("androidx.startup:startup-runtime:1.1.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("com.google.protobuf:protobuf-java:3.25.2")
    implementation("com.google.protobuf:protobuf-kotlin:3.25.2")
    implementation("com.google.protobuf:protoc:3.25.2")

    val accompanist_version = "0.32.0"
    implementation("com.google.accompanist:accompanist-adaptive:$accompanist_version")
    implementation("com.google.accompanist:accompanist-navigation-animation:$accompanist_version")

    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    compileOnly(files("libs/XposedBridgeAPI-89.jar"))

    val shizuku_version = "13.1.5"
    implementation("dev.rikka.shizuku:api:$shizuku_version")
    implementation("dev.rikka.shizuku:provider:$shizuku_version")
}