package com.oscaruiz.mycqrs.core.infrastructure.spring;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.oscaruiz.mycqrs.core.infrastructure.spring",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    @ArchTest
    static final ArchRule springAdapterDoesNotDependOnMicronaut =
            noClasses()
                    .that().resideInAPackage("..core.infrastructure.spring..")
                    .should().dependOnClassesThat().resideInAPackage("io.micronaut..");
}
