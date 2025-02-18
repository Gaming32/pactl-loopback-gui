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

val fatJar by tasks.registering(Jar::class) {
    group = "build"

    archiveClassifier.set("all")
    dependsOn(tasks.jar)

    manifest {
        attributes["Main-Class"] = "io.github.gaming32.pactlloopbackgui.Main"
    }

    from(
        (tasks.jar.get().outputs.files + configurations.runtimeClasspath.get().files)
            .map { if (it.isDirectory) it else zipTree(it) }
    )
    duplicatesStrategy = DuplicatesStrategy.WARN
}
tasks.assemble.get().dependsOn(fatJar)
