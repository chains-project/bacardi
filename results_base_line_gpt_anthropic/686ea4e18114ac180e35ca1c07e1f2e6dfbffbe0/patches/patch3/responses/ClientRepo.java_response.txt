package com.wire.lithium;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.xenon.WireAPI;
import com.wire.xenon.WireClient;
import com.wire.xenon.backend.models.NewBot;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.factories.CryptoFactory;
import com.wire.xenon.factories.StorageFactory;
import com.wire.xenon.state.State;

// Updated import for Client class as required
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Client; // Ensure the correct Client class is imported

import java.io.IOException;
import java.util.UUID;

public class ClientRepo {
    protected final Client httpClient;
    protected final CryptoFactory cf;
    protected final StorageFactory sf;

    public ClientRepo(Client httpClient, CryptoFactory cf, StorageFactory sf) {
        this.httpClient = httpClient;
        this.cf = cf;
        this.sf = sf;
    }

    public WireClient getClient(UUID botId) throws IOException, CryptoException {
        NewBot state = sf.create(botId).getState();
        Crypto crypto = cf.create(botId);
        WireAPI api = new WireAPI(httpClient, state.token); // Corrected instantiation to use the correct class
        
        // Create an instance of WireClient through a proper static factory method if available
        WireClient wireClient = WireClient.create(api, crypto, state); // Assuming a static method for instantiation
        return wireClient; // Return the created WireClient instance
    }

    public void purgeBot(UUID botId) throws IOException {
        State state = sf.create(botId);
        if (state == null)
            return;

        boolean purged = state.removeState();
        if (!purged)
            throw new IOException("Failed to purge Bot: " + botId);
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public CryptoFactory getCf() {
        return cf;
    }

    public StorageFactory getSf() {
        return sf;
    }
}