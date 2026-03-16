plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.0"
}

val git: String = versionBanner()
val builder: String = builder()
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

        filesMatching(listOf("custom-fishing.properties")) {
            expand(rootProject.properties)
        }

        filesMatching(listOf("*.yml", "*/*.yml")) {
            expand(
                "project_version" to (rootProject.properties["project_version"] ?: "unknown"),
                "config_version" to (rootProject.properties["config_version"] ?: "unknown")
            )
        }
    }
}

fun versionBanner(): String {
    return try {
        providers.exec {
            commandLine("git", "rev-parse", "--short=8", "HEAD")
        }.standardOutput.asText.get().trim()
    } catch (e: Exception) {
        println("Warning: Could not get git revision: ${e.message}")
        "Unknown"
    }
}

fun builder(): String {
    return try {
        providers.exec {
            commandLine("git", "config", "user.name")
        }.standardOutput.asText.get().trim()
    } catch (e: Exception) {
        println("Warning: Could not get git user name: ${e.message}")
        "Unknown"
    }
}
