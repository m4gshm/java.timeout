group = "m4gshm"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

plugins {
    `java-library`
    `maven-publish`
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

dependencyManagement {
    dependencies {
        dependency("org.projectlombok:lombok:[1.18,)")
        dependency("org.slf4j:slf4j-api:[1.7,)")
        dependency("javax.servlet:javax.servlet-api:[3.0,)")
        dependency("io.github.openfeign:feign-core:[10,)")
        dependency("io.projectreactor:reactor-core:[0.9,)")
        dependency("org.springframework.cloud:spring-cloud-openfeign-core:[2,)") {
            exclude("jakarta.annotation:jakarta.annotation-api")
        }
        dependency("junit:junit:[4,)")
    }
}

dependencies {
    listOf(
            "org.projectlombok:lombok"
    ).forEach { processor ->
        annotationProcessor(processor)
        compileOnly(processor)
        testAnnotationProcessor(processor)
        testCompileOnly(processor)
    }

    compileOnly("javax.servlet:javax.servlet-api")
    compileOnly("io.github.openfeign:feign-core")
    compileOnly("org.springframework.cloud:spring-cloud-openfeign-core")
    compileOnly("org.slf4j:slf4j-api")
    compileOnly("io.projectreactor:reactor-core")

    testImplementation("org.slf4j:slf4j-api")
    testImplementation("io.projectreactor:reactor-core")
    testImplementation("junit:junit")

    val javaVersion = JavaVersion.current()
    if (javaVersion.isJava9Compatible && !javaVersion.isJava9) {
        compileOnly("javax.xml.ws:jaxws-api:[2,)") {
            isTransitive = false
        }
    }
}

tasks.compileJava {
    options.apply {
        compilerArgs.add("-Xlint:unchecked")
        isDeprecation = true
        isWarnings = false
    }
}

java {
    withSourcesJar()
}

val buildForJava8 by extra { true }
if (!buildForJava8) {
    java {
        targetCompatibility = JavaVersion.VERSION_1_9
        sourceCompatibility = JavaVersion.VERSION_1_9
        modularity.inferModulePath.set(true)
    }
    sourceSets["main"].java.srcDirs("src/main/module")

    apply {
        from("./automaticModules.gradle.kts")
    }
} else {
    java {
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }
    tasks.jar {
        manifest {
            attributes["Automatic-Module-Name"] = "m4gshm.java.timeout"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("java") {
            repositories {
                val mavenTempLocalRepoUrl: String? by rootProject
                if (mavenTempLocalRepoUrl != null) maven {
                    name = "mavenTempLocal"
                    url = uri(mavenTempLocalRepoUrl!!)
                }

                val mavenRepoSnapshotUrl: String? by rootProject
                val mavenRepoReleaseUrl: String? by rootProject
                when {
                    version.endsWith("-SNAPSHOT") -> mavenRepoSnapshotUrl
                    else -> mavenRepoReleaseUrl
                }?.let { repoUrl ->
                    maven {
                        name = "mavenCustom"
                        url = uri(repoUrl)
                        isAllowInsecureProtocol = true
                        credentials {
                            val mavenRepoUser: String? by rootProject
                            val mavenRepoPassword: String? by rootProject
                            username = mavenRepoUser
                            password = mavenRepoPassword
                        }
                    }
                }
            }

            pom {
                properties.put("maven.compiler.target", "${java.targetCompatibility}")
                properties.put("maven.compiler.source", "${java.sourceCompatibility}")
                developers {
                    developer {
                        id.set("m4gshm")
                        name.set("Bulgakov Alexander")
                        email.set("buls@yandex.ru")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/m4gshm/java.timeout.git")
                    developerConnection.set("scm:git:https://github.com/m4gshm/java.timeout.git")
                    url.set("https://github.com/m4gshm/java.timeout")
                }
            }

//            versionMapping {
//                usage("java-api") {
//                    fromResolutionOf("runtimeClasspath")
//                }
//                usage("java-runtime") {
//                    fromResolutionResult()
//                }
//            }

            from(components["java"])
        }
    }
}

