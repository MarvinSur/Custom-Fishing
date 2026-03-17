plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.0"
}

val git : String = versionBanner()
val builder : String = builder()
ext["git_version"] = git
ext["builder"] = builder

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.gradleup.shadow")

    repositories {
        mavenCentral()
    }

    tasks.processResources {
        filteringCharset = "UTF-8"

        filesMatching(arrayListOf("custom-fishing.properties")) {
            expand(rootProject.properties)
        }

        filesMatching(arrayListOf("*.yml", "*/*.yml")) {
            expand(
                Pair("project_version", rootProject.properties["project_version"]!!),
                Pair("config_version", rootProject.properties["config_version"]!!)
            )
        }
    }
}

fun versionBanner(): String {
    return try {
        project.providers.exec {
            commandLine("git", "rev-parse", "--short=8", "HEAD")
        }.standardOutput.asText.get().trim()
    } catch (e: Exception) {
        println("Warning: Could not get git revision: ${e.message}")
        System.getenv("GITHUB_SHA")?.take(8) ?: "Unknown"
    }
}

fun builder(): String {
    return try {
        project.providers.exec {
            commandLine("git", "config", "user.name")
        }.standardOutput.asText.get().trim()
    } catch (e: Exception) {
        println("Warning: Could not get git user name: ${e.message}")
        System.getenv("GITHUB_ACTOR") ?: "Unknown"
    }
}