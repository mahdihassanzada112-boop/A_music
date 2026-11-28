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
    ":core:common", ":core:design", ":core:database", ":core:storage", ":core:audio",
    ":core:theming",
    ":themes:darkmodern", ":themes:glass",
    ":data",
    ":features:home", ":features:library", ":features:settings", ":features:player",
    ":features:studio", ":features:customize",
)
