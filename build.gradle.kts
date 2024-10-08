plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

val targetJavaVersion = 8

allprojects {
    group = "com.molean"
    version = "1.2.8"

    repositories {
        maven("https://mirrors.huaweicloud.com/repository/maven")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.rosewooddev.io/repository/public/")
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        mavenCentral()
        maven("https://oss.sonatype.org/content/groups/public/")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        sourceCompatibility = targetJavaVersion.toString()
        targetCompatibility = targetJavaVersion.toString()
    }
}
dependencies {
    compileOnly("com.destroystokyo.paper:paper-api:1.14-R0.1-SNAPSHOT")

    // 仅用于不支持 PersistentDataHolder 特性的版本 (1.13或以下)，无需升级到最新版
    implementation("de.tr7zw:item-nbt-api:2.11.1") { isTransitive = false }

    implementation("org.jetbrains:annotations:23.0.0")
    for (proj in rootProject.project(":nms").subprojects) {
        implementation(proj)
    }
}
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        val lang = JavaLanguageVersion.of(targetJavaVersion)
        toolchain.languageVersion.set(lang)
    }
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        mapOf(
            "org.intellij.lang.annotations" to "annotations.intellij",
            "org.jetbrains.annotations" to "annotations.jetbrains",
            "de.tr7zw.changeme.nbtapi" to "nbtapi",
        ).forEach{ (original, target) ->
            relocate(original, "com.molean.staticmap.utils.$target")
        }
    }
    build {
        dependsOn(shadowJar)
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from("LICENSE")
        from(sourceSets.main.get().resources.srcDirs) {
            expand(mapOf("version" to project.version))
            include("plugin.yml")
        }
    }
}
