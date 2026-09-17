import java.util.Properties

plugins {
    java
    application
    id("com.github.yannicklamprecht.mc.management.schema-extractor")
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

val jacksonVersion = "3.2.2"
val slf4jVersion = "2.0.17"
val logbackVersion = "1.5.18"
val junitVersion = "5.11.4"

dependencies {
    implementation("tools.jackson.core:jackson-databind:$jacksonVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


minecraftManagement {
    packageName.set("com.github.yannicklamprecht.mc.management")
}

application {
    mainClass.set("com.github.yannicklamprecht.mc.management.Main")
}

tasks.named<JavaExec>("run") {
    val serverDefaults = rootProject.layout.projectDirectory.file("server/server.properties")
    val localServerProperties = rootProject.layout.projectDirectory.file("server/run/server.properties")
    doFirst {
        if (environment["MINECRAFT_MANAGEMENT_SECRET"]?.toString().isNullOrBlank()) {
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
            environment("MINECRAFT_MANAGEMENT_SECRET", secret)
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
