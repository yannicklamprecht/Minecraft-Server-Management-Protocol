plugins {
    id("com.example.msmp.schema-extractor")
}

minecraftManagement {
    onlyReleases.set(true)
    outputDir.set(layout.projectDirectory.dir("protocol-schemas"))
    generatedSourcesDir.set(layout.projectDirectory.dir("client/src/generated/java"))
    packageName.set("com.example.msmp.generated")
}

