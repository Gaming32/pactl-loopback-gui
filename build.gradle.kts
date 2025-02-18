plugins {
    id("java")
}

group = "io.github.gaming32"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("commons-io:commons-io:2.18.0")

    implementation("org.slf4j:slf4j-simple:2.0.16")

    compileOnly("org.jetbrains:annotations:24.0.0")
}
