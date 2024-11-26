package se.kth.model;

import java.util.Objects;

/**
 * This class is used to represent a dependency that is in conflict with the client's dependency.
 * To resolve the conflict, the client's dependency must be updated to a version that is compatible with the child dependency.
 * <p>To solve the conflict:
 * <ul>
 *     <li>Update the client's dependency to a version that is defined in the child dependency.</li>
 *     <li>Add a new dependency in the client's pom.xml file</li>
 * </ul>
 * </p>
 */
@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode
@lombok.ToString
public class ConflictedDependencies {

    // The dependency that is in conflict with the client dependency
    // In most of the cases, this is a transitive dependency of the client's dependency
    private DependencyTree child;

    // The client dependency that is in conflict with the child dependency
    // This is the dependency that is in the client's pom.xml file or as a transitive dependency from another dependency in the client's pom.xml file
    private DependencyTree clientDependency;

    public ConflictedDependencies(DependencyTree child, DependencyTree clientDependency) {
        this.child = Objects.requireNonNull(child, "The child dependency cannot be null");
        this.clientDependency = Objects.requireNonNull(clientDependency, "The client dependency cannot be null");
    }
}
