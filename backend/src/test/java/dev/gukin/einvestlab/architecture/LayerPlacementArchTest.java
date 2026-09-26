package dev.gukin.einvestlab.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class LayerPlacementArchTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("dev.gukin.einvestlab");

    @Test
    void shouldPlaceEntitiesInDomainLayer() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should().resideInAPackage("..domain..")
                .allowEmptyShould(false);

        rule.check(CLASSES);
    }

    @Test
    void shouldPlaceRepositoriesInPersistenceLayer() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Repository.class)
                .should().resideInAPackage("..infrastructure.persistence..")
                .allowEmptyShould(false);

        rule.check(CLASSES);
    }

    @Test
    void shouldPlaceServicesInApplicationLayer() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Service.class)
                .should().resideInAPackage("..application..")
                .allowEmptyShould(false);

        rule.check(CLASSES);
    }

    @Test
    void shouldPlaceRestControllersInInterfacesLayer() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .should().resideInAPackage("..interfaces..")
                .allowEmptyShould(false);

        rule.check(CLASSES);
    }
}
