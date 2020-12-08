import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.jfrog.bintray.gradle.BintrayExtension

project.group = "tech.basepair"
project.version = "${version}"

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.4.21"
    id("org.jetbrains.dokka") version "1.4.20"
    id("com.jfrog.bintray") version "1.8.5"
    id("net.researchgate.release") version "2.8.1"

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    `maven-publish`
}

repositories {
    jcenter()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.apache.commons", "commons-text" , "1.8")

    implementation("org.jetbrains.kotlinx", "kotlinx-collections-immutable-jvm", "0.3.3")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

tasks.withType<KotlinCompile> {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=compatibility")
    }
}

tasks.withType<Jar> {
    from(project.projectDir) {
        include("LICENSE")
        into("META-INF")
    }
}

tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("javadoc"))
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}

val sourcesJar by tasks.creating(Jar::class) {
    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

//tasks.register("bintrayUpload") {
//    description = "Uploads to Bintray"
//    dependsOn("publish")
//    dependsOn("bintrayUpload")
//    tasks.getByName("bintrayUpload").mustRunAfter("publish")
//}
//

tasks.getByName("afterReleaseBuild").dependsOn("bintrayUpload")

publishing {
    publications {
        create<MavenPublication>("default") {
            artifact(dokkaJar)
            artifact(sourcesJar)
            from(components["java"])

            pom {
                name.set("Xml Blob")
                description.set("Utility for building Xml documents")
                url.set("https://github.com/basepair-tech/xml-blob")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("jaiew")
                        name.set("Jaie Wilson")
                        email.set("jaie.wilson@basepair.tech")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/basepair-tech/xml-blob.git")
                    developerConnection.set("scm:git:ssh:git@github.com:basepair-tech/xml-blob.git")
                    url.set("https://github.com/basepair-tech/xml-blob.git")
                }
            }
        }
    }
}

bintray {
    user = findProperty("bintray.basepair.user") as String?
    key = findProperty("bintray.basepair.apiKey") as String?
    publish = true
    setPublications("default")
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = "xml-blob"
        desc = "Utility for building XML documents with a simple interface"
        userOrg = "basepair"
        setLicenses("Apache-2.0")
        websiteUrl = "https://github.com/basepair-tech/xml-blob"
        vcsUrl = "https://github.com/basepair-tech/xml-blob.git"
        publicDownloadNumbers = true
        version(delegateClosureOf<BintrayExtension.VersionConfig> {
            name = project.version as String
            gpg(delegateClosureOf<BintrayExtension.GpgConfig> {
                sign = true
                passphrase = findProperty("signing.basepair.password") as String?
            })
        })
    })
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
