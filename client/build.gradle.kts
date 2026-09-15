plugins {
    java
    application
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

sourceSets {
    create("codegen") {
        java.srcDir("src/codegen/java")
    }
    main {
        java.srcDir(layout.buildDirectory.dir("generated/sources/msmp/main/java"))
    }
}

configurations.named("codegenImplementation") {
    extendsFrom(configurations.implementation.get())
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")

    "codegenImplementation"("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
}

val generatedMsmpDir = layout.buildDirectory.dir("generated/sources/msmp/main/java")
val protocolSchemasDir = rootProject.layout.projectDirectory.dir("protocol-schemas")

val generateMsmpSources = tasks.register<JavaExec>("generateMsmpSources") {
    group = "code generation"
    description = "Generates typed Minecraft management protocol DTOs and API facades from OpenRPC schemas."
    dependsOn(tasks.named("compileCodegenJava"))

    classpath = sourceSets["codegen"].runtimeClasspath
    mainClass.set("com.example.msmp.codegen.MsmpOpenRpcGenerator")

    inputs.dir(protocolSchemasDir)
    outputs.dir(generatedMsmpDir)

    args(
        protocolSchemasDir.asFile.absolutePath,
        generatedMsmpDir.get().asFile.absolutePath,
        "com.example.msmp.generated"
    )
}

tasks.named("compileJava") {
    dependsOn(generateMsmpSources)
}

tasks.named<Delete>("clean") {
    delete(generatedMsmpDir)
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
