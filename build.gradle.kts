plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.0"
}

val git: String = versionBanner()
val builder: String = builder()
ext["git_version"] = git
ext["builder"] = builder

allprojects {
    group = "net.momirealms"
    version = rootProject.properties["project_version"] ?: "unknown"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://repo.momirealms.net/releases/")
        maven("https://jitpack.io")
        maven("https://repo.codemc.io/repository/maven-public/")
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.gradleup.shadow")

    dependencies {
        // Paper API
        compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
        
        // Cloud Command Framework
        implementation("org.incendo:cloud-core:2.0.0")
        implementation("org.incendo:cloud-services:2.0.0")
        implementation("org.incendo:cloud-bukkit:2.0.0-beta.10")
        implementation("org.incendo:cloud-paper:2.0.0-beta.10")
        implementation("org.incendo:cloud-minecraft-extras:2.0.0-beta.10")
        
        // Configuration
        implementation("dev.dejvokep:boosted-yaml:1.3.7")
        
        // JSON
        implementation("com.google.code.gson:gson:2.10.1")
        
        // Database
        implementation("com.zaxxer:HikariCP:5.0.1")
        implementation("org.mongodb:mongodb-driver-sync:4.10.2")
        implementation("redis.clients:jedis:4.4.3")
        implementation("org.mariadb.jdbc:mariadb-java-client:3.2.0")
        implementation("mysql:mysql-connector-java:8.0.33")
        implementation("org.xerial:sqlite-jdbc:3.44.1.0")
        implementation("com.h2database:h2:2.2.224")
        
        // Utilities
        implementation("org.apache.commons:commons-pool2:2.12.0")
        implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
        implementation("org.apache.commons:commons-lang3:3.14.0")
        
        // Adventure (MiniMessage)
        implementation("net.kyori:adventure-api:4.17.0")
        implementation("net.kyori:adventure-platform-bukkit:4.3.2")
        implementation("net.kyori:adventure-text-minimessage:4.17.0")
        
        // BStats
        implementation("org.bstats:bstats-bukkit:3.0.2")
        
        // Annotations
        compileOnly("org.jetbrains:annotations:24.1.0")
        
    }

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
                "project_version" to (rootProject.properties["project_version"] ?: "unknown"),
                "config_version" to (rootProject.properties["config_version"] ?: "unknown")
            )
        }
    }

    tasks.shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set(rootProject.name + "-" + project.name)
        archiveVersion.set(rootProject.properties["project_version"] as String? ?: "unknown")
        
        mergeServiceFiles()
        minimize()
        
        val libsPackage = "net.momirealms.customfishing.libs"
        
        relocate("org.incendo", "$libsPackage.org.incendo")
        relocate("dev.dejvokep.boostedyaml", "$libsPackage.dev.dejvokep.boostedyaml")
        relocate("com.google.gson", "$libsPackage.com.google.gson")
        relocate("com.zaxxer.hikari", "$libsPackage.com.zaxxer.hikari")
        relocate("com.mongodb", "$libsPackage.com.mongodb")
        relocate("org.bson", "$libsPackage.org.bson")
        relocate("redis.clients", "$libsPackage.redis.clients")
        relocate("org.apache.commons.pool2", "$libsPackage.org.apache.commons.pool2")
        relocate("com.github.benmanes.caffeine", "$libsPackage.com.github.benmanes.caffeine")
        relocate("net.kyori", "$libsPackage.net.kyori")
        relocate("org.bstats", "$libsPackage.org.bstats")
        relocate("org.mariadb", "$libsPackage.org.mariadb")
        relocate("com.mysql", "$libsPackage.com.mysql")
        relocate("org.sqlite", "$libsPackage.org.sqlite")
        relocate("org.h2", "$libsPackage.org.h2")
        
        exclude("META-INF/maven/**")
        exclude("META-INF/versions/**")
        exclude("**/module-info.class")
    }

    tasks.build {
        dependsOn(tasks.shadowJar)
    }
}

tasks.register("shadowJarAll") {
    dependsOn(subprojects.map { it.tasks.shadowJar })
    doLast {
        println("╔══════════════════════════════════════╗")
        println("║   ✅ ALL SHADOW JARS BUILT!          ║")
        println("╚══════════════════════════════════════╝")
        
        subprojects.forEach { subproject ->
            val jarFile = subproject.layout.buildDirectory
                .file("libs/${rootProject.name}-${subproject.name}-${rootProject.properties["project_version"]}.jar")
                .get().asFile
            
            if (jarFile.exists()) {
                val sizeMB = String.format("%.2f", jarFile.length() / 1024.0 / 1024.0)
                println("   📦 ${jarFile.name} - ${sizeMB} MB")
            }
        }
    }
}

fun versionBanner(): String {
    return try {
        providers.exec {
            commandLine("git", "rev-parse", "--short=8", "HEAD")
        }.standardOutput.asText.get().trim()
    } catch (e: Exception) {
        "Unknown"
    }
}

fun builder(): String {
    return try {
        providers.exec {
            commandLine("git", "config", "user.name")
        }.standardOutput.asText.get().trim()
    } catch (e: Exception) {
        "Unknown"
    }
}
