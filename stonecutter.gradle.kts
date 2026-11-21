plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.13-SNAPSHOT" apply false
    id("architectury-plugin") version "3.4-SNAPSHOT" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

stonecutter active "1.21.1" /* [SC] DO NOT EDIT */

// Builds every version into build/libs/{mod.version}/{loader}
stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
    group = "versioned"
    ofTask("buildAndCollect")
}

// Builds loader-specific versions into build/libs/{mod.version}/{loader}
for (it in stonecutter.tree.branches) {
    if (it.id.isEmpty()) continue
    val loader = it.id.upperCaseFirst()
    stonecutter registerChiseled tasks.register("chiseledBuild$loader", stonecutter.chiseled) {
        group = "versioned"
        versions { branch, _ -> branch == it.id }
        ofTask("buildAndCollect")
    }
}

// Per-version builds (groundwork for multi-version; extend `supported_versions`
// in gradle.properties and the `versions(...)` list in settings.gradle.kts)
val supportedVersions: List<String> =
    (rootProject.providers.gradleProperty("supported_versions").orElse(""))
        .get().split(",").map { it.trim() }.filter { it.isNotEmpty() }

for (v in supportedVersions) {
    val taskName = "chiseledBuild" + v.replace('.', '_')
    stonecutter registerChiseled tasks.register(taskName, stonecutter.chiseled) {
        group = "versioned"
        versions { _, project -> project.version == v }
        ofTask("buildAndCollect")
    }
}
