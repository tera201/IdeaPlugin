plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.15.0"
    id("org.jetbrains.kotlin.jvm") version "1.8.0"
    id("org.openjfx.javafxplugin") version "0.0.14"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
//    maven {
//        url = uri("https://maven.pkg.github.com/tera201/JavaFXUMLCityBuilder")
//        credentials {
//            username = project.properties["username"].toString()
//            password = project.properties["password"].toString()
//        }
//    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation(fileTree(mapOf("dir" to "lib", "include" to listOf("*.jar"))))
    implementation("org.example:javafx-uml-city-builder:latest.integration")
    implementation("org.example:javafx-uml-graph-idea:0.0.1-SNAPSHOT")
    implementation("org.example:code-to-uml:0.0.2-SNAPSHOT")
    implementation("org.example:swrminer:0.0.2-SNAPSHOT")
}

javafx {
    version = "20"
    modules("javafx.controls")
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    version.set("2023.1")
    type.set("IC") // Target IDE Platform
    plugins.set(listOf("com.intellij.javafx:1.0.4"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "17"
            apiVersion = "1.8"
            languageVersion = "1.8"
        }
    }

    patchPluginXml {
        sinceBuild.set("203")
        untilBuild.set("231.*")
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
        kotlinOptions.languageVersion = "1.8"
        kotlinOptions.apiVersion = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
        kotlinOptions.languageVersion = "1.8"
        kotlinOptions.apiVersion = "1.8"
    }
}
