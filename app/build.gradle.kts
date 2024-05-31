import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.proto
import java.util.Locale

plugins {
    alias(libs.plugins.agp.app)
    id(libs.plugins.booster.get().pluginId)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.compose.compiler)
    id(libs.plugins.protobuf.get().pluginId)
    alias(libs.plugins.jgit)
}

if (!File("${rootDir}/app/dict.txt").exists()) {
    val cmd = "python3 ${rootDir}/scripts/gen_dict.py ${rootDir}/app"
    val proc = Runtime.getRuntime().exec(cmd)
    proc.waitFor()
}

val branch = jgit.repo()?.raw?.branch.let {
        if (it == "release" || it == "dev") it else "canary"
    }

android {
    namespace = "xyz.hyli.connect"
    compileSdk = 34

    defaultConfig {
        applicationId = "xyz.hyli.connect"
        minSdk = 26
        targetSdk = 33
        val majorCode = 1
        versionCode = majorCode * 10000 + (jgit.repo()?.commitCount("refs/remotes/origin/$branch") ?: 0)
        val latestTag = jgit.repo()?.latestTag ?: "1.0.0"
        val commitId = jgit.repo()?.raw?.resolve("HEAD")?.name?.take(7) ?: "canary"
        versionName = if (branch == "release") {
            latestTag
        } else {
            latestTag + "_" + commitId
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
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
        create("release") {
            if (System.getenv("release_key_exists") == "true") {
                storeFile = file(System.getenv("ANDROID_KEYSTORE_FILE"))
                storePassword = System.getenv("RELEASE_KEY_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            } else {
                storeFile = file("debug-key.jks")
                storePassword = "androiddebug"
                keyAlias = "key0"
                keyPassword = "androiddebug"
            }
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }
    buildTypes {
        // CI TEST
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("CI") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    defaultConfig {
        buildConfigField("String", "RELEASE_CHANNEL", "\"${branch.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                Locale.getDefault()
            ) else it.toString()
        }}\"")
    }
    android.applicationVariants.all {
        outputs.all {
            if (this is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                this.outputFileName = "HyLi-Connect_$versionName($versionCode)_$branch.apk"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    buildFeatures {
        compose = true
        viewBinding = true
        aidl = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    kotlin {
        sourceSets.all {
            languageSettings {
                languageVersion = "2.0"
            }
        }
    }
    protobuf {
        protoc {
            artifact = libs.protobuf.protoc.get().toString()
        }
        generateProtoTasks {
            all().forEach { task ->
                task.builtins {
                    id("java") {
                        option("lite")
                    }
                }
            }
        }
    }
    sourceSets {
        getByName("main") {
            proto {
                srcDir("src/main/proto")
                include("**/*.proto")
            }
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(platform(libs.kotlin.bom))
    implementation(libs.kotlinx.coroutines)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.material)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.material3.window.size)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.fastjson2.kotlin)
    implementation(libs.xxpermissions)
    implementation(libs.c2pinyin)
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.kotlin)
    implementation(libs.protobuf.protoc)
    implementation(libs.accompanist.adaptive)
    implementation(libs.accompanist.navigation.animation)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.hiddenapibypass)
    compileOnly(files("libs/XposedBridgeAPI-89.jar"))
}
