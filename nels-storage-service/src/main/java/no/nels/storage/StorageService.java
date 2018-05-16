package no.nels.storage;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;

public final class StorageService extends AbstractVerticle{
    private static Logger logger = LoggerFactory.getLogger(StorageService.class);

    @Override
    public void start() throws Exception {
        try {
            Config.init();
            vertx.deployVerticle(HttpVerticle.class.getName());
            vertx.deployVerticle(MqConsumerVerticle.class.getName());
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw e;
        }
    }
}
