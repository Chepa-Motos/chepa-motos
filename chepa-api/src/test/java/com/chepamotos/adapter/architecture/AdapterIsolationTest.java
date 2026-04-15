package com.chepamotos.adapter.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.chepamotos")
class AdapterIsolationTest {

    @ArchTest
    static final ArchRule adapter_must_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage("com.chepamotos.adapter..")
            .should().dependOnClassesThat().resideInAnyPackage("com.chepamotos.infrastructure..");
}
