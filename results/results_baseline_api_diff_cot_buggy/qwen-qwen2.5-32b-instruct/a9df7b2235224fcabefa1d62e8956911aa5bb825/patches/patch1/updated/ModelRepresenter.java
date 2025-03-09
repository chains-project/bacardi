/**
 * Copyright (c) 2012 to original author or authors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.sonatype.maven.polyglot.yaml;

import org.apache.maven.model.Contributor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import java.beans.IntrospectionException;
import java.util.*;

import static java.lang.String.format;

/**
 * YAML model representer.
 *
 * @author jvanzyl
 * @author bentmann
 * @since 0.7
 */
class ModelRepresenter extends Representer {
  public ModelRepresenter() {
    this.representers.put(Xpp3Dom.class, new RepresentXpp3Dom());
    Represent stringRepresenter = this.representers.get(String.class);
    this.representers.put(Boolean.class, stringRepresenter);
    this.multiRepresenters.put(Number.class, stringRepresenter);
    this.multiRepresenters.put(Date.class, stringRepresenter);
    this.multiRepresenters.put(Enum.class, stringRepresenter);
    this.multiRepresenters.put(Calendar.class, stringRepresenter);
  }

  protected Set<Property> getProperties(Class<? extends Object> type) {
    try {
      if (type.isAssignableFrom(Model.class)) {
        return sortTypeWithOrder(type, ORDER_MODEL);
      } else if (type.isAssignableFrom(Developer.class)) {
        return sortTypeWithOrder(type, ORDER_DEVELOPER);
      } else if (type.isAssignableFrom(Contributor.class)) {
        return sortTypeWithOrder(type, ORDER_CONTRIBUTOR);
      } else if (type.isAssignableFrom(Dependency.class)) {
        return sortTypeWithOrder(type, ORDER_DEPENDENCY);
      } else if (type.isAssignableFrom(Plugin.class)) {
        return sortTypeWithOrder(type, ORDER_PLUGIN);
      } else {
        return PropertyUtils.getProperties(type, BeanAccess.FIELD);
      }
    } catch (IntrospectionException e) {
      throw new RuntimeException(e);
    }
  }

  private Set<Property> sortTypeWithOrder(Class<? extends Object> type, List<String> order)
          throws IntrospectionException {
    Set<Property> standard = PropertyUtils.getProperties(type, BeanAccess.FIELD);
    Set<Property> sorted = new TreeSet<>(new ModelPropertyComparator(order));
    sorted.addAll(standard);
    return sorted;
  }

  private class ModelPropertyComparator implements Comparator<Property> {
    private List<String> names;

    public ModelPropertyComparator(List<String> names) {
      this.names = names;
    }

    public int compare(Property o1, Property o2) {
      // important go first
      for (String name : names) {
        int c = compareByName(o1, o2, name);
        if (c != 0) {
          return c;
        }
      }
      // all the rest
      return o1.compareTo(o2);
    }

    private int compareByName(Property o1, Property o2, String name) {
      if (o1.getName().equals(name)) {
        return -1;
      } else if (o2.getName().equals(name)) {
        return 1;
      }
      return 0; // compare further
    }
  }

  private class RepresentXpp3Dom implements Represent {
    private static final String ATTRIBUTE_PREFIX = "attr/";

    public Node representData(Object data) {
      return representMapping(Tag.MAP, toMap((Xpp3Dom) data), null);
    }

    private Map<String, Object> toMap(Xpp3Dom node) {
      Map<String, Object> map = new LinkedHashMap<>();

      int n = node.getChildCount();
      for (int i = 0; i < n; i++) {
        Xpp3Dom child = node.getChild(i);

        String singularName = null;
        int childNameLength = child.getName().length();
        if ("reportPlugins".equals(child.getName())) {
          singularName = "plugin";
        } else if (childNameLength > 3 && child.getName().endsWith("ies")) {
          singularName = child.getName().substring(0, childNameLength - 3);
        } else if (childNameLength > 1 && child.getName().endsWith("s")) {
          singularName = child.getName().substring(0, childNameLength - 1);
        }

        Object childValue = child.getValue();
        if (childValue == null) {
          childValue = toMap(child);
        }
        map.put(child.getName(), childValue);
      }

      for (String attrName : node.getAttributeNames()) {
        map.put(ATTRIBUTE_PREFIX + attrName, node.getAttribute(attrName));
      }

      return map;
    }
  }

  // Model elements order {
  //TODO move to polyglot-common, or to org.apache.maven:maven-model
  private static List<String> ORDER_MODEL = new ArrayList<>(Arrays.asList(
          "modelEncoding",
          "modelVersion",
          "parent",
          "groupId",
          "artifactId",
          "version",
          "packaging",

          "name",
          "description",
          "url",
          "inceptionYear",
          "organization",
          "licenses",
          "developers",
          "contributers",
          "mailingLists",
          "scm",
          "issueManagement",
          "ciManagement",

          "properties",
          "prerequisites",
          "modules",
          "dependencyManagement",
          "dependencies",
          "distributionManagement",
          //"repositories",
          //"pluginRepositories",
          "build",
          "profiles",
          "reporting"
  ));
  private static List<String> ORDER_DEVELOPER = new ArrayList<>(Arrays.asList(
          "name", "id", "email"));
  private static List<String> ORDER_CONTRIBUTOR = new ArrayList<>(Arrays.asList(
          "name", "id", "email"));
  private static List<String> ORDER_DEPENDENCY = new ArrayList<>(Arrays.asList(
          "groupId", "artifactId", "version", "type", "classifier", "scope"));
  private static List<String> ORDER_PLUGIN = new ArrayList<>(Arrays.asList(
          "groupId", "artifactId", "version", "inherited", "extensions", "configuration"));
  //}
}