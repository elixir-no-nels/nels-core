package no.nels.tsd;


import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.auth.AuthProvider;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.BasicAuthHandler;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import no.nels.tsd.constants.ConfigConstant;


public final class HttpVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(HttpVerticle.class);

    @Override
    public void start() throws Exception {

        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        router.route().handler(BasicAuthHandler.create(new AuthProvider(new BasicHttpAuthProvider())));

        router.route("/version").produces("application/json").handler(routingContext -> {

            JsonObject jsonObject = new JsonObject();
            jsonObject.put("version", 1);
            HttpServerResponse response = routingContext.response();
            response.end(jsonObject.encode());
        });

        router.post("/users/:userName/connect").consumes("*/json").produces("text/plain").blockingHandler(RoutingFacade::connectTo, false);

        router.delete("/users/:userName/disconnect").handler(RoutingFacade::disconnectTsdSession);

        router.getWithRegex("\\/users\\/([^\\/]+)\\/(export|import|export\\/.+|import\\/.+)").produces("application/json").blockingHandler(RoutingFacade::navigateTo, false);

        router.get("/users/:userName").produces("application/json").blockingHandler(RoutingFacade::navigateToHome, false);

        vertx.createHttpServer().requestHandler(router::accept).listen(
                Integer.parseInt(Config.valueOf(ConfigConstant.PORT)), Config.valueOf(ConfigConstant.HOST));
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }
}
