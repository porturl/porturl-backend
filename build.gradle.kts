import net.researchgate.release.ReleaseExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.graalvm.native)
    alias(libs.plugins.researchgate.release)
    alias(libs.plugins.benmanes.versions)
    jacoco
}

group = "org.friesoft.porturl"

java {
    sourceCompatibility = JavaVersion.VERSION_25
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
    implementation(libs.bundles.spring.starters)
    implementation(libs.micrometer.tracing.bridge.brave)
    compileOnly(libs.lombok)

    implementation(libs.springdoc.openapi.starter.webmvc.api)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // logging bundle
    implementation(libs.bundles.logging)

    implementation(libs.keycloak.admin.client)

    developmentOnly(libs.spring.boot.devtools)
    runtimeOnly(libs.micrometer.registry.prometheus)
    runtimeOnly(libs.h2)

    annotationProcessor(libs.spring.boot.configuration.processor)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.security.test)
}

configure<ReleaseExtension> {
    with(git) {
        failOnUnversionedFiles.set(false)
        requireBranch.set("main")
    }

    tagTemplate.set("v\$version")  // Creates tags like v1.0.0
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    finalizedBy("jacocoTestReport")
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn("test")
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    // 🎯 EXCLUSIONS CONFIGURATION - Add/modify patterns here
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/Application.class",  // Main application class
                    "**/config/**",                      // Configuration packages
                    "**/*Config.class",                  // Configuration classes
                    "**/*Configuration.class",           // Spring configuration
                    "**/dto/**",                         // Data transfer objects
                    "**/entity/**",                      // JPA entities
                    "**/model/**",                       // Domain models
                    "**/*Exception.class",               // Custom exceptions
                    "**/constant/**"                     // Constants
                )
            }
        })
    )
}

// 🎯 COVERAGE THRESHOLDS - Adjust percentages here
tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("jacocoTestReport")
    violationRules {
        // Gesamt-Projekt Regel
        rule {
            limit {
                minimum = "0.40".toBigDecimal() // 40% overall
            }
        }

        // Pro-Klasse Regel (verhindert "Coverage-Hiding")
        rule {
            element = "CLASS"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.20".toBigDecimal() // 20% pro Klasse (weniger streng)
            }
        }

        // Branch-Coverage (optional)
        rule {
            element = "CLASS"
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.50".toBigDecimal() // 50% if/else Coverage
            }
        }
    }
}

// GraalVM Native Image configuration
graalvmNative {
    binaries {
        named("main") {
            imageName.set("porturl-backend")
            mainClass.set("org.friesoft.porturl.PortUrlApplication")
            buildArgs.add("--verbose")
            buildArgs.add("-H:+StaticExecutableWithDynamicLibC")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("-H:+UnlockExperimentalVMOptions")
            buildArgs.add("-march=compatibility")
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(25))
                vendor.set(JvmVendorSpec.GRAAL_VM)
            })
        }
    }
}

tasks.register("writeArtifactInfo") {
    doLast {
        val buildDir = layout.buildDirectory.get().asFile
        val infoFile = buildDir.resolve("artifact-info.properties")

        val jarTask = tasks.named<BootJar>("bootJar").get()
        val nativeImageName = graalvmNative.binaries.named("main").get().imageName.get()

        infoFile.writeText(
            buildString {
                appendLine("JAR_NAME=${jarTask.archiveFileName.get()}")
                appendLine("NATIVE_NAME=${nativeImageName}")
            }
        )
    }
}
// Ensure this runs after the artifacts are built
tasks.named("nativeCompile") { finalizedBy("writeArtifactInfo") }
