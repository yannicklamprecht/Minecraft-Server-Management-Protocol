plugins {
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.example.msmp"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val jacksonVersion = "2.20.0"

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.palantir.javapoet:javapoet:0.6.0")
}

gradlePlugin {
    plugins {
        create("minecraftManagementSchemaExtractor") {
            id = "com.example.msmp.schema-extractor"
            implementationClass = "com.example.msmp.plugin.MinecraftManagementPlugin"
            displayName = "Minecraft Management Protocol Schema Extractor"
            description = "Downloads Minecraft server JARs, executes data generation, and extracts OpenRPC schemas for the Minecraft Server Management Protocol."
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}
