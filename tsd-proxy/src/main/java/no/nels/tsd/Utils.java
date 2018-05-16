package no.nels.tsd;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import no.nels.tsd.constants.ConfigConstant;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

public final class Utils {
    private static Logger logger = LoggerFactory.getLogger(Utils.class);

    public static Connection createMqConnection() throws IOException, TimeoutException, NoSuchAlgorithmException, KeyManagementException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(Config.valueOf(ConfigConstant.MQ_HOST));
        factory.setUsername(Config.valueOf(ConfigConstant.MQ_USER));
        factory.setPassword(Config.valueOf(ConfigConstant.MQ_PASSWORD));
        factory.setVirtualHost(Config.valueOf(ConfigConstant.MQ_VIRTUAL_HOST));
        factory.setRequestedHeartbeat(60);
        factory.setNetworkRecoveryInterval(10000);
        factory.setConnectionTimeout(5000);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setTopologyRecoveryEnabled(true);
        factory.setPort(Integer.parseInt(Config.valueOf(ConfigConstant.MQ_PORT)));
        if (Config.valueOf(ConfigConstant.MQ_SSL).equals("true")) {
            factory.useSslProtocol();
        }
        return factory.newConnection();
    }

    public static Channel createMqPublisherChannel(Connection connection, String exchangeName) throws IOException {
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(exchangeName, "topic", true);
        return channel;
    }
}
