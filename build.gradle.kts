import net.researchgate.release.ReleaseExtension

plugins {
    java
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.graalvm.buildtools.native") version "0.9.28"
    id("net.researchgate.release") version "3.0.2"
}

group = "org.friesoft.porturl"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
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
    implementation("ch.qos.logback:logback-access")
    implementation("ch.qos.logback:logback-classic")
    implementation("ch.qos.logback.contrib:logback-json-classic:0.1.5")
    implementation("ch.qos.logback.contrib:logback-jackson:0.1.5")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

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
}
tasks.withType<Test> {
    useJUnitPlatform()
}

