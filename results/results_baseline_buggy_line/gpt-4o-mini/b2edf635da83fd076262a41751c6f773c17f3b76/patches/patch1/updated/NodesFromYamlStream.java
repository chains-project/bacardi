package org.jclouds.byon.functions;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.propagate;
import static org.jclouds.util.Closeables2.closeQuietly;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.jclouds.byon.Node;
import org.jclouds.byon.domain.YamlNode;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;

@Singleton
public class NodesFromYamlStream implements Function<ByteSource, LoadingCache<String, Node>> {

   public static class Config {
      public List<YamlNode> nodes;
   }

   public LoadingCache<String, Node> apply(ByteSource source) {

      LoaderOptions loaderOptions = new LoaderOptions();
      Constructor constructor = new Constructor(Config.class, loaderOptions);

      TypeDescription nodeDesc = new TypeDescription(YamlNode.class);
      nodeDesc.putListPropertyType("tags", String.class);
      constructor.addTypeDescription(nodeDesc);

      TypeDescription configDesc = new TypeDescription(Config.class);
      configDesc.putListPropertyType("nodes", YamlNode.class);
      constructor.addTypeDescription(configDesc);
      Yaml yaml = new Yaml(constructor);
      Config config;
      InputStream in = null;
      try {
         in = source.openStream();
         config = (Config) yaml.load(in);
      } catch (IOException ioe) {
         throw propagate(ioe);
      } finally {
         closeQuietly(in);
      }
      checkState(config != null, "missing config: class");
      checkState(config.nodes != null, "missing nodes: collection");

      Map<String, Node> backingMap = Maps.uniqueIndex(Iterables.transform(config.nodes, YamlNode.toNode),
            new Function<Node, String>() {
               public String apply(Node node) {
                  return node.getId();
               }
            });
      LoadingCache<String, Node> cache = CacheBuilder.newBuilder().build(CacheLoader.from(Functions.forMap(backingMap)));
      for (String node : backingMap.keySet())
         cache.getUnchecked(node);
      return cache;
   }
}