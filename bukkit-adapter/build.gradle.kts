import java.util.zip.ZipFile

plugins {
    id("uxm.paper-plugin")
}

uxplima {
    slug = "uxmskyblock"
}

repositories {
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":rest-adapter"))
    implementation(project(":persistence-adapter"))
    api(project(":api"))

    compileOnly(libs.bundles.adventure)

    implementation(uxm.uxmlib.common)
    implementation(uxm.uxmlib.item)
    implementation(uxm.uxmlib.gui)
    implementation(uxm.uxmlib.menu)
    implementation(uxm.uxmlib.command)
    implementation(uxm.uxmlib.hud)
    implementation(uxm.uxmlib.bedrock)
    implementation(uxm.uxmlib.condition)
    implementation(uxm.uxmlib.integration)
    implementation(uxm.uxmlib.redis)
    implementation(uxm.uxmlib.schematic)
    // Holograms a player reads are sent per viewer and never spawned.
    implementation(uxm.uxmlib.packet)
    implementation(libs.lettuce.core)
    implementation(uxm.bstats.bukkit)
}

tasks.shadowJar {
    archiveBaseName = "uxmSkyblock"
    // Paper owns Adventure; the convention already leaves slf4j out.
    exclude("net/kyori/**")
    dependencies {
        exclude(dependency("org.jetbrains:annotations:.*"))
        // The library's packet pipeline speaks to the server's own channels, so netty is the server's.
        exclude(dependency("io.netty:.*:.*"))
    }
    // uxmlib-packet is compiled against the Mojang mapped server. This tells Paper not to remap the jar.
    manifest { attributes("paperweight-mappings-namespace" to "mojang") }
    // The libraries the convention does not relocate. org.sqlite stays: the driver finds its native library by its
    // own package name.
    for ((from, to) in listOf(
        "com.google.gson" to "gson",
        "io.lettuce" to "lettuce",
        "reactor" to "reactor",
        "org.reactivestreams" to "reactivestreams",
        "redis.clients" to "jedis",
        "io.javalin" to "javalin",
        "org.eclipse.jetty" to "jetty",
        "jakarta.servlet" to "jakarta.servlet",
        "javax.servlet" to "javax.servlet",
        "kotlin" to "kotlin",
        "org.intellij" to "intellij",
        "org.jetbrains" to "jetbrains",
        "org.postgresql" to "postgresql",
        "org.mariadb" to "mariadb",
    )) {
        relocate(from, "com.uxplima.uxmskyblock.libs.$to")
    }
    // One services file per JDBC driver: merged, never the first copy alone, or only one driver is found.
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    filesNotMatching("META-INF/services/**") { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
}

// Every bundled class sits in this plugin's namespace, so a library added later cannot ship at its own name.
val verifyNamespace by tasks.registering {
    description = "Fails when the shaded jar holds a class outside this plugin's namespace."
    group = "verification"
    dependsOn(tasks.shadowJar)
    val jar = tasks.shadowJar.flatMap { it.archiveFile }
    inputs.file(jar)
    doLast {
        val allowed = listOf("com/uxplima/uxmskyblock/", "org/sqlite/")
        val stray = sortedSetOf<String>()
        ZipFile(jar.get().asFile).use { zip ->
            for (entry in zip.entries()) {
                val path = Regex("^META-INF/versions/\\d+/").replace(entry.name, "")
                if (path.endsWith(".class") && !path.endsWith("module-info.class") && allowed.none { path.startsWith(it) }) {
                    stray.add(path.substringBeforeLast('/'))
                }
            }
        }
        if (stray.isNotEmpty()) {
            throw GradleException("Classes outside this plugin's namespace: " + stray.take(10).joinToString(", "))
        }
    }
}

tasks.named("check") { dependsOn(verifyNamespace) }
