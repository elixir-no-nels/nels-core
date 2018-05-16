package no.nels.master.api.facades;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import no.nels.commons.constants.StatsContextType;
import no.nels.master.api.constants.JsonKey;
import no.nels.master.api.constants.UrlParam;
import no.nels.master.api.db.DAOService;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 * Created by weizhang on 12/7/16.
 */
public class StatFacade {

    private static Logger logger = LoggerFactory.getLogger(StatFacade.class);

    public static void addStat(RoutingContext routingContext) {

        Optional<JsonObject> body = Optional.ofNullable(routingContext.getBodyAsJson());
        logger.debug("json body: " + body.map(x -> x.toString()));
        Optional<Long> targetId = body.map(x -> x.getLong("targetId"));
        Optional<Long> contextId = body.map(x -> x.getLong("contextId"));
        Optional<Double> value = body.map(x -> x.getDouble("value"));

        if (!targetId.isPresent() ||
                !contextId.isPresent() ||
                !value.isPresent()) {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end(new JsonObject().put(JsonKey.DESCRIPTION, new JsonArray().add("Some json fields are missing.")).encode());
            return;
        }

        if (contextId.get() != StatsContextType.NELS_PERSONAL_DISK_USAGE &&
                contextId.get() != StatsContextType.NELS_PERSONAL_DISK_USAGE_SUMMARY &&
                contextId.get() != StatsContextType.NELS_PROJECT_DISK_USAGE &&
                contextId.get() != StatsContextType.NELS_PROJECT_DISK_USAGE_SUMMARY) {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end(new JsonObject().put(JsonKey.DESCRIPTION, new JsonArray().add("Unsupported context type")).encode());
            return;
        }

        DAOService.getInstance().insertStat(contextId.get(), targetId.get(), value.get(), result -> {
            if (null != result) {
                routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });

    }


    public static void getStats(RoutingContext routingContext) {

        Optional<String> sinceStr = Optional.ofNullable(routingContext.request().getParam("since"));
        String contextId = routingContext.request().getParam("contextId");
        DAOService.getInstance().queryStats(Long.valueOf(contextId), sinceStr.map(Long::valueOf), result -> {
            routingContext.response().putHeader("content-type", "application/json").end(result);
        });

    }

    public static void getStatsTarget(RoutingContext routingContext) {
        String contextId = routingContext.request().getParam(UrlParam.CONTEXT_ID);
        String targetId = routingContext.request().getParam(UrlParam.TARGET_ID);

        if (StringUtils.isNumeric(contextId) && StringUtils.isNumeric(targetId)) {
            if ((Long.valueOf(contextId) != StatsContextType.NELS_PERSONAL_DISK_USAGE) &&
                    (Long.valueOf(contextId) != StatsContextType.NELS_PROJECT_DISK_USAGE) &&
                    (Long.valueOf(contextId) != StatsContextType.NELS_PROJECT_DISK_USAGE_SUMMARY) &&
                    (Long.valueOf(contextId) != StatsContextType.NELS_PROJECT_DISK_USAGE_SUMMARY) ) {
                routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end(new JsonObject().put(JsonKey.DESCRIPTION, new JsonArray().add("not correct context type")).encode());
            } else {
                String sql = "SELECT value, statstime FROM statistics WHERE statscontextid=? AND targetid=? ORDER BY statstime DESC";
                DAOService.getInstance().getDbHelper().select(sql, new JsonArray().add(Integer.valueOf(contextId)).add(Integer.valueOf(targetId)), result -> {
                    if (result.succeeded()) {
                        routingContext.response().end(result.result());
                    } else {
                        logger.error("Can't get target", result.cause());
                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                    }
                });
            }
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end(new JsonObject().put(JsonKey.DESCRIPTION, new JsonArray().add("Url params should be numeric")).encode());
        }
    }
}