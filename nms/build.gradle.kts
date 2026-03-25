subprojects {
    apply(plugin="java")
    extensions.configure<JavaPluginExtension> {
        disableAutoTargetJvm()
    }

    dependencies {
        if (name != "shared") add("compileOnly", project(":nms:shared"))
    }
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        sourceCompatibility = "8"
        targetCompatibility = "8"
    }
}
