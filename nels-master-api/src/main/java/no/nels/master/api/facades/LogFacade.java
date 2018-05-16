package no.nels.master.api.facades;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import no.nels.master.api.db.DAOService;

import java.util.Optional;

/**
 * Created by weizhang on 12/7/16.
 */
public class LogFacade {
    private static Logger logger = LoggerFactory.getLogger(LogFacade.class);

    public static void getLogs(RoutingContext routingContext) {

        Optional<String> sinceStr = Optional.ofNullable(routingContext.request().getParam("since"));
        String contextId = routingContext.request().getParam("contextId");
        DAOService.getInstance().queryLogs(Long.valueOf(contextId), sinceStr.map(Long::valueOf), result -> {
            routingContext.response().putHeader("content-type", "application/json").end(result);
        });

    }

    public static void addLog(RoutingContext routingContext) {

        Optional<JsonObject> body = Optional.ofNullable(routingContext.getBodyAsJson());
        logger.debug("json body: " + body.map(x -> x.toString()));
        Optional<Integer> targetId = body.map(x -> x.getInteger("target_id"));
        Optional<Integer> contextId = body.map(x -> x.getInteger("context_id"));
        Optional<Integer> operatorId = body.map(x -> x.getInteger("operator_id"));
        Optional<String> text = body.map(x -> x.getString("text"));
        if (!targetId.isPresent() ||
                !contextId.isPresent() ||
                !operatorId.isPresent() ||
                !text.isPresent()) {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
            throw new RuntimeException("Some json fields are missing.");
        }
        DAOService.getInstance().insertLog(contextId.get(), targetId.get(), operatorId.get(), text.get(), result -> {
            if (null != result) {
                routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });

    }
}