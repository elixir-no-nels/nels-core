package no.nels.master.api;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import no.nels.master.api.constants.ConfigName;
import no.nels.vertx.commons.constants.MqJobType;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

/**
 * Created by weizhang on 4/26/16.
 */
public final class Sender {
    private static final String EXCHANGE_NAME = "async_jobs_exchange";
    private static Logger logger = LoggerFactory.getLogger(Sender.class);

    public static void send(String params, long jobTypeId, long jobId) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(Config.valueOf(ConfigName.MQ_HOST));
        factory.setPort(Integer.valueOf(Config.valueOf(ConfigName.MQ_PORT)));

        factory.setUsername(Config.valueOf(ConfigName.MQ_USER));
        factory.setPassword(Config.valueOf(ConfigName.MQ_PASSWORD));
        factory.setVirtualHost(Config.valueOf(ConfigName.MQ_VIRTUAL_HOST));
        String routingKey = "";
        MqJobType jobType = MqJobType.valueOf(Math.toIntExact(jobTypeId));
        switch (jobType) {
            case STORAGE_COPY:
            case STORAGE_MOVE:
                routingKey = "storage.";
                break;
            case SBI_PULL:
            case SBI_PUSH:
                routingKey = "sbi.";
                break;
            case TSD_PULL:
            case TSD_PUSH:
                routingKey = "tsd.";
                break;
            case NIRD_SBI_PUSH:
            case NIRD_SBI_PULL:
                routingKey = "nird.";
                break;
            default:
                logger.debug("unknown jobType:" + jobType.name());

        }
        routingKey = routingKey + jobTypeId + "." + jobId + ".submit";

        try {
            factory.useSslProtocol();
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);

            channel.basicPublish(EXCHANGE_NAME, routingKey, null, params.getBytes());
            logger.debug(" routingKey: " + routingKey + ",[x] Sent: " + params);
            connection.close();
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage());
            e.printStackTrace();
        } catch (TimeoutException ex) {
            logger.error(ex.getLocalizedMessage());
            ex.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getLocalizedMessage());
            e.printStackTrace();
        } catch (KeyManagementException e) {
            logger.error(e.getLocalizedMessage());
            e.printStackTrace();
        }


    }
}
