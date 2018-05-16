package no.nels.tsd;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;

import java.io.IOException;

public final class TsdService extends AbstractVerticle {
    private static Logger logger = LoggerFactory.getLogger(TsdService.class);

    @Override
    public void start() throws Exception {
        try {
            Config.init();
            vertx.deployVerticle(HttpVerticle.class.getName());
            vertx.deployVerticle(MqConsumerVerticle.class.getName());
        } catch (IOException e) {
            logger.error(e);
            throw e;
        }
    }
}
