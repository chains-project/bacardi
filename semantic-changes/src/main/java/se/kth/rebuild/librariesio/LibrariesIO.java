package se.kth.rebuild.librariesio;


import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.Util.JsonUtils;
import se.kth.model.UpdatedDependency;
import se.kth.rebuild.librariesio.model.Project;

import java.io.IOException;

public class LibrariesIO {

    private static final Logger logger = LoggerFactory.getLogger(LibrariesIO.class);

    private static final String BASE_URL = "https://libraries.io/api/Maven";

    private final String apiKey;
    private final OkHttpClient httpClient;

    public LibrariesIO(final String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient();
    }

    public Project getProject(UpdatedDependency updatedDependency) {
        Response response = this.request(updatedDependency.dependencyGroupID, updatedDependency.dependencyArtifactID);
        if (response.isSuccessful()) {
            try {
                return JsonUtils.parseString(response.body().string(), Project.class);
            } catch (IOException e) {
                throw new RuntimeException("Didn't work for " + updatedDependency.dependencyGroupID + ":" + updatedDependency.dependencyArtifactID + "!", e);
            }
        }
        return null;
    }


    private Response request(String groupName, String artifactName) {
        HttpUrl httpUrl = HttpUrl.parse(BASE_URL).newBuilder()
                .addPathSegment(groupName + ":" + artifactName)
                .addQueryParameter("api_key", apiKey)
                .build();

        Request request = new Request.Builder()
                .url(httpUrl)
                .build();

        try {
            Response response = httpClient.newCall(request).execute();
            return response;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
