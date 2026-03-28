package com.chepamotos.domain.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.chepamotos")
class DomainIsolationTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_spring_or_outer_layers = noClasses()
            .that().resideInAPackage("com.chepamotos.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "com.chepamotos.adapter..",
                    "com.chepamotos.infrastructure.."
            );
}
