package com.oscaruiz.mycqrs.core.infrastructure.micronaut;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.oscaruiz.mycqrs.core.infrastructure.micronaut",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    @ArchTest
    static final ArchRule micronautAdapterDoesNotDependOnSpring =
            noClasses()
                    .that().resideInAPackage("..core.infrastructure.micronaut..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..");
}
