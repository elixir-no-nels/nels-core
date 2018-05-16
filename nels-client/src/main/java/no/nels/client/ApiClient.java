package no.nels.client;

import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

/**
 * Created by xiaxi on 25/05/2017.
 */
public class ApiClient {
    static Client getMasterClient() {

        ClientConfig config = new ClientConfig();
        String timeout = Config.getTimeout();
        if (!StringUtils.isEmpty(timeout) && Long.valueOf(timeout) > 0) {
            config.property(ClientProperties.CONNECT_TIMEOUT, timeout);
            config.property(ClientProperties.READ_TIMEOUT, timeout);
        }

        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
                .nonPreemptive()
                .credentials(Config.getMasterApiUsername(), Config.getMasterApiPassword())
                .build();

        return ClientBuilder.newClient(config.register(feature));
    }

    static Client getStorageClient() {
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
                .nonPreemptive()
                .credentials(Config.getExtraApiUsername(), Config.getExtraApiPassword())
                .build();

        ClientConfig config = new ClientConfig();

        String timeout = Config.getTimeout();
        if (!StringUtils.isEmpty(timeout) && Long.valueOf(timeout) > 0) {
            config.property(ClientProperties.CONNECT_TIMEOUT, timeout);
            config.property(ClientProperties.READ_TIMEOUT, timeout);
        }
        return ClientBuilder.newClient(config.register(feature));
    }
}
