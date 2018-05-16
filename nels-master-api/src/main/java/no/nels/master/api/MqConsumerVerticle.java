package no.nels.master.api;

import com.rabbitmq.client.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import no.nels.master.api.constants.ConfigName;
import no.nels.master.api.db.DAOService;
import no.nels.vertx.commons.db.DBHelper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

public final class MqConsumerVerticle extends AbstractVerticle {
    private static Logger logger = LoggerFactory.getLogger(MqConsumerVerticle.class);


    @Override
    public void start() throws Exception {
        logger.debug("start mqconsumer verticle: " + Thread.currentThread());
        DAOService.init(vertx, Config.valueOf(ConfigName.DB_URL), Config.valueOf(ConfigName.DB_USER), Config.valueOf(ConfigName.DB_DRIVER_CLASS),
                Config.valueOf(ConfigName.DB_PASSWORD), 30);
        DBHelper.init( Config.valueOf(ConfigName.DB_DRIVER_CLASS), Config.valueOf(ConfigName.DB_URL), Config.valueOf(ConfigName.DB_USER), Config.valueOf(ConfigName.DB_PASSWORD));

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(Config.valueOf(ConfigName.MQ_HOST));
        factory.setPort(5671);
        factory.setUsername(Config.valueOf(ConfigName.MQ_USER));
        factory.setPassword(Config.valueOf(ConfigName.MQ_PASSWORD));
        factory.setVirtualHost(Config.valueOf(ConfigName.MQ_VIRTUAL_HOST));

        Connection connection = null;
        try {
            factory.useSslProtocol();
            connection = factory.newConnection();
            Channel feedChannel = connection.createChannel();
            feedChannel.exchangeDeclare("async_jobs_update_exchange", "topic", true);
            feedChannel.queueDeclare("async_jobs_feed_queue", true, false, false, null);
            feedChannel.queueBind("async_jobs_feed_queue", "async_jobs_update_exchange", "*.*.feed");

            feedChannel.basicConsume("async_jobs_feed_queue", false, new MqConsumer(feedChannel));

            Channel statusChannel = connection.createChannel();

            statusChannel.exchangeDeclare("async_jobs_update_exchange", "topic", true);
            statusChannel.queueDeclare("async_jobs_status_queue", true, false, false, null);
            statusChannel.queueBind("async_jobs_status_queue", "async_jobs_update_exchange", "*.*.status");

            statusChannel.basicConsume("async_jobs_status_queue", false, new MqConsumer(statusChannel));
        } catch (IOException | TimeoutException e) {
            logger.error(e.getMessage());

            if (connection != null && connection.isOpen()) {
                try {
                    connection.close();
                } catch (IOException e1) {
                }
            }
            throw e;
        }
    }

    /**
     * Master api is supposed to do two things
     * 1. update job state
     * 2. add job feed
     */
    private class MqConsumer extends DefaultConsumer {
        public MqConsumer(Channel channel) {
            super(channel);
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
            String message = new String(body, "UTF-8");
            logger.debug("RCV: " + message);

            String routingKey = envelope.getRoutingKey();
            String[] keys = routingKey.split("\\.");
            long jobId = Long.valueOf(keys[1]);
            Long jobStatus = null;
            Double completion = null;
            logger.debug("routingKey: " + routingKey + ", keys:" + Arrays.toString(keys) + ",jobId:" + jobId);
            logger.debug("In mq consumer handler: " + Thread.currentThread());
            String messageType = keys[2];

            switch (messageType) {
                case "status":
                    JsonObject jsonObject = new JsonObject(message);
                    if (jsonObject.containsKey("job_status")) {
                        jobStatus = jsonObject.getLong("job_status");
                    }
                    if (jsonObject.containsKey("completion")) {
                        completion = jsonObject.getDouble("completion");
                    }
                    double completionDouble = completion * 100;
                    logger.debug("start updating job: jobStatus:" + jobStatus + ",completion:" + (int) completionDouble);


                    try {
                        JDBCHelper.update(jobId, jobStatus, (int)completionDouble);
                    } catch (SQLException e) {
                        logger.error("update error." + e.getLocalizedMessage());
                    }


                    break;
                case "feed":
                    DAOService.getInstance().insertJobFeed(jobId, message, res -> {
                        logger.debug("inserted jobfeed: " + res);
                    });
                    break;
                default:
                    logger.debug("unknown messagetype:" + messageType);

            }
            getChannel().basicAck(envelope.getDeliveryTag(), false);
        }
    }
}
