buildscript {
    val dependencyManagementPluginVer: String by rootProject.extra { "1.0.11.RELEASE" }
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("io.spring.gradle:dependency-management-plugin:$dependencyManagementPluginVer")
    }
}

group = "m4gshm.timeout"
version = "0.0.1.SNAPSHOT"

repositories.addAll(rootProject.buildscript.repositories)

plugins {
    `java-library`
    `maven-publish`
}

val asSubproject = pluginManager.hasPlugin("io.spring.dependency-management")
if (!asSubproject) apply<io.spring.gradle.dependencymanagement.DependencyManagementPlugin>()

if (!asSubproject) configure<io.spring.gradle.dependencymanagement.internal.dsl.StandardDependencyManagementExtension> {
    dependencies {
        dependency("org.projectlombok:lombok:[1.18,)")
        dependency("org.slf4j:slf4j-api:[1.7,)")
        dependency("javax.servlet:javax.servlet-api:3.0.1")
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
    implementation("org.slf4j:slf4j-api")
    implementation("io.projectreactor:reactor-core")

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
    sourceCompatibility = JavaVersion.VERSION_1_8
}

val javaVersion = JavaVersion.current()
if (javaVersion.isJava9Compatible) {
    val version9 = "9"
    val sourceSetJava9 = sourceSets.create("java$version9") {
        compileClasspath = sourceSets["main"].compileClasspath
        java {
            srcDirs("src/main/module")
            include("module-info.java")
        }
    }

    val java8Classes = tasks.getByName(sourceSetJava9.compileJavaTaskName, JavaCompile::class) {
        sourceCompatibility = version9
        targetCompatibility = version9
//        if (!javaVersion.isJava9) {
//            options.compilerArgs.addAll(listOf("--release", version9))
//        }
    }

    tasks.jar {
        dependsOn(java8Classes)
        into("META-INF/versions/$version9") {
            from(sourceSetJava9.output)
        }

        manifest {
            attributes(mapOf("Multi-Release" to true))
        }
    }

    apply {
        from("./automaticModules.gradle.kts")
    }

    java {
        modularity.inferModulePath.set(true)
    }

} else {
    tasks.jar {
        manifest {
            attributes("Automatic-Module-Name" to "java.timeout")
        }
    }
}

