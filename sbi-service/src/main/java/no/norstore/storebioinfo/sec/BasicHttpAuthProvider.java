package no.norstore.storebioinfo.sec;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import no.norstore.storebioinfo.Config;

public final class BasicHttpAuthProvider implements AuthProvider {
    private static Logger logger = LoggerFactory.getLogger(BasicHttpAuthProvider.class);

    @Override
    public void authenticate(JsonObject jsonObject, Handler<AsyncResult<User>> handler) {
        String userName = jsonObject.getString("username");
        String password = jsonObject.getString("password");


        logger.debug("credentials in request:" + userName + "----" + password);
        if (null != password && password.equals(Config.valueOf(userName))) {
            logger.debug("is authenticated.");
            handler.handle(Future.succeededFuture());
        } else {
            logger.debug("is not authenticated.");
            handler.handle(Future.failedFuture("Authentication failed"));
        }


    }
}
