package no.norstore.storebioinfo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import no.norstore.storebioinfo.mq.MqConsumerVerticle;

import java.io.IOException;

public final class SbiService extends AbstractVerticle {
    private static Logger logger = LoggerFactory.getLogger(SbiService.class);


    @Override
    public void start() throws Exception {
        try {
            Config.init();
            vertx.deployVerticle(Route.class.getName());
            vertx.deployVerticle(MqConsumerVerticle.class.getName());
        } catch (IOException e) {
            logger.error(e);
            throw e;
        }
    }
}
