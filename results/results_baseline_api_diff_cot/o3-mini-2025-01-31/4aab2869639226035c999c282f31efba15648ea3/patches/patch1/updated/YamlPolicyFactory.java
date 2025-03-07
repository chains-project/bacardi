package com.artipie.security.policy;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorageFactory;
import com.artipie.asto.factory.Config;
import com.artipie.asto.Storage;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Policy factory to create {@link YamlPolicy}. Yaml policy is read from storage, and it's required
 * to describe this storage in the configuration. Configuration format is the following:
 *
 * policy:
 *   type: yaml_policy
 *   storage:
 *     type: fs
 *     path: /some/path
 *
 * The storage itself is expected to have yaml files with permissions in the following structure:
 *
 * ..
 * ├── roles.yaml
 * ├── users
 * │   ├── david.yaml
 * │   ├── jane.yaml
 * │   ├── ...
 *
 * @since 1.2
 */
@ArtipiePolicyFactory("yaml_policy")
public final class YamlPolicyFactory implements PolicyFactory {

    @Override
    public Policy<?> getPolicy(final PolicyConfig config) {
        final PolicyConfig sub = config.config("storage");
        try {
            final String type = sub.string("type");
            final YamlMapping yaml = Yaml.createYamlInput(sub.toString()).readYamlMapping();
            final Storage storage;
            // Here we support only filesystem storage ("fs") per the configuration example.
            // In the new API, the generic Storages helper was removed, so we instantiate
            // the appropriate storage factory based on the type.
            if ("fs".equalsIgnoreCase(type)) {
                storage = new FileStorageFactory().newStorage(new YamlStorageConfig(yaml));
            } else {
                throw new IllegalArgumentException("Unsupported storage type: " + type);
            }
            return new YamlPolicy(new BlockingStorage(storage));
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
    }

    /**
     * A simple adapter that implements the new {@link Config} interface by wrapping a {@link YamlMapping}.
     * This allows the storage factories (such as {@link FileStorageFactory}) to read their configuration
     * from the YAML file.
     */
    private static final class YamlStorageConfig implements Config {
        /**
         * Underlying YAML configuration.
         */
        private final YamlMapping mapping;

        /**
         * Ctor.
         *
         * @param mapping YAML mapping with storage configuration.
         */
        YamlStorageConfig(final YamlMapping mapping) {
            this.mapping = mapping;
        }

        @Override
        public String string(final String key) {
            return this.mapping.string(key);
        }

        // If the new Config interface in the dependency declares additional methods,
        // they should be implemented here accordingly.
    }
}