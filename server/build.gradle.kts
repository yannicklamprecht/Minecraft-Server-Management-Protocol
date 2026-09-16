plugins {
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

tasks.runServer {
    minecraftVersion("1.21.11")
    systemProperty("com.mojang.eula.agree", true)

    // Seed the local configuration, preserving subsequent edits and existing secrets.
    val defaults = layout.projectDirectory.file("server.properties")
    doFirst {
        val properties = runDirectory.file("server.properties").get().asFile
        if (!properties.exists()) {
            properties.parentFile.mkdirs()
            defaults.asFile.copyTo(properties)
        }
        val settings = java.util.Properties().apply {
            properties.inputStream().use { load(it) }
        }
        if (settings.getProperty("management-server-secret").isNullOrBlank()) {
            val defaultSettings = java.util.Properties().apply {
                defaults.asFile.inputStream().use { load(it) }
            }
            settings.setProperty("management-server-secret", defaultSettings.getProperty("management-server-secret"))
            properties.outputStream().use { settings.store(it, "Local Paper server configuration") }
        }
    }
}
