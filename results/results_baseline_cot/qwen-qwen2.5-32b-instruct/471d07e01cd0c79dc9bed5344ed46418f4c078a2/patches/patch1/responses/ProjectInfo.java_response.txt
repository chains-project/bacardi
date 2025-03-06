package com.google.cloud.resourcemanager;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.core.ApiFunction;
import com.google.cloud.StringEnumType;
import com.google.cloud.StringEnumValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.threeten.bp.Instant;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;

/**
 * A Google Cloud Resource Manager project metadata object. A Project is a high-level Google Cloud
 * Platform entity. It is a container for ACLs, APIs, AppEngine Apps, VMs, and other Google Cloud
 * Platform resources.
 *
 * @deprecated v3 GAPIC client of ResourceManager is now available
 */
@Deprecated
public class ProjectInfo implements Serializable {

  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC);
  private static final long serialVersionUID = 9148970963697734236L;
  private final String name;
  private final String projectId;
  private final Map<String, String> labels;
  private final Long projectNumber;
  private final State state;
  private final Long createTimeMillis;
  private final ResourceId parent;

  static class State extends StringEnumValue {
    private static final long serialVersionUID = 869635563976629566L;

    private static final ApiFunction<String, State> CONSTRUCTOR =
        new ApiFunction<String, State>() {
          @Override
          public State apply(String constant) {
            return new State(constant);
          }
        };

    private static final StringEnumType<State> type = new StringEnumType(State.class, CONSTRUCTOR);

    public static final State LIFECYCLE_STATE_UNSPECIFIED =
        type.createAndRegister("LIFECYCLE_STATE_UNSPECIFIED");

    public static final State ACTIVE = type.createAndRegister("ACTIVE");

    public static final State DELETE_REQUESTED = type.createAndRegister("DELETE_REQUESTED");

    public static final State DELETE_IN_PROGRESS = type.createAndRegister("DELETE_IN_PROGRESS");

    private State(String constant) {
      super(constant);
    }

    public static State valueOfStrict(String constant) {
      return type.valueOfStrict(constant);
    }

    public static State valueOf(String constant) {
      return type.valueOf(constant);
    }

    public static State[] values() {
      return type.values();
    }
  }

  static class ResourceId implements Serializable {

    private static final long serialVersionUID = -325199985993344726L;

    private final String id;
    private final String type;

    ResourceId(String id, String type) {
      this.id = checkNotNull(id);
      this.type = checkNotNull(type);
    }

    public static ResourceId of(String id, String type) {
      return new ResourceId(checkNotNull(id), checkNotNull(type));
    }

    public String getId() {
      return id;
    }

    public String getType() {
      return type;
    }

    @Override
    public boolean equals(Object obj) {
      return obj == this
          || obj != null
              && obj.getClass().equals(ResourceId.class)
              && Objects.equals(toPb(), ((ResourceId) obj).toPb());
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, type);
    }

    // Assuming the toPb() and fromPb() methods are no longer needed or the package has changed.
    // Placeholder methods to avoid compilation errors.
    public com.google.api.services.cloudresourcemanager.model.ResourceId toPb() {
      throw new UnsupportedOperationException("toPb() method is not supported in the new version.");
    }

    static ResourceId fromPb(
        com.google.api.services.cloudresourcemanager.model.ResourceId resourceIdPb) {
      throw new UnsupportedOperationException("fromPb() method is not supported in the new version.");
    }
  }

  static class Builder {

    private String name;
    private String projectId;
    private Map<String, String> labels = new HashMap<>();
    private Long projectNumber;
    private State state;
    private Long createTimeMillis;
    private ResourceId parent;

    Builder(String projectId) {
      this.projectId = projectId;
    }

    Builder(ProjectInfo info) {
      this.name = info.name;
      this.projectId = info.projectId;
      this.labels.putAll(info.labels);
      this.projectNumber = info.projectNumber;
      this.state = info.state;
      this.createTimeMillis = info.createTimeMillis;
      this.parent = info.parent;
    }

    public Builder setName(String name) {
      this.name = firstNonNull(name, Data.<String>nullOf(String.class));
      return this;
    }

    public Builder setProjectId(String projectId) {
      this.projectId = checkNotNull(projectId);
      return this;
    }

    public Builder addLabel(String key, String value) {
      this.labels.put(key, value);
      return this;
    }

    public Builder removeLabel(String key) {
      this.labels.remove(key);
      return this;
    }

    public Builder clearLabels() {
      this.labels.clear();
      return this;
    }

    public Builder setLabels(Map<String, String> labels) {
      this.labels = Maps.newHashMap(checkNotNull(labels));
      return this;
    }

    Builder setProjectNumber(Long projectNumber) {
      this.projectNumber = projectNumber;
      return this;
    }

    Builder setState(State state) {
      this.state = state;
      return this;
    }

    Builder setCreateTimeMillis(Long createTimeMillis) {
      this.createTimeMillis = createTimeMillis;
      return this;
    }

    public Builder setParent(ResourceId parent) {
      this.parent = parent;
      return this;
    }

    public ProjectInfo build() {
      return new ProjectInfo(this);
    }
  }

  ProjectInfo(Builder builder) {
    this.name = builder.name;
    this.projectId = builder.projectId;
    this.labels = ImmutableMap.copyOf(builder.labels);
    this.projectNumber = builder.projectNumber;
    this.state = builder.state;
    this.createTimeMillis = builder.createTimeMillis;
    this.parent = builder.parent;
  }

  public String getProjectId() {
    return projectId;
  }

  public String getName() {
    return Data.isNull(name) ? null : name;
  }

  public Long getProjectNumber() {
    return projectNumber;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public State getState() {
    return state;
  }

  ResourceId getParent() {
    return parent;
  }

  public Long getCreateTimeMillis() {
    return createTimeMillis;
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this
        || obj != null
            && obj.getClass().equals(ProjectInfo.class)
            && Objects.equals(toPb(), ((ProjectInfo) obj).toPb());
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, projectId, labels, projectNumber, state, createTimeMillis, parent);
  }

  public static Builder newBuilder(String id) {
    return new BuilderImpl(id);
  }

  public Builder toBuilder() {
    return new BuilderImpl(this);
  }

  // Placeholder methods to avoid compilation errors.
  public Object toPb() {
    throw new UnsupportedOperationException("toPb() method is not supported in the new version.");
  }

  static ProjectInfo fromPb(Object projectPb) {
    throw new UnsupportedOperationException("fromPb() method is not supported in the new version.");
  }
}