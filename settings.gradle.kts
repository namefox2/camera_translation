pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ARTranslator"
include(":app")
include(":core:ui")
include(":core:translation")
include(":core:database")
include(":feature:ar")
include(":feature:text")
include(":feature:voice")
include(":feature:phrasebook")
include(":feature:language")
