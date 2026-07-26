package com.jarl.seatforge.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ModularMonolithArchitectureTest {

    private static final String ROOT = "com.jarl.seatforge";
    private static final Set<String> MODULES = Set.of(
            "identity", "events", "inventory", "orders", "payments",
            "notifications", "audit", "shared"
    );

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importProductionClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    @Test
    void t04_domain_is_independent_from_frameworks_and_infrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "javax.persistence..",
                        "org.hibernate..",
                        "jakarta.servlet..",
                        "javax.servlet..",
                        "java.net.http..",
                        "..infrastructure.."
                )
                .because("el dominio debe seguir siendo Java puro y apuntar hacia adentro");

        rule.check(productionClasses);
    }

    @Test
    void application_does_not_depend_on_infrastructure() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("los adaptadores implementan puertos de aplicación, no al contrario")
                .check(productionClasses);
    }

    @Test
    void t05_modules_only_access_the_public_application_api_of_other_modules() {
        classes()
                .that().resideInAnyPackage(MODULES.stream()
                        .map(module -> ROOT + "." + module + "..")
                        .toArray(String[]::new))
                .should(onlyAccessPublicApiOfOtherModules())
                .because("un módulo no debe acceder al dominio, puertos de salida, repositorios ni adaptadores ajenos")
                .check(productionClasses);
    }

    @Test
    void modules_are_free_of_cycles() {
        slices()
                .matching(ROOT + ".(*)..")
                .should().beFreeOfCycles()
                .because("los límites modulares deben permitir una extracción futura")
                .check(productionClasses);
    }

    @Test
    void packages_scream_business_capabilities_instead_of_technical_groups() {
        noClasses()
                .should().resideInAnyPackage(
                        "..controller..", "..controllers..",
                        "..service..", "..services..",
                        "..repository..", "..repositories.."
                )
                .because("los paquetes deben expresar capacidades; web y persistence son adaptadores dentro de infrastructure")
                .check(productionClasses);
    }

    private static ArchCondition<JavaClass> onlyAccessPublicApiOfOtherModules() {
        return new ArchCondition<>("acceder a otros módulos únicamente mediante su API pública") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                Optional<String> originModule = moduleOf(origin.getPackageName());
                if (originModule.isEmpty()) {
                    return;
                }

                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    Optional<String> targetModule = moduleOf(target.getPackageName());
                    if (targetModule.isEmpty() || targetModule.equals(originModule)) {
                        continue;
                    }

                    if (!isAllowedPublicTarget(target.getPackageName(), targetModule.orElseThrow())) {
                        events.add(SimpleConditionEvent.violated(origin, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static boolean isAllowedPublicTarget(String packageName, String targetModule) {
        String moduleRoot = ROOT + "." + targetModule;
        List<String> publicPackages = List.of(moduleRoot + ".application.port.in");

        if (publicPackages.stream().anyMatch(api -> isPackageOrChild(packageName, api))) {
            return true;
        }

        return targetModule.equals("shared")
                && isPackageOrChild(packageName, moduleRoot + ".domain");
    }

    private static Optional<String> moduleOf(String packageName) {
        String prefix = ROOT + ".";
        if (!packageName.startsWith(prefix)) {
            return Optional.empty();
        }

        String remainder = packageName.substring(prefix.length());
        String candidate = remainder.contains(".")
                ? remainder.substring(0, remainder.indexOf('.'))
                : remainder;
        return MODULES.contains(candidate) ? Optional.of(candidate) : Optional.empty();
    }

    private static boolean isPackageOrChild(String actual, String expected) {
        return actual.equals(expected) || actual.startsWith(expected + ".");
    }
}
