package se.kth;

import Util.BreakingUpdateProvider;
import com.github.dockerjava.api.command.CreateContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.model.BreakingUpdate;
import se.kth.models.FailureCategory;

import java.nio.file.Path;
import java.util.List;

import static Util.Constants.BENCHMARK_PATH;

public class Bump {

    private static final Logger log = LoggerFactory.getLogger(Bump.class);

    private final static String LOCAL_PATH_M2 = "/Users/frank/Documents/Work/PHD/bacardi/M2";


    public static void main(String[] args) {


        log.info("");
        log.info("*********************************************");
        log.info("Bump analysis started");
        log.info("*********************************************");
        log.info("");


        //filtering breaking dependency updates
        log.info("Filtering breaking dependency updates");
        log.info("");


        List<BreakingUpdate> breaking = BreakingUpdateProvider.getBreakingUpdatesFromResourcesByCategory(BENCHMARK_PATH, FailureCategory.COMPILATION_FAILURE);

        log.info("Breaking dependency updates: {}", breaking.size());
        log.info("");
        breaking.forEach(e -> log.info("{}", e.breakingCommit));
        log.info("");


        DockerBuild dockerBuild = new DockerBuild();
        // identify breaking updates and download image and copy the project

        breaking.stream()
                .filter(e -> e.breakingCommit.equals("c8da6c3c823d745bb37b072a4a33b6342a86dcd9"))
                .forEach(e -> getProjectData(e, dockerBuild));

    }


    private static void getProjectData(BreakingUpdate breakingUpdate, DockerBuild dockerBuild) {


        try {
            String imageId = breakingUpdate.breakingUpdateReproductionCommand.replace("docker run ", "");

            String[] entrypoint = new String[]{"/bin/sh"};

            //pull image
            dockerBuild.ensureBaseMavenImageExists(imageId);

            CreateContainerResponse container = dockerBuild.startContainerEntryPoint(imageId, entrypoint);

            // Copy m2 folder to local path in the breaking commit folder
            Path localPath = Path.of("%s/%s".formatted(LOCAL_PATH_M2, breakingUpdate.breakingCommit));
            dockerBuild.copyM2FolderToLocalPath(container.getId(), localPath);


        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


}
