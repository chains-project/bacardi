package se.kth.instrumentation;

import java.util.List;

/**
 * Builds the {@code hostPath:containerPath} bind-mount spec Apptainer's {@code --bind}
 * flag expects, replacing Docker's {@code HostConfig}/{@code Mount} builder API.
 */
public class HostConfigBuilder {

    private final String agentPath;

    public HostConfigBuilder(String agentPath) {
        this.agentPath = agentPath;
    }

    public List<String> build() {
        return List.of("%s:/instrumentation/semantic-agent-1.0-SNAPSHOT.jar".formatted(agentPath));
    }
}
