plugins {
    // On the root class path so a module can apply the Shadow plugin that comes with uxm-gradle without a version.
    id("uxm.paper-plugin") apply false
}

allprojects {
    group = "com.uxplima"
    version = project.findProperty("projectVersion")?.toString() ?: "0.0.1-SNAPSHOT"
}

subprojects {
    // joml ships class files whose type annotations javac warns about; the warning is not ours to fix.
    tasks.withType<JavaCompile>().configureEach { options.compilerArgs.add("-Xlint:-classfile") }
}
