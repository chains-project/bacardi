package org.jfrog.hudson.release.scm.perforce;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.server.IOptionsServer;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.p4.PerforceScm;
import org.jenkinsci.plugins.p4.client.ClientHelper;
import org.jenkinsci.plugins.p4.client.ConnectionFactory;
import org.jenkinsci.plugins.p4.client.ConnectionHelper;
import org.jfrog.build.vcs.perforce.PerforceClient;
import hudson.model.ItemGroup; // Added import for ItemGroup
import hudson.model.Item; // Added import for Item
import org.jenkinsci.plugins.p4.workspace.Workspace; // Added import for Workspace

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interacts with Perforce various release operations.
 * Using the updated perforce plugin - P4.
 *
 * @author Aviad Shikloshi
 */
public class P4Manager extends AbstractPerforceManager<PerforceScm> {

    private static final Logger logger = Logger.getLogger(P4Manager.class.getName());

    public P4Manager(AbstractBuild<?, ?> build, TaskListener buildListener) {
        super(build, buildListener);
    }

    @Override
    public void prepare() {

        PerforceScm perforceScm = getJenkinsScm();
        String credentials = perforceScm.getCredential();

        ConnectionHelper connection = new ConnectionHelper(credentials, buildListener);
        IOptionsServer server = ConnectionFactory.getConnection();
        try {
            String clientString = getClientString();
            if (connection.isClient(clientString)) {
                // Create a Workspace instance (assuming a default workspace is needed)
                Workspace workspace = new Workspace(); // Create a new Workspace instance
                ClientHelper perforceClient = new ClientHelper((ItemGroup) null, credentials, buildListener, workspace); // Updated constructor call
                IClient client = perforceClient.getClient();
                try {
                    this.perforce = new PerforceClient(server, client);
                    this.perforce.initConnection();
                } catch (Exception e) {
                    logger.warning("Could not instantiate connection with PerforceClient: " + e.getMessage());
                }
            } else {
                logger.warning("Client " + clientString + " is not a valid client.");
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "Error occurred: ", e);
        }

    }

    @Override
    public PerforceClient establishConnection() throws Exception {
        this.perforce.initConnection();
        return this.perforce;
    }

    private String getClientString() {
        String client = StringUtils.EMPTY;
        try {
            EnvVars envVars = build.getEnvironment(buildListener);
            client = envVars.get("P4_CLIENT");
        } catch (Exception e) {
            logger.log(Level.FINE, "P4: Unable to read P4_CLIENT");
        }
        return client;
    }
}