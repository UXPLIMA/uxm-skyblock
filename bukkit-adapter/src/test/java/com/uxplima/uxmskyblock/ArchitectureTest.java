package com.uxplima.uxmskyblock;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The APIs nothing here may use, and the direction the modules depend in. The modules keep the core free of
 * server types at compile time; this keeps the adapters from reaching back into the plugin's own wiring.
 */
@AnalyzeClasses(packages = "com.uxplima.uxmskyblock", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule nothingUsesTheBukkitScheduler = noClasses()
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("org.bukkit.scheduler.BukkitScheduler")
            .as("work goes through SchedulerPort so Folia keeps working");

    @ArchTest
    static final ArchRule theCoreKnowsNoServer = noClasses()
            .that()
            .resideInAPackage("com.uxplima.uxmskyblock.core..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.bukkit..",
                    "io.papermc..",
                    "net.kyori..",
                    "com.uxplima.uxmskyblock.bukkit..",
                    "com.uxplima.uxmskyblock.persistence..")
            .as("core imports no server type and no adapter");
}
