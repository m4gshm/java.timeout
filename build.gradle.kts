repositories.addAll(rootProject.buildscript.repositories)

plugins {
    id("java")
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

    compileOnly("javax.servlet:javax.servlet-api")
    compileOnly("com.netflix.ribbon:ribbon-loadbalancer")
    compileOnly("io.github.openfeign:feign-core")
    compileOnly("org.springframework.cloud:spring-cloud-openfeign-core")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    implementation("org.slf4j:slf4j-api")
    implementation("io.projectreactor:reactor-core")

    testImplementation("junit:junit")

    if (JavaVersion.current().isJava9Compatible()) {
        compileOnly("javax.xml.ws:jaxws-api")
    }

}

