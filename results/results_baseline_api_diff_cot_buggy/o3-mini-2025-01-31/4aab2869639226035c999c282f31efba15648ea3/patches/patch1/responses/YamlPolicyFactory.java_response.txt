package com.artipie.security.policy;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StorageFactory;
import java.io.IOException;
import java.io.UncheckedIOException;

@ArtipiePolicyFactory("yaml_policy")
public final class YamlPolicyFactory implements PolicyFactory {

    @Override
    public Policy<?> getPolicy(final PolicyConfig config) {
        final PolicyConfig sub = config.config("storage");
        try {
            final YamlMapping mapping = Yaml.createYamlInput(sub.toString()).readYamlMapping();
            final Config storageConfig = new YamlStorageConfig(mapping);
            return new YamlPolicy(
                new BlockingStorage(StorageFactory.newStorage(storageConfig))
            );
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
    }

    private static final class YamlStorageConfig implements Config {
        private final YamlMapping mapping;

        YamlStorageConfig(final YamlMapping mapping) {
            this.mapping = mapping;
        }

        @Override
        public String string(final String key) {
            return this.mapping.string(key);
        }
    }
}
