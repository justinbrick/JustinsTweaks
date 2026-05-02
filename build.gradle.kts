import java.util.UUID

plugins {
    `java-library`
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    // To change the versions see the gradle.properties file
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.9-alpha")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    runServer {
        minecraftVersion("26.1.2")
    }

    jar {
        enabled = true
        from("LICENSE") {
            rename { "${it}_${project.version}" }
        }
        manifest {
            attributes(
                "Implementation-Version" to "${project.version}",
            )
        }
    }
}