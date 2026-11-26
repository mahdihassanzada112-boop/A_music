pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "A-Music"
include(
    ":app",
    ":core:common", ":core:design", ":core:database", ":core:storage",
    ":features:home", ":features:library", ":features:settings",
)
