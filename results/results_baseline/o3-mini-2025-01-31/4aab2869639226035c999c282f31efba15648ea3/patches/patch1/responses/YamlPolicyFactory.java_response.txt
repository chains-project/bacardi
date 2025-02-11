/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.security.policy;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;

@ArtipiePolicyFactory("yaml_policy")
public final class YamlPolicyFactory implements PolicyFactory {

    @Override
    public Policy<?> getPolicy(final PolicyConfig config) {
        final PolicyConfig sub = config.config("storage");
        try {
            final YamlMapping mapping = Yaml.createYamlInput(sub.toString()).readYamlMapping();
            final Storage storage = newStorage(sub.string("type"), mapping);
            return new YamlPolicy(new BlockingStorage(storage));
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
    }

    private Storage newStorage(final String type, final YamlMapping mapping) {
        if ("fs".equalsIgnoreCase(type)) {
            return new FileStorage(Paths.get(mapping.string("path")));
        }
        throw new IllegalArgumentException("Unsupported storage type: " + type);
    }
}