@file:Suppress("UnstableApiUsage")

import org.gradle.jvm.tasks.Jar

plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.github.johnrengelman.shadow")
}

val loader = prop("loom.platform")!!
val minecraft = stonecutter.current.version
val common: Project = requireNotNull(stonecutter.node.sibling("")?.project) {
    "No common project for $project"
}

version = "${mod.version}+$minecraft"
base {
    archivesName.set("figura-relay-$loader")
}
architectury {
    platformSetupLoomIde()
    fabric()
}

val commonBundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val shadowBundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

configurations {
    compileClasspath.get().extendsFrom(commonBundle)
    runtimeClasspath.get().extendsFrom(commonBundle)
    get("developmentFabric").extendsFrom(commonBundle)
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings(loom.layered {
        mappings("org.quiltmc:quilt-mappings:$minecraft+build.${common.mod.dep("quilt_mappings_build")}:intermediary-v2")
        officialMojangMappings()
    })
    modImplementation("net.fabricmc:fabric-loader:${common.mod.dep("fabric_loader")}")
    // Only the fabric modules actually used (not the whole fabric-api)
    modImplementation(fabricApi.module("fabric-networking-api-v1", common.mod.dep("fabric_api")))
    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", common.mod.dep("fabric_api")))
    modImplementation(fabricApi.module("fabric-command-api-v2", common.mod.dep("fabric_api")))

    commonBundle(project(common.path, "namedElements")) { isTransitive = false }
    shadowBundle(project(common.path, "transformProductionFabric")) { isTransitive = false }
}

tasks.processResources {
    val props = mapOf(
        "version" to "${mod.version}+$minecraft",
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

tasks.shadowJar {
    exclude("architectury.common.json")
    configurations = listOf(shadowBundle)
    archiveClassifier = "dev-shadow"
}

tasks.remapJar {
    input = tasks.shadowJar.get().archiveFile
    dependsOn(tasks.shadowJar)
    archiveClassifier = "fabric"
}

tasks.jar {
    archiveClassifier = "dev"
}


java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
tasks.named<Jar>("sourcesJar") {
    val commonSources = common.tasks.named<Jar>("sourcesJar")
    dependsOn(commonSources)
    from(commonSources.get().archiveFile.map { zipTree(it) })
}

tasks.register<Copy>("buildAndCollect") {
    group = "versioned"
    description = "Must run through 'chiseledBuild'"
    from(tasks.remapJar.get().archiveFile, tasks.remapSourcesJar.get().archiveFile)
    into(rootProject.layout.buildDirectory.file("libs/${mod.version}/$loader"))
    dependsOn("build")
}

tasks.build {
    group = "versioned"
    description = "Must run through 'chiseledBuild'"
}
