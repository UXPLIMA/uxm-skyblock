plugins {
    id("uxm.java")
}

dependencies {
    implementation(project(":core"))
    api(project(":api"))

    implementation(uxm.uxmlib.storage)
    implementation(libs.gson)

    // Network drivers; SQLite comes with uxmlib-storage.
    runtimeOnly(libs.mariadb.jdbc)
    runtimeOnly(libs.postgresql)
}
