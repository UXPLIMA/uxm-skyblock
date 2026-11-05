package com.uxplima.uxmskyblock.bukkit.bootstrap;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;

import com.uxplima.uxmlib.storage.sql.StorageDrivers;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Fetches the database drivers a server does not carry, before the plugin starts.
 *
 * <p>A server ships SQLite and MySQL's driver and no other, so an operator who names MariaDB or PostgreSQL would
 * otherwise get a plugin that cannot connect. The drivers are fetched once into the server's libraries folder
 * rather than shaded, so every plugin that offers them shares one copy on disk.
 */
public final class SkyblockLoader implements PluginLoader {

    @Override
    public void classloader(PluginClasspathBuilder classpath) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addRepository(
                new RemoteRepository.Builder("central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR)
                        .build());
        for (String driver : StorageDrivers.coordinates()) {
            resolver.addDependency(new Dependency(new DefaultArtifact(driver), null));
        }
        classpath.addLibrary(resolver);
    }
}
