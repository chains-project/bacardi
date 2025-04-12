package rq3;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@lombok.ToString
public class CauseInformation {
    private int numberByLLM;
    private int numberByBreakingChanges;
    private List<BreakingInfo> byBreakingChanges;
    private List<BreakingInfo> byLLMs;


    public CauseInformation(int byLLM, int byBreakingChange, List<BreakingInfo> byBreakingChanges, List<BreakingInfo> byLLMs) {
        this.numberByLLM = byLLM;
        this.numberByBreakingChanges = byBreakingChange;
        this.byBreakingChanges = byBreakingChanges;
        this.byLLMs = byLLMs;
    }

    public CauseInformation(List<BreakingInfo> byBreakingChanges, List<BreakingInfo> byLLMs) {
        this.byBreakingChanges = byBreakingChanges;
        this.byLLMs = byLLMs;
        this.numberByLLM = byLLMs.size();
        this.numberByBreakingChanges = byBreakingChanges.size();
    }

    public CauseInformation() {
    }
}
