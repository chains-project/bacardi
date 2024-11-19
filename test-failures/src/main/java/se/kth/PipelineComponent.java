package se.kth;

import lombok.Getter;
import se.kth.model.BreakingUpdate;

@Getter
public abstract class PipelineComponent {

    public void ensureOutputDirsExist() {
    }

    public abstract void execute(BreakingUpdate breakingUpdate);

    public void finish() {
    }
}
