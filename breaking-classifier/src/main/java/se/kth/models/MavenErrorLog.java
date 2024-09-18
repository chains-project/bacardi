package se.kth.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@lombok.Getter
@lombok.Setter
public class MavenErrorLog {

    Map<String, Set<ErrorInfo>> errorInfo;


    public MavenErrorLog() {
        this.errorInfo = new HashMap<>();
    }

    public void addErrorInfo(String currentPath, ErrorInfo errorInfo) {
        if (this.errorInfo.containsKey(currentPath)) {
            for (ErrorInfo error : this.errorInfo.get(currentPath)) {
                if (error.getClientLinePosition().equals(errorInfo.getClientLinePosition())
                        && error.getClientFilePath().equals(errorInfo.getClientFilePath())
                ) {
                    return;
                }
            }
            this.errorInfo.get(currentPath).add(errorInfo);
        } else {
            Set<ErrorInfo> errors = this.errorInfo.computeIfAbsent(currentPath, k -> new HashSet<>());
            errors.add(errorInfo);
        }
    }

    @Override
    public String toString() {
        return "MavenErrorLog{" +
                "errorInfo=" + errorInfo +
                '}';
    }
}
