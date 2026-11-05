plugins {
    id("uxm.java")
}

dependencies {
    implementation(project(":core"))
    api(project(":api"))

    implementation(uxm.uxmlib.storage)
    implementation(libs.gson)

    testImplementation(uxm.uxmlib.storage.testing)
}
