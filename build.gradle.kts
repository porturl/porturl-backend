import net.researchgate.release.ReleaseExtension

plugins {
    java
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.graalvm.buildtools.native") version "0.11.0"
    id("net.researchgate.release") version "3.1.0"
    id("com.github.ben-manes.versions") version "0.52.0"
}

group = "org.friesoft.porturl"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-rest")
    compileOnly("org.projectlombok:lombok")

    // logging
    implementation("ch.qos.logback.access:logback-access-common:2.0.6")
    implementation("ch.qos.logback.access:logback-access-tomcat:2.0.6")
    implementation("ch.qos.logback:logback-classic")
    implementation("ch.qos.logback.contrib:logback-json-classic:0.1.5")
    implementation("ch.qos.logback.contrib:logback-jackson:0.1.5")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("com.h2database:h2")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

configure<ReleaseExtension> {
    with(git) {
        failOnUnversionedFiles.set(false)
        requireBranch.set("main")
    }

    tagTemplate.set("v\$version")  // Creates tags like v1.0.0
}
tasks.withType<Test> {
    useJUnitPlatform()
}

