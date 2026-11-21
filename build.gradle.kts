import org.gradle.api.JavaVersion

plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
}

val minecraft = stonecutter.current.version

version = "${mod.version}+$minecraft"
group = mod.group
base {
    archivesName.set("figura-relay-common")
}

architectury.common(stonecutter.tree.branches.mapNotNull {
    if (stonecutter.current.project !in it) null
    else it.project.prop("loom.platform")
})

repositories {
    // Official CDN mirror of the NeoForged maven — maven.neoforged.net itself
    // is unreachable from some networks.
    maven("https://neoforged.forgecdn.net/releases/") { name = "NeoForged CDN" }
    maven("https://maven.quiltmc.org/repository/release/")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings(loom.layered {
        mappings("org.quiltmc:quilt-mappings:$minecraft+build.${mod.dep("quilt_mappings_build")}:intermediary-v2")
        officialMojangMappings()
    })
    modCompileOnly("net.fabricmc:fabric-loader:${mod.dep("fabric_loader")}")

    // Official Figura release jar for the target MC version, placed into .libs/
    // locally and downloaded in CI (see .github/workflows/build.yml); not stored
    // in the repository. modCompileOnly makes Loom remap it to project mappings.
    modCompileOnly(files("$rootDir/.libs/figura-${prop("figura_version")}+$minecraft-fabric-mc.jar"))
}

loom {
    // Node projects live under versions/<v>/ — the widener is in the shared root sources
    accessWidenerPath = rootProject.file("src/main/resources/figura-relay.accesswidener")

    mixin {
        // Loom 1.13 disables the legacy mixin AP by default; the architectury
        // transformer still relies on the refmap, so opt back in.
        useLegacyMixinAp = true
        defaultRefmapName.set("figura-relay-common-refmap.json")
        // Common has no fabric.mod.json — register the mixin config explicitly
        // so the annotation processor produces the refmap.
        add("main", "figura-relay-common-refmap.json")
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 21
}

tasks.build {
    group = "versioned"
}
