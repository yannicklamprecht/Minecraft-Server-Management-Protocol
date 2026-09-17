package com.github.yannicklamprecht.mc.management.plugin;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class MinecraftManagementExtension {
    public static final String DEFAULT_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    public static final String DEFAULT_MIN_MC_VERSION = "1.21.9";
    public static final String DEFAULT_CLIENT_CLASS = "com.github.yannicklamprecht.mc.management.transport.MinecraftManagementClient";

    @Inject
    public MinecraftManagementExtension(ObjectFactory objects) {
        getManifestUrl().convention(DEFAULT_MANIFEST_URL);
        getOnlyReleases().convention(true);
        getVersions().convention(objects.listProperty(String.class).empty());
        getMinMinecraftVersion().convention(DEFAULT_MIN_MC_VERSION);
        getClientClassName().convention(DEFAULT_CLIENT_CLASS);
        // Note: packageName is intentionally not defaulted to force explicit configuration
    }

    public abstract Property<String> getManifestUrl();
    public abstract Property<Boolean> getOnlyReleases();
    public abstract ListProperty<String> getVersions();
    public abstract Property<String> getMinMinecraftVersion();
    public abstract DirectoryProperty getOutputDir();
    public abstract DirectoryProperty getCacheDir();
    public abstract Property<String> getJavaExecutable();

    public abstract DirectoryProperty getSchemasDir();
    public abstract DirectoryProperty getGeneratedSourcesDir();
    public abstract Property<String> getPackageName();
    public abstract Property<String> getClientClassName();
}
