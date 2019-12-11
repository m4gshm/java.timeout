repositories.addAll(rootProject.buildscript.repositories)

plugins {
    id("java")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8

}

dependencies {
    listOf(
            "org.projectlombok:lombok:1.18.8"
    ).forEach { processor ->
        annotationProcessor(processor)
        compileOnly(processor)
        testAnnotationProcessor(processor)
        testCompileOnly(processor)
    }

    compileOnly("javax.servlet:javax.servlet-api:3.1.0")
    compileOnly("org.slf4j:slf4j-api:1.7.26")
    testRuntime("org.slf4j:slf4j-api:1.7.26")
    compileOnly("com.netflix.ribbon:ribbon-loadbalancer:2.3.0")
    compileOnly("io.github.openfeign:feign-core:10.1.0")
    compileOnly("org.springframework.cloud:spring-cloud-openfeign-core:2.1.0.RELEASE")
//    compileOnly("org.springframework:spring-context:5.1.4.RELEASE")
//    compileOnly("org.springframework.boot:spring-boot:2.1.2.RELEASE")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:2.1.2.RELEASE")
    compileOnly("io.projectreactor:reactor-core:3.2.5.RELEASE")
    testCompile("io.projectreactor:reactor-core:3.2.5.RELEASE")

    testCompile("junit:junit:4.12")

}

