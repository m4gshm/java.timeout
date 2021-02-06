buildscript {
    repositories.addAll(rootProject.buildscript.repositories)
    dependencies {
        classpath("de.jjohannes.gradle:extra-java-module-info:0.5")
    }
}

apply {
    plugin(de.jjohannes.gradle.javamodules.ExtraModuleInfoPlugin::class.java)
}

configure<de.jjohannes.gradle.javamodules.ExtraModuleInfoPluginExtension> {
    this.failOnMissingModuleInfo.set(false)
    automaticModule("javax.servlet-api-3.0.1.jar", "javax.servlet.api")
    automaticModule("feign-core-11.0.jar", "feign.core")
    automaticModule("spring-cloud-openfeign-core-3.0.1.jar", "spring.cloud.openfeign.core")
}