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

    implementation("javax.servlet:javax.servlet-api")
    implementation("org.slf4j:slf4j-api")
    implementation("com.netflix.ribbon:ribbon-loadbalancer")
    implementation("io.github.openfeign:feign-core")
    implementation("org.springframework.cloud:spring-cloud-openfeign-core")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("io.projectreactor:reactor-core")
    testImplementation("junit:junit")

    if (JavaVersion.current().isJava9Compatible()) {
        implementation("javax.xml.ws:jaxws-api")
    }

}

