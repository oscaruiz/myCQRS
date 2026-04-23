package com.oscaruiz.mycqrs.core;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
        packages = "com.oscaruiz.mycqrs.core",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    @ArchTest
    static final ArchRule contractsDoNotDependOnSpring =
            noClasses()
                    .that().resideInAPackage("..core.contracts..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule dddDoesNotDependOnSpring =
            noClasses()
                    .that().resideInAPackage("..core.ddd..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule idempotencyDoesNotDependOnSpring =
            noClasses()
                    .that().resideInAPackage("..core.idempotency..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule observabilityDoesNotDependOnSpring =
            noClasses()
                    .that().resideInAPackage("..core.infrastructure.observability..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule coreSlicesAreFreeOfCycles =
            slices().matching("..core.(*)..").should().beFreeOfCycles();
}
