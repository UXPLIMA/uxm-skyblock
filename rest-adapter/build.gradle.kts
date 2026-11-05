plugins {
    id("uxm.java")
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))

    implementation(libs.javalin)
    implementation(libs.gson)
    compileOnly(libs.slf4j.api)
}
