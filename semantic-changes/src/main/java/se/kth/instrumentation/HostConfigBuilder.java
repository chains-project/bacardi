package se.kth.instrumentation;

import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;

import java.util.List;

public class HostConfigBuilder {

    private final String agentPath;

    public HostConfigBuilder(String agentPath) {
        this.agentPath = agentPath;
    }

    public HostConfig build() {
        return HostConfig.newHostConfig()
                .withMounts(List.of(new Mount()
                        .withSource(agentPath.toString())
                        .withTarget("/instrumentation/semantic-agent-1.0-SNAPSHOT.jar")
                        .withType(MountType.BIND)));
    }
}
