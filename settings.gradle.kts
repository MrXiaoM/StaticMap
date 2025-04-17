pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "StaticMap"
include(":nms")
include(":nms:shared")
include(":nms:1_17")
include(":nms:1_18")
include(":nms:1_19")
include(":nms:1_20_1")
include(":nms:1_20_2")
include(":nms:1_20_4")
include(":nms:1_20_6")
include(":nms:1_21")
include(":nms:1_21_5")
