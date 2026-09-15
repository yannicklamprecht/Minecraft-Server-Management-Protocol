plugins {
    java
    application
    id("com.example.msmp.schema-extractor")
}

group = "com.example.msmp"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val jacksonVersion = "2.20.0"
val slf4jVersion = "2.0.17"
val logbackVersion = "1.5.18"
val junitVersion = "5.11.4"

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


minecraftManagement {
    packageName.set("de.craftstuebchen.mc.management")
}

application {
    mainClass.set("com.example.msmp.Main")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform()
}
