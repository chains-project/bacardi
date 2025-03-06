/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.security.policy;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FsStorage;
import com.artipie.asto.memory.MemoryStorage;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.Storages;
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
            final Storage storage;
            if ("fs".equals(sub.string("type"))) {
                storage = new FsStorage(sub.string("path"));
            } else {
                storage = new MemoryStorage();
            }
            return new YamlPolicy(new BlockingStorage(storage));
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
    }
}