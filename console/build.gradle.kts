import java.util.Properties

plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("gg.jte.gradle") version "3.2.4"
}

group = "com.github.yannicklamprecht.mc.management"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val springBootVersion = "4.1.1"
val jteVersion = "3.2.4"

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))

    implementation(project(":spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("gg.jte:jte-spring-boot-starter-4:$jteVersion")

    // spring-boot-starter-test alone no longer pulls in @WebMvcTest/MockMvc support in Boot 4;
    // that now lives in the dedicated webmvc test starter.
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}

jte {
    generate()
}

springBoot {
    mainClass.set("com.github.yannicklamprecht.mc.management.console.ManagementConsoleApplication")
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    val serverDefaults = rootProject.layout.projectDirectory.file("server/server.properties")
    val localServerProperties = rootProject.layout.projectDirectory.file("server/run/server.properties")
    doFirst {
        // Matches the "skyblock" server configured in src/main/resources/application.yml.
        if (environment["SKYBLOCK_MANAGEMENT_SECRET"]?.toString().isNullOrBlank()) {
            val defaults = Properties().apply {
                serverDefaults.asFile.inputStream().use { load(it) }
            }
            val localSettings = Properties().apply {
                if (localServerProperties.asFile.exists()) {
                    localServerProperties.asFile.inputStream().use { load(it) }
                }
            }
            val secret = localSettings.getProperty("management-server-secret")
                ?.takeIf { it.isNotBlank() }
                ?: defaults.getProperty("management-server-secret")
            environment("SKYBLOCK_MANAGEMENT_SECRET", secret)
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform()
}
