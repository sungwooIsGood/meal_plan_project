package com.mealplan.mealplan.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 헥사고날 의존성 방향을 기계적으로 강제한다.
 * primary → application → domain ← secondary (모두 안쪽 domain을 향함).
 */
class HexagonalArchitectureTest {

    private static JavaClasses classes;

    private static final String BASE = "com.mealplan.mealplan";

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    void domain_should_not_depend_on_other_layers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE + ".domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        BASE + ".application..",
                        BASE + ".primary..",
                        BASE + ".secondary..");
        rule.check(classes);
    }

    @Test
    void application_should_not_depend_on_adapters() {
        // application은 primary/secondary(어댑터 구현체)에 의존하지 않는다.
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE + ".application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(BASE + ".primary..", BASE + ".secondary..");
        rule.check(classes);
    }

    @Test
    void primary_should_not_depend_on_secondary() {
        // 인바운드 어댑터는 아웃바운드 어댑터를 직접 참조하지 않는다 (application 경유).
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE + ".primary..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE + ".secondary..");
        rule.check(classes);
    }

    @Test
    void domain_should_not_use_spring_or_persistence_annotations() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE + ".domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "javax.persistence..");
        rule.check(classes);
    }
}
