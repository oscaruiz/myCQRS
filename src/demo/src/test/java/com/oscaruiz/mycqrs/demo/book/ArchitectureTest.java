package com.oscaruiz.mycqrs.demo.book;

import com.oscaruiz.mycqrs.core.contracts.command.CommandHandler;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
        packages = "com.oscaruiz.mycqrs.demo",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    // withOptionalLayers(true) allows declared layers to be empty.
    // Today it applies only to the implicit 'domain service' layer:
    // domain logic lives in the aggregate, we have no domain services.
    // Trade-off to keep in mind: this flag also tolerates application
    // or any adapter being empty — if a real layer gets deleted by
    // mistake, this rule would still pass. Acceptable today because
    // the onion shape is small and obvious under review.
    @ArchTest
    static final ArchRule bookFollowsOnionArchitecture =
            onionArchitecture()
                    .domainModels("..demo.book.domain..")
                    .applicationServices("..demo.book.application..")
                    .adapter("jpa",     "..demo.book.infrastructure.jpa..")
                    .adapter("mongo",   "..demo.book.infrastructure.mongo..")
                    .adapter("api",     "..demo.book.infrastructure.api..")
                    .adapter("outbox",  "..demo.book.infrastructure.outbox..")
                    .adapter("service", "..demo.book.infrastructure.service..")
                    .withOptionalLayers(true);

    // Cross-bounded-context collaborators (Book's infrastructure adapters
    // calling Author's published ports) are legitimate but confuse ArchUnit's
    // onion model, which otherwise treats every incoming dependency as a
    // violation. We ignore only the PUBLISHED-PORT edges — Book reaching
    // into Author's domain (AuthorRepository, AuthorAggregate) and into
    // Author's application.query ports (AuthorReadModelRepository,
    // AuthorResponse). Any other edge from Book into Author (e.g., into
    // {@code author.infrastructure.*} or {@code author.application.command.*})
    // will surface as an onion violation here.
    @ArchTest
    static final ArchRule authorFollowsOnionArchitecture =
            onionArchitecture()
                    .domainModels("..demo.author.domain..")
                    .applicationServices("..demo.author.application..")
                    .adapter("jpa",    "..demo.author.infrastructure.jpa..")
                    .adapter("mongo",  "..demo.author.infrastructure.mongo..")
                    .adapter("api",    "..demo.author.infrastructure.api..")
                    .adapter("outbox", "..demo.author.infrastructure.outbox..")
                    .withOptionalLayers(true)
                    .ignoreDependency(
                            resideInAPackage("..demo.book.."),
                            resideInAnyPackage(
                                    "..demo.author.domain..",
                                    "..demo.author.application.query.."
                            )
                    );

    // redundante con la regla onion; mantenida como assertion explícita —
    // un fallo aquí apunta al boundary roto sin parsing del mensaje de onion.
    @ArchTest
    static final ArchRule bookApplicationDoesNotDependOnInfrastructure =
            noClasses()
                    .that().resideInAPackage("..demo.book.application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..demo.book.infrastructure.jpa..",
                            "..demo.book.infrastructure.mongo..",
                            "..demo.book.infrastructure.api..",
                            "..demo.book.infrastructure.outbox.."
                    );

    @ArchTest
    static final ArchRule authorApplicationDoesNotDependOnInfrastructure =
            noClasses()
                    .that().resideInAPackage("..demo.author.application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..demo.author.infrastructure.jpa..",
                            "..demo.author.infrastructure.mongo..",
                            "..demo.author.infrastructure.api..",
                            "..demo.author.infrastructure.outbox.."
                    );

    // Book's domain may only reach into Author's bounded context through
    // domain-level packages (the AuthorRepository port and AuthorAggregate
    // type) — never through Author's application services or adapters. This
    // keeps cross-aggregate references narrow: a future Book invariant that
    // needs to consult an Author must go through AuthorRepository, not
    // through a query handler, a projection, or any JPA/Mongo plumbing.
    @ArchTest
    static final ArchRule bookDomainOnlyTouchesAuthorViaDomainPort =
            noClasses()
                    .that().resideInAPackage("..demo.book.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..demo.author.application..",
                            "..demo.author.infrastructure.."
                    );

    // Book's infrastructure may depend on Author's domain ports (e.g., the
    // RepositoryAuthorExistenceChecker adapter consults AuthorRepository) and
    // on Author's application query ports (e.g., Mongo projections read
    // AuthorReadModelRepository to denormalize embedded summaries). It must
    // NOT reach into Author's infrastructure: doing so would couple the two
    // adapters' internals and defeat the query-port abstraction.
    @ArchTest
    static final ArchRule bookInfrastructureDoesNotDependOnAuthorInfrastructure =
            noClasses()
                    .that().resideInAPackage("..demo.book.infrastructure..")
                    .should().dependOnClassesThat().resideInAPackage(
                            "..demo.author.infrastructure.."
                    );

    // Symmetric guard: Author's infrastructure has no business touching
    // Book's infrastructure either. No current use case requires it; this
    // rule reserves the boundary.
    @ArchTest
    static final ArchRule authorInfrastructureDoesNotDependOnBookInfrastructure =
            noClasses()
                    .that().resideInAPackage("..demo.author.infrastructure..")
                    .should().dependOnClassesThat().resideInAPackage(
                            "..demo.book.infrastructure.."
                    );

    private static final ArchCondition<JavaClass> notDependOnAnotherCommandHandler =
            new ArchCondition<JavaClass>("not depend on another CommandHandler implementation") {
                @Override
                public void check(JavaClass source, ConditionEvents events) {
                    for (Dependency dep : source.getDirectDependenciesFromSelf()) {
                        JavaClass target = dep.getTargetClass();
                        if (target.isAssignableTo(CommandHandler.class)
                                && !target.isEquivalentTo(CommandHandler.class)
                                && !target.equals(source)) {
                            events.add(SimpleConditionEvent.violated(
                                    source,
                                    source.getFullName() + " depends on another CommandHandler "
                                            + target.getFullName() + " (" + dep.getDescription() + ")"));
                        }
                    }
                }
            };

    // A handler must not directly depend on another handler.
    // The predicate excludes three things intentionally:
    //   1. Self-reference — a handler trivially references its own
    //      class via method signatures and fields.
    //   2. The CommandHandler interface itself — every handler depends
    //      on the contract it implements; that is not "another handler".
    //   3. Anything not assignable to CommandHandler — irrelevant.
    // What remains: concrete OTHER handler implementations. Those are
    // forbidden as direct dependencies. Coordination between commands
    // must go through the CommandBus, which is a runtime dispatch
    // mechanism and does not appear as a class-level dependency.
    //
    // The rule applies across bounded contexts: a Book handler cannot
    // reference an Author handler class, and vice versa.
    @ArchTest
    static final ArchRule commandHandlersDoNotDependOnOtherCommandHandlers =
            classes()
                    .that().implement(CommandHandler.class)
                    .should(notDependOnAnotherCommandHandler);

    @ArchTest
    static final ArchRule bookSlicesAreFreeOfCycles =
            slices().matching("..demo.book.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule authorSlicesAreFreeOfCycles =
            slices().matching("..demo.author.(*)..").should().beFreeOfCycles();
}
