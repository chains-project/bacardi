package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.Util.BreakingUpdateProvider;
import se.kth.model.BreakingUpdate;
import se.kth.models.FailureCategory;
import se.kth.utils.Config;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Pipeline {

    private static final Logger logger = LoggerFactory.getLogger(Pipeline.class);

    private List<PipelineComponent> components;
    private List<BreakingUpdate> breakingUpdates;

    public Pipeline() {
        this.components = new LinkedList<>();
        this.breakingUpdates =
                BreakingUpdateProvider.getBreakingUpdatesFromResourcesByCategory(Config.getBumpDir().toString(),
                        FailureCategory.TEST_FAILURE);
    }

    public Pipeline with(PipelineComponent component) {
        this.components.add(component);
        return this;
    }

    public void run() {
        for (PipelineComponent component : components) {
            logger.info("Starting pipeline step " + component.getClass());

            component.ensureOutputDirsExist();

            AtomicInteger failed = new AtomicInteger();
            try (ExecutorService executorService = Executors.newFixedThreadPool(50)) {
                for (BreakingUpdate update : this.breakingUpdates) {
                    executorService.submit(() -> {
                        try {
                            component.execute(update);
                        } catch (Exception e) {
                            failed.getAndIncrement();
                        }
                    });
                }
                executorService.shutdown();
                executorService.awaitTermination(1, TimeUnit.HOURS);

                System.out.println("failed: " + failed);
                component.finish();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }

            logger.info("Finished pipeline step " + component.getClass());
        }
    }
}
