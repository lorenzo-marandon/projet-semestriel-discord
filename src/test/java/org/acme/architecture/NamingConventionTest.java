package org.acme.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.persistence.Entity;
import jakarta.ws.rs.Path;

@AnalyzeClasses(packages = "org.acme")
public class NamingConventionTest {

        @ArchTest
        static final ArchRule resources_should_be_named_resource = classes()
                        .that().resideInAPackage("..api")
                        .and().areAnnotatedWith(Path.class)
                        .should().haveSimpleNameEndingWith("Resource");

        @ArchTest
        static final ArchRule entities_should_be_named_entity = classes()
                        .that().resideInAPackage("..persistence..")
                        .and().areAnnotatedWith(Entity.class)
                        .should().haveSimpleNameEndingWith("Entity");

        @ArchTest
        static final ArchRule repositories_should_be_named_repository = classes()
                        .that().resideInAPackage("..persistence..")
                        .and().implement(PanacheRepositoryBase.class)
                        .should().haveSimpleNameEndingWith("Repository");

        @ArchTest
        static final ArchRule dtos_should_be_named_dto = classes()
                        .that().resideInAPackage("..dto")
                        .should().haveSimpleNameEndingWith("DTO");

        @ArchTest
        static final ArchRule mappers_should_be_named_mapper = classes()
                        .that().resideInAPackage("..mapper..")
                        .should().haveSimpleNameEndingWith("Mapper");

        @ArchTest
        static final ArchRule filters_should_be_named_filter = classes()
                        .that().resideInAPackage("..filters..")
                        .should().haveSimpleNameEndingWith("Filter")
                        .as("Classes in filters subpackages must have the 'Filter' suffix (e.g., TestDebugFilter)");

        @ArchTest
        static final ArchRule clients_should_be_named_client = classes()
                        .that().resideInAPackage("..client..")
                        .and().areInterfaces()
                        .should().haveSimpleNameEndingWith("Client")
                        .as("Rest Clients should have the 'Client' suffix");
}
