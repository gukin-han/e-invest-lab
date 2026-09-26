package dev.gukin.einvestlab.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class DependencyArchTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("dev.gukin.einvestlab");

    @Test
    void shouldRespectLayerDependencyDirection() {
        ArchRule rule = layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Domain").definedBy("..domain..")
                .layer("Application").definedBy("..application..")
                .layer("Infra").definedBy("..infrastructure..")
                .layer("Interfaces").definedBy("..interfaces..")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infra", "Interfaces")
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Infra", "Interfaces")
                .whereLayer("Infra").mayNotBeAccessedByAnyLayer()
                .whereLayer("Interfaces").mayNotBeAccessedByAnyLayer();

        rule.check(CLASSES);
    }

    @Test
    void shouldNotDependOnSpringFrameworkFromDomainLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..");

        rule.check(CLASSES);
    }
}
