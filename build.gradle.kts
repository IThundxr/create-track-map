plugins {
  kotlin("jvm") version "2.0.21"
  kotlin("plugin.serialization") version "2.0.20"
  java
  id("fabric-loom") version "1.7.+"
  id("com.modrinth.minotaur") version "2.+"
}

val mod_version: String by project
val minecraft_version: String by project
val maven_group: String by project
val archives_base_name: String by project
val create_version_short: String by project

version = mod_version
group = maven_group

val archives_version = "$mod_version+mc$minecraft_version-fabric"

repositories {
  mavenCentral()
  maven("https://jitpack.io")  // MixinExtras, Fabric ASM, BlueMap API
  maven("https://maven.jamieswhiteshirt.com/libs-release")  // Reach Entity Attributes
  maven("https://mvn.devos.one/snapshots/")  // Create Fabric
  maven("https://mvn.devos.one/releases") // Porting Lib Releases
  maven("https://api.modrinth.com/maven")  // LazyDFU
  maven("https://maven.tterrag.com/")  // Flywheel
  maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") // Forge config api port
}

val fabric_loader_version: String by project
val fabric_api_version: String by project
val fabric_kotlin_version: String by project
val create_version: String by project
val porting_lib_version: String by project
val ktor_version: String by project
val kotlin_json_version: String by project
val kotlin_css_version: String by project

dependencies {
  minecraft("com.mojang:minecraft:$minecraft_version")
  mappings(loom.officialMojangMappings())

  modImplementation("net.fabricmc:fabric-loader:$fabric_loader_version")
  modImplementation("net.fabricmc.fabric-api:fabric-api:$fabric_api_version")
  modImplementation("net.fabricmc:fabric-language-kotlin:$fabric_kotlin_version")

  modImplementation("com.simibubi.create:create-fabric-${minecraft_version}:$create_version+mc$minecraft_version")

  implementation(include("io.ktor:ktor-server-core-jvm:$ktor_version")!!)
  implementation(include("io.ktor:ktor-server-cio-jvm:$ktor_version")!!) 
  implementation(include("io.ktor:ktor-server-cors-jvm:$ktor_version")!!)
  implementation(include("org.jetbrains.kotlin-wrappers:kotlin-css:$kotlin_css_version")!!)

  compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlin_json_version")
  compileOnly("com.github.BlueMap-Minecraft:BlueMapAPI:v2.5.1")
}

val targetJavaVersion = 17

tasks {
  processResources {
    inputs.property("version", project.version)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
      expand(
        "version" to version,
        "minecraft_version" to minecraft_version,
        "fabric_loader_version" to fabric_loader_version,
        "fabric_api_version" to fabric_api_version,
        "fabric_kotlin_version" to fabric_kotlin_version
      )
    }
  }

  compileKotlin {
    kotlinOptions.jvmTarget = targetJavaVersion.toString()
  }

  withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
  }

  remapJar {
    archiveBaseName.set(archives_base_name)
    archiveVersion.set(archives_version)
  }
}

java {
  val javaVersion = JavaVersion.toVersion(targetJavaVersion)
  if (JavaVersion.current() < javaVersion) {
    toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
  }
}

val modrinth_id: String by project

modrinth {
  token.set(System.getenv("MODRINTH_TOKEN"))
  projectId.set(modrinth_id)
  versionNumber.set("$mod_version")
  versionName.set("CTM Fabric $mod_version")
  gameVersions.add(minecraft_version)
  loaders.add("fabric")
  loaders.add("quilt")
  dependencies {
    required.project("create-fabric")
    required.project("fabric-api")
    required.project("fabric-language-kotlin")
  }

  uploadFile.set { tasks.remapJar.get().archiveFile }
  changelog.set(project.file("CHANGELOG.md").readText())
  syncBodyFrom.set(project.file("README.md").readText())
}

tasks.modrinth.get().dependsOn(tasks.modrinthSyncBody)
