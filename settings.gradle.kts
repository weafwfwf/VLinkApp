pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack is needed later for the Xray/V2Ray core AAR dependency
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "VLink"
include(":app")
