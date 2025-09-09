// settings.gradle.kts  —— 置換整檔

pluginManagement {
    repositories {
        // 可簡化為這三個倉庫；若你喜歡保留 content 過濾也可以，但不是必需
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    // ✅ 在這裡宣告要給「專案中的 plugins{}」用的版本
    plugins {
        id("androidx.navigation.safeargs.kotlin") version "2.7.7"
        // 若需要也可一併放這個（選擇性）
        id("com.google.gms.google-services") version "4.4.2"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PetNutritionistApp"
include(":app")
