plugins {
    id("com.example.msmp.schema-extractor")
}

minecraftManagement {
    onlyReleases.set(true)
    outputDir.set(layout.projectDirectory.dir("protocol-schemas"))
}

