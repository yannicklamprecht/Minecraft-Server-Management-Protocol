plugins {
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.github.yannicklamprecht.mc.management"
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

val jacksonVersion = "3.2.2"

dependencies {
    implementation("tools.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.palantir.javapoet:javapoet:0.19.0")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    plugins {
        create("minecraftManagementSchemaExtractor") {
            id = "com.github.yannicklamprecht.mc.management.schema-extractor"
            implementationClass = "com.github.yannicklamprecht.mc.management.plugin.MinecraftManagementPlugin"
            displayName = "Minecraft Management Protocol Schema Extractor"
            description = "Downloads Minecraft server JARs, executes data generation, and extracts OpenRPC schemas for the Minecraft Server Management Protocol."
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.test {
    useJUnitPlatform()
}
