plugins {
    id("com.gradleup.shadow") version "9.3.0" apply false
}

allprojects {
    group = "net.momirealms"
    version = project.findProperty("project_version") as String? ?: "unknown"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://repo.momirealms.net/releases/")
        maven("https://jitpack.io")
    }
}

subprojects {
    // Apply plugin java dan shadow
    apply(plugin = "java")
    apply(plugin = "com.gradleup.shadow")

    // Konfigurasi Java
    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    tasks.processResources {
        filteringCharset = "UTF-8"
        filesMatching(listOf("custom-fishing.properties")) {
            expand(project.properties)
        }
        filesMatching(listOf("*.yml", "*/*.yml")) {
            expand(
                "project_version" to (project.findProperty("project_version") ?: "unknown"),
                "config_version" to (project.findProperty("config_version") ?: "unknown")
            )
        }
    }

    // Dependencies
    dependencies {
        compileOnly("io.papermc.paper:paper-api:${project.findProperty("paper_version")}")
        compileOnly("net.kyori:adventure-api:${project.findProperty("adventure_bundle_version")}")
        compileOnly("net.kyori:adventure-platform-bukkit:${project.findProperty("adventure_platform_version")}")
        compileOnly("net.kyori:adventure-text-minimessage:${project.findProperty("adventure_bundle_version")}")
        compileOnly("org.jetbrains:annotations:${project.findProperty("jetbrains_annotations_version")}")
        compileOnly("com.google.code.gson:gson:${project.findProperty("gson_version")}")

        implementation("org.incendo:cloud-core:${project.findProperty("cloud_core_version")}")
        implementation("org.incendo:cloud-services:${project.findProperty("cloud_services_version")}")
        implementation("org.incendo:cloud-bukkit:${project.findProperty("cloud_bukkit_version")}")
        implementation("org.incendo:cloud-paper:${project.findProperty("cloud_paper_version")}")
        implementation("org.incendo:cloud-minecraft-extras:${project.findProperty("cloud_minecraft_extras_version")}")
        implementation("dev.dejvokep:boosted-yaml:${project.findProperty("boosted_yaml_version")}")

        // Database
        implementation("com.mysql:mysql-connector-java:${project.findProperty("mysql_driver_version")}")
        implementation("org.xerial:sqlite-jdbc:${project.findProperty("sqlite_driver_version")}")
        implementation("com.zaxxer:HikariCP:${project.findProperty("hikari_version")}")

        // Utilities
        implementation("org.apache.commons:commons-pool2:${project.findProperty("commons_pool_version")}")
        implementation("org.apache.commons:commons-lang3:3.14.0")
        implementation("com.github.ben-manes.caffeine:caffeine:${project.findProperty("caffeine_version")}")
        implementation("org.bstats:bstats-bukkit:${project.findProperty("bstats_version")}")
    }

    // Konfigurasi shadowJar hanya untuk module core
    if (project.name == "core") {
        tasks.shadowJar {
            archiveClassifier.set("")
            archiveBaseName.set(rootProject.name)
            archiveVersion.set(project.version.toString())

            mergeServiceFiles()
            minimize()

            val libsPackage = "net.momirealms.customfishing.libs"
            relocate("org.incendo", "$libsPackage.org.incendo")
            relocate("dev.dejvokep.boostedyaml", "$libsPackage.dev.dejvokep.boostedyaml")
            relocate("com.zaxxer.hikari", "$libsPackage.com.zaxxer.hikari")
            relocate("org.apache.commons.pool2", "$libsPackage.org.apache.commons.pool2")
            relocate("org.apache.commons.lang3", "$libsPackage.org.apache.commons.lang3")
            relocate("com.github.benmanes.caffeine", "$libsPackage.com.github.benmanes.caffeine")
            relocate("org.bstats", "$libsPackage.org.bstats")
            relocate("com.mysql", "$libsPackage.com.mysql")
            relocate("org.sqlite", "$libsPackage.org.sqlite")

            exclude("META-INF/maven/**")
            exclude("META-INF/versions/**")
            exclude("**/module-info.class")
            exclude("LICENSE*")
            exclude("NOTICE*")
        }

        tasks.build {
            dependsOn(tasks.shadowJar)
        }
    } else {
        // Module api dan compatibility: jar biasa
        tasks.jar {
            archiveBaseName.set(rootProject.name + "-" + project.name)
            archiveVersion.set(project.version.toString())
        }
    }
}

// Task untuk build semua module
tasks.register("buildAll") {
    dependsOn(subprojects.map { it.tasks.named("build") })
}