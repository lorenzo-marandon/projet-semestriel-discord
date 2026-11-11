package org.acme.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "org.acme.catalog.product", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerArchitectureTest {

    @ArchTest
    static final ArchRule layered_architecture = layeredArchitecture()
            .consideringAllDependencies()
            .layer("API").definedBy("..api..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Infrastructure").definedBy("..infrastructure..")

            .whereLayer("API").mayOnlyAccessLayers("Application")
            .whereLayer("Application").mayOnlyAccessLayers("Domain", "Infrastructure")
            .whereLayer("Infrastructure").mayOnlyAccessLayers("Domain", "Application")
            .whereLayer("Domain").mayNotAccessAnyLayer()

            // Essential ignores for Reactive/Mutiny and common libraries
            .ignoreDependency(resideInAnyPackage("org.acme.."), resideInAnyPackage("java.."))
            .ignoreDependency(resideInAnyPackage("org.acme.."), resideInAnyPackage("jakarta.."))
            .ignoreDependency(resideInAnyPackage("org.acme.."), resideInAnyPackage("io.quarkus.."))
            .ignoreDependency(resideInAnyPackage("org.acme.."), resideInAnyPackage("io.smallrye.mutiny..")) // Add this!
            .ignoreDependency(resideInAnyPackage("org.acme.."), resideInAnyPackage("org.slf4j.."))
            .ignoreDependency(resideInAnyPackage("org.acme.."), resideInAnyPackage("com.fasterxml.jackson.."))
            .ignoreDependency(resideInAnyPackage("org.acme.."), resideInAnyPackage("org.eclipse.microprofile.."));

    @ArchTest
    static final ArchRule application_and_api_should_be_stateless = classes() // Use classes(), not noClasses()
            .that().resideInAnyPackage("..application..", "..api..")
            .and().haveSimpleNameNotEndingWith("DTO") // DTOs and Requests are data, not services
            .and().haveSimpleNameNotEndingWith("Request")
            .should().haveOnlyFinalFields()
            .as("To satisfy [NFR-RELY-02], services and resources must be stateless.");
}
