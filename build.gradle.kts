import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// jlynx version
version = "1.8.1"

plugins {
    // Apply the java-library plugin to add support for Java Library
    `java-library`
    kotlin("jvm") version "1.3.31"
}

repositories {
    jcenter()
}

dependencies {
    // This dependency is exported to consumers, that is to say found on their compile classpath.
    //api("org.apache.commons:commons-math3:3.6.1")

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    //implementation("com.google.guava:guava:27.0.1-jre")

    // Use JUnit test framework
    testImplementation("junit:junit:4.12")
    testImplementation(kotlin("stdlib-jdk8"))

    // jdbc drivers
    //testRuntime("com.h2database:h2:1.3.176")
    testRuntime("org.hsqldb:hsqldb:2.4.1")
    testRuntime("org.postgresql:postgresql:42.2.11")
    testRuntime("mysql:mysql-connector-java:8.0.11")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.6"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.6"
}

