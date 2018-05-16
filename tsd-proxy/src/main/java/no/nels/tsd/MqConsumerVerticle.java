package no.nels.tsd;

import com.rabbitmq.client.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import no.nels.tsd.constants.ConfigConstant;
import no.nels.vertx.commons.constants.MqJobType;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;


public final class MqConsumerVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(MqConsumerVerticle.class);

    @Override
    public void start() throws Exception {
        Connection connection = null;
        try {
            connection = Utils.createMqConnection();

            int consumerNum = Integer.parseInt(Config.valueOf(ConfigConstant.MQ_CONSUMER_NUMBER));

            Channel consumerChannel;
            Channel publishChannel;
            for (int i = 0; i < consumerNum; i++) {
                publishChannel = Utils.createMqPublisherChannel(connection, "async_jobs_update_exchange");

                consumerChannel = connection.createChannel();

                consumerChannel.exchangeDeclare("async_jobs_exchange", "topic", true);
                consumerChannel.queueDeclare("tsd_queue", true, false, false, null);
                consumerChannel.queueBind("tsd_queue", "async_jobs_exchange", "tsd.*.*.submit");
                int prefetchCount = 1;
                consumerChannel.basicQos(prefetchCount);

                consumerChannel.basicConsume("tsd_queue", false, new MqConsumer(consumerChannel, publishChannel));
            }
        } catch (IOException | TimeoutException | NoSuchAlgorithmException | KeyManagementException e) {
            logger.error(e);

            if (connection != null & connection.isOpen()) {
                try {
                    connection.close();
                } catch (IOException e1) {
                }
            }
            throw e;
        }

    }



    private class MqConsumer extends DefaultConsumer {
        private final Channel publishChannel;

        public MqConsumer(Channel consumerChannel, Channel publishChannel) {
            super(consumerChannel);
            this.publishChannel = publishChannel;
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
            String message = new String(body, "UTF-8");
            logger.debug("Message: " + message);
            String routingKey = envelope.getRoutingKey();
            logger.debug("Routing key: " + routingKey);
            int jobType = Integer.parseInt(routingKey.split("\\.")[1]);
            long jobId = Long.parseLong(routingKey.split("\\.")[2]);

            final String feedRoutingKey = StringUtils.join(new String[]{String.valueOf(jobType), String.valueOf(jobId), "feed"}, ".");
            final String statusRoutingKey = StringUtils.join(new String[]{String.valueOf(jobType), String.valueOf(jobId), "status"}, ".");
            switch (MqJobType.valueOf(jobType)) {
                case TSD_PUSH:
                    logger.debug("Move data from tsd to NeLS");
                    MqJobFacade.pushDataToNels(new JsonObject(message), feedInfo -> {

                        try {
                            publishChannel.basicPublish("async_jobs_update_exchange", feedRoutingKey, null, feedInfo);
                        } catch (IOException e) {
                            //TODO
                        }
                    }, statusMessage -> {

                        try {
                            publishChannel.basicPublish("async_jobs_update_exchange", statusRoutingKey, null, statusMessage);
                        } catch (IOException e) {
                            //TODO
                        }
                    });
                    break;
                case TSD_PULL:
                    logger.debug("Move data from NeLS to tsd");
                    MqJobFacade.pullDataFromNels(new JsonObject(message), feedInfo -> {

                        try {
                            publishChannel.basicPublish("async_jobs_update_exchange", feedRoutingKey, null, feedInfo);
                        } catch (IOException e) {
                            //TODO
                        }
                    }, statusMessage -> {

                        try {
                            publishChannel.basicPublish("async_jobs_update_exchange", statusRoutingKey, null, statusMessage);
                        } catch (IOException e) {
                            //TODO
                        }
                    });
                    break;
            }
            getChannel().basicAck(envelope.getDeliveryTag(), false);
        }
    }

}
