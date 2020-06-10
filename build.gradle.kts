repositories.addAll(rootProject.buildscript.repositories)

plugins {
    java
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
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

    compileOnly(platform("org.springframework.cloud:spring-cloud-dependencies"))
    compileOnly("javax.servlet:javax.servlet-api")
    compileOnly("io.github.openfeign:feign-core")
    compileOnly("org.springframework.cloud:spring-cloud-openfeign-core")
    compileOnly("org.slf4j:slf4j-api")
    compileOnly("io.projectreactor:reactor-core")

    testImplementation("junit:junit")

    if (JavaVersion.current().isJava9Compatible()) {
        compileOnly("javax.xml.ws:jaxws-api")
    }
}

