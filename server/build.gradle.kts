import xyz.jpenilla.runpaper.task.RunServer

plugins {
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

/**
 * Seeds [runDirectory]/server.properties from [defaults] on first run only, preserving
 * subsequent manual edits and filling in the default secret only when missing or blank -
 * mirroring the original single-server behavior for every registered server task.
 */
fun RunServer.seedManagementServerProperties(defaults: File) {
    doFirst {
        val properties = runDirectory.file("server.properties").get().asFile
        if (!properties.exists()) {
            properties.parentFile.mkdirs()
            defaults.copyTo(properties)
        }
        val settings = java.util.Properties().apply {
            properties.inputStream().use { load(it) }
        }
        if (settings.getProperty("management-server-secret").isNullOrBlank()) {
            val defaultSettings = java.util.Properties().apply {
                defaults.inputStream().use { load(it) }
            }
            settings.setProperty("management-server-secret", defaultSettings.getProperty("management-server-secret"))
            properties.outputStream().use { settings.store(it, "Local Paper server configuration") }
        }
    }
}

// One local Paper server per MSMP protocol/JSON-RPC version generated under
// client/src/generated/java/.../v<protocol>/, so every generated API can be exercised against
// a real server. Each gets its own run directory, server.properties (game + management ports),
// and Gradle task, so any combination of them can be started in parallel in separate terminals,
// e.g. `./gradlew :server:runServer :server:runServerV3_1_0` (each blocks its own terminal/console).
tasks.runServer {
    minecraftVersion("1.21.11") // MSMP protocol 2.0.0 (v2_0_0) - the default server, unchanged.
    systemProperty("com.mojang.eula.agree", true)
    seedManagementServerProperties(layout.projectDirectory.file("server.properties").asFile)
}

data class AdditionalServer(
    val taskSuffix: String,
    val minecraftVersion: String,
    val protocolVersion: String,
)

listOf(
    AdditionalServer("V1_0_0", "1.21.10", "1.0.0"),
    AdditionalServer("V3_0_0", "26.2", "3.0.0"),
    AdditionalServer("V3_1_0", "26.3", "3.1.0"),
).forEach { server ->
    tasks.register<RunServer>("runServer${server.taskSuffix}") {
        group = "run paper"
        description = "Runs a local Paper ${server.minecraftVersion} server (MSMP protocol ${server.protocolVersion} / " +
            "generated package v${server.taskSuffix.removePrefix("V")}), in parallel with the other runServer* tasks."

        minecraftVersion(server.minecraftVersion)
        systemProperty("com.mojang.eula.agree", true)
        runDirectory.set(layout.projectDirectory.dir("run-${server.taskSuffix.lowercase()}"))
        seedManagementServerProperties(
            layout.projectDirectory.file("server-${server.taskSuffix.lowercase()}.properties").asFile
        )
    }
}
