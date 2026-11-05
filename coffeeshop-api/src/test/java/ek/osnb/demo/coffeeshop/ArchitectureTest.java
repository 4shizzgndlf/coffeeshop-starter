package ek.osnb.demo.coffeeshop;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Checks the architecture rules from the README on every build.
 * A new feature package (e.g. "review") is checked automatically.
 *
 * Run with: ./mvnw test
 */
@AnalyzeClasses(packages = ArchitectureTest.ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String ROOT = "ek.osnb.demo.coffeeshop";

    // Rule 1: a feature only shows its contract to the rest of the app.
    // Everything else (entities, repositories, services, controllers, DTOs) is
    // package-private, so the compiler stops other features from using it.
    @ArchTest
    static final ArchRule featuresOnlyExposeTheirContract =
            classes().that().resideInAPackage(ROOT + "..")
                    .and().resideOutsideOfPackages(ROOT, ROOT + ".common..")
                    .and().arePublic()
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Api")
                    .orShould().haveSimpleNameEndingWith("Info")
                    .orShould().haveSimpleNameEndingWith("Event")
                    .orShould().haveSimpleNameEndingWith("Exception")
                    .because("other features may only use a feature's *Api, *Info, *Event and *Exception types. "
                            + "Make every other class package-private (no 'public'), "
                            + "also @Configuration classes such as SecurityConfig: Spring finds them anyway");

    // Rule 2: entities are never shared between features. Another feature stores
    // the id (Long productId) instead of a JPA association (@ManyToOne Product).
    @ArchTest
    static final ArchRule entitiesArePackagePrivate =
            classes().that().areAnnotatedWith(Entity.class)
                    .should().notBePublic()
                    .because("other features must reference an entity by its id, not by its type");

    // Rule 3: no feature depends on a feature that depends back on it.
    // If two features need each other, one of them publishes an event instead.
    @ArchTest
    static final ArchRule noCyclesBetweenFeatures =
            slices().matching(ROOT + ".(*)..")
                    .should().beFreeOfCycles();

    // Rule 4: common is shared by every feature, so it must not know any of them
    @ArchTest
    static final ArchRule commonDependsOnNoFeature =
            classes().that().resideInAPackage(ROOT + ".common..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(ROOT + ".common..", "java..", "jakarta..", "org.springframework..");
}
