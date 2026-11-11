plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.24"
}

val minecraftVersion = project.property("minecraftVersion") as String
val paperBuild = project.property("paperBuild") as String

group = "de.ermackov"
// CI passes -PreleaseVersion=<mc>-NNNN; local builds are marked as dev builds
version = providers.gradleProperty("releaseVersion").getOrElse("$minecraftVersion-dev")

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("$minecraftVersion.build.$paperBuild-stable")
}

// The opening logic is shared with the Fabric mod, both compile against Mojang names.
sourceSets.main {
    java.srcDir("../common/src/main/java")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 25
    }
    processResources {
        val props = mapOf("version" to project.version, "minecraftVersion" to minecraftVersion)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
