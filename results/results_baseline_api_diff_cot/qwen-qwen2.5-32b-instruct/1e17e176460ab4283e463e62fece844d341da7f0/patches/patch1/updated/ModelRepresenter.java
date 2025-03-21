package org.sonatype.maven.polyglot.yaml;

import org.apache.maven.model.Contributor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import java.beans.IntrospectionException;
import java.util.*;

import static java.lang.String.format;

class ModelRepresenter extends Representer {
  public ModelRepresenter() {
    super(new DumperOptions());
    this.representers.put(Xpp3Dom.class, new RepresentXpp3Dom());
    Represent stringRepresenter = this.representers.get(String.class);
    this.representers.put(Boolean.class, stringRepresenter);
    this.multiRepresenters.put(Number.class, stringRepresenter);
    this.multiRepresenters.put(Date.class, stringRepresenter);
    this.multiRepresenters.put(Enum.class, stringRepresenter);
    this.multiRepresenters.put(Calendar.class, stringRepresenter);
  }

  protected NodeTuple representJavaBeanProperty(Object javaBean, Property property,
                                                Object propertyValue, Tag customTag) {
    if (property != null && property.getName().equals("pomFile")) {
      // "pomFile" is not a part of POM http://maven.apache.org/xsd/maven-4.0.0.xsd
      return null;
    }

    if (propertyValue == null) return null;
    if (propertyValue instanceof Map) {
      Map map = (Map) propertyValue;
      if (map.isEmpty()) return null;
    }
    if (propertyValue instanceof List) {
      List map = (List) propertyValue;
      if (map.isEmpty()) return null;
    }
    if (javaBean instanceof Dependency) {
      //skip optional if it is false
      if (skipBoolean(property, "optional", propertyValue, false)) return null;
      //skip type if it is jar
      if (skipString(property, "type", propertyValue, "jar")) return null;
    }
    if (javaBean instanceof Plugin) {
      //skip extensions if it is false
      if (skipBoolean(property, "extensions", propertyValue, false)) return null;
      //skip inherited if it is true
      if (skipBoolean(property, "inherited", propertyValue, true)) return null;
    }
    return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
  }

  private boolean skipString(Property property, String name, Object propertyValue, String value) {
    if (name.equals(property.getName())) {
      String v = (String) propertyValue;
      return (value.equals(v));
    }
    return false;
  }

  private boolean skipBoolean(Property property, String name, Object propertyValue, boolean value) {
    if (name.equals(property.getName())) {
      Boolean v = (Boolean) propertyValue;
      return (v.equals(value));
    }
    return false;
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
        String childName = child.getName();

        String singularName = null;
        int childNameLength = childName.length();
        if ("reportPlugins".equals(childName)) {
          singularName = "plugin";
        } else if (childNameLength > 3 && childName.endsWith("ies")) {
          singularName = childName.substring(0, childNameLength - 3);
        } else if (childNameLength > 1 && childName.endsWith("s")) {
          singularName = childName.substring(0, childNameLength - 1);
        }

        Object childValue = child.getValue();
        if (childValue == null) {
          childValue = toMap(child);
        }
        map.put(childName, childValue);
      }

      for (String attrName : node.getAttributeNames()) {
        map.put(ATTRIBUTE_PREFIX + attrName, node.getAttribute(attrName));
      }

      return map;
    }
  }

  // Model elements order {
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
        return super.getProperties(type, BeanAccess.FIELD);
      }
    } catch (IntrospectionException e) {
      throw new RuntimeException(e);
    }
  }

  private Set<Property> sortTypeWithOrder(Class<? extends Object> type, List<String> order) {
    Set<Property> standard = super.getProperties(type, BeanAccess.FIELD);
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
      for (String name : names) {
        int c = compareByName(o1, o2, name);
        if (c != 0) {
          return c;
        }
      }
      return o1.compareTo(o2);
    }

    private int compareByName(Property o1, Property o2, String name) {
      if (o1.getName().equals(name)) {
        return -1;
      } else if (o2.getName().equals(name)) {
        return 1;
      }
      return 0;
    }
  }
}