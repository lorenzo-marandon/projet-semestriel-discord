package org.acme.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;

@AnalyzeClasses(packages = "org.acme.discord.domain", importOptions = { ImportOption.DoNotIncludeTests.class })
class DomainIsolationTest {

        @ArchTest
        static final ArchRule domain_should_not_depend_on_frameworks = noClasses()
                        .that()
                        .resideInAPackage("..domain..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(
                                        "io.quarkus..",
                                        "jakarta.ws.rs..",
                                        "jakarta.persistence..",
                                        "org.hibernate..")
                        .as("Domain must stay framework independent");

        @ArchTest
        static final ArchRule domain_should_not_access_infrastructure = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .as("Domain logic must not depend on technical details (DB/Client)");
}
