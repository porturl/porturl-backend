import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.graalvm.native)
    alias(libs.plugins.benmanes.versions)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.openapi.generator)
    jacoco
}

springBoot {
    buildInfo()
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$projectDir/src/main/resources/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.path)
    apiPackage.set("org.friesoft.porturl.api")
    modelPackage.set("org.friesoft.porturl.dto")
    configOptions.set(mapOf(
        "interfaceOnly" to "true",
        "useSpringBoot3" to "true",
        "useJakartaEe" to "true",
        "openApiNullable" to "false",
        "generateAliasAsModel" to "true",
        "useTags" to "true"
    ))
}

sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated/openapi/src/main/java"))
        }
    }
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}

sonar {
    properties {
        property("sonar.projectKey", "porturl_porturl-backend")
        property("sonar.organization", "porturl")
        property("sonar.coverage.jacoco.xmlReportPaths", "${layout.buildDirectory.get().asFile}/reports/jacoco/test/jacocoTestReport.xml")
    }
}

// Logic to append commit hash to version if not a tagged release
val isRelease = System.getenv("GITHUB_REF")?.startsWith("refs/tags/v") == true || System.getenv("IS_RELEASE_BUILD") == "true"
if (!isRelease) {
    val gitCommit = providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
    }.standardOutput.asText.get().trim()
    
    // Only append if we can get the commit hash (e.g. git is available)
    if (gitCommit.isNotEmpty()) {
        version = "$version-$gitCommit"
    }
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
    implementation(platform(libs.spring.cloud.bom))
    implementation(libs.bundles.spring.starters)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.spring.session.core)
    implementation(libs.caffeine)
    implementation(libs.spring.boot.starter.aspectj)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.opentelemetry)
    implementation(libs.spring.cloud.gateway.server.webmvc)
    implementation(libs.micrometer.tracing.bridge.otel)
    compileOnly(libs.lombok)

    implementation(libs.springdoc.openapi.starter.webmvc.api)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.jackson.dataformat.yaml)

    // logging bundle
    implementation(libs.bundles.logging)
    implementation(libs.opentelemetry.exporter.otlp)
    implementation(libs.opentelemetry.logback.appender)

    implementation(libs.keycloak.admin.client)
    implementation(libs.jakarta.validation)
    implementation(libs.swagger.annotations)

    developmentOnly(libs.spring.boot.devtools)
    runtimeOnly(libs.micrometer.registry.prometheus)
    runtimeOnly(libs.micrometer.registry.otlp)
    runtimeOnly(libs.h2)

    annotationProcessor(libs.spring.boot.configuration.processor)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.security.test)
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
            imageName.set("porturl-backend-${project.version}")
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
        val buildDirectory = layout.buildDirectory.get().asFile
        val infoFile = buildDirectory.resolve("artifact-info.properties")

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
