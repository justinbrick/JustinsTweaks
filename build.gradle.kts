import java.util.UUID

plugins {
    `java-library`
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    // To change the versions see the gradle.properties file
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    runServer {
        minecraftVersion("1.21.10")
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