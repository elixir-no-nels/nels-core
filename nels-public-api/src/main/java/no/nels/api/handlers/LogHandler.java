package no.nels.api.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import no.nels.api.Route;
import no.nels.api.constants.JsonKey;
import no.nels.api.constants.UrlParam;
import no.nels.commons.constants.JsonObjectKey;
import no.nels.commons.constants.LogContextType;
import no.nels.commons.constants.NelsUserType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

public final class LogHandler implements IHttpRequestHandler {

    private static Logger logger = LoggerFactory.getLogger(LogHandler.class);

    public void getLogs(RoutingContext routingContext)  {

        validateRequest(routingContext,
                () -> true,
                () -> {

            Optional<String> sinceStr = Optional.ofNullable(routingContext.request().getParam(UrlParam.SINCE));

            Map<String, Long> params = validateNumericParamOfUrlPath(routingContext, UrlParam.CONTEXT_ID);
            long contextId = params.get(UrlParam.CONTEXT_ID);
            String sql;
            JsonArray paramsJsonArray = new JsonArray();
            paramsJsonArray.add(contextId);
            if (sinceStr.isPresent()) {
                Date sinceTime = new java.util.Date(Long.valueOf(sinceStr.get()) * 1000);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String sinceParam = dateFormat.format(sinceTime);
                sql = "select * from structured_log where logcontextId=? and logtime >=to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS') ORDER BY id DESC";
                paramsJsonArray.add(sinceParam);
            } else {
                sql = "select * from structured_log where logcontextId=? ORDER BY id DESC";
            }
            Route.vertxDBHelper.select(sql, stringAsyncResult ->
                returnResponse(routingContext, HttpResponseStatus.OK, stringAsyncResult)
            );
        });
    }

    public void addLog(RoutingContext routingContext) {
        routingContext.request().exceptionHandler(throwable -> logger.error("addLog error:" + throwable.getLocalizedMessage()) );
        validateRequest(routingContext, () -> true,  () -> {
            logger.debug("Add log");
            JsonObject jsonObject = validateRequestBody(routingContext, JsonObjectKey.TARGET_ID, JsonObjectKey.CONTEXT_ID, JsonObjectKey.OPERATOR_ID, JsonObjectKey.TEXT);
            if (routingContext.user().principal().getString(JsonKey.NAME).equalsIgnoreCase("sbi")) {
              JsonObject body = validateRequestBody(routingContext, JsonObjectKey.TARGET_ID, JsonObjectKey.CONTEXT_ID, JsonObjectKey.OPERATOR_ID, JsonObjectKey.TEXT, JsonObjectKey.FEDERATED_ID);
              String federatedId = jsonObject.getString(JsonObjectKey.FEDERATED_ID);
              Route.vertxDBHelper.getOne("SELECT id FROM users WHERE idpusername = ?", new JsonArray().add(federatedId), stringAsyncResult -> {
                  if (stringAsyncResult.succeeded()) {
                      long nelsId = new JsonObject(stringAsyncResult.result()).getLong(JsonKey.ID);
                      body.put(JsonObjectKey.OPERATOR_ID, nelsId);
                      insertStructuredLog(routingContext, body);

                  } else {
                      routingContext.response().putHeader("content-type", "application/json").setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end(new JsonObject().put(JsonKey.DESCRIPTION, "User not found.").encode());
                  }
              });
            } else {
                insertStructuredLog(routingContext, jsonObject);
            }
        });
    }

    public void getSbiBlockQuota(RoutingContext routingContext) {

        validateRequest(routingContext,
                () -> routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.ADMINISTRATOR.getName()) || routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.HELPDESK.getName()),
                () -> {
                    String sql = "select * from structured_log where logcontextId=? ORDER BY id DESC limit 1";

                    Route.vertxDBHelper.getOne(sql, new JsonArray().add(LogContextType.SBI_BLOCK_QUOTA_UPDATE.getValue()), stringAsyncResult -> {
                        if (stringAsyncResult.succeeded()) {
                            String result = stringAsyncResult.result();
                            String e = "";
                            if (!result.isEmpty()) {

                                e = new JsonObject(result).getString(JsonObjectKey.LOGTEXT, "");
                            }

                            routingContext.response().putHeader("content-type", "application/json").setStatusCode(HttpResponseStatus.OK.code()).end(e);

                        } else {
                            routingContext.response().putHeader("content-type", "application/json").setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(new JsonObject().put(JsonKey.DESCRIPTION, stringAsyncResult.cause().getLocalizedMessage()).encode());
                        }
                    });


                }
        );
    }

    public void updateSbiBlockQuota(RoutingContext routingContext) {

        validateRequest(routingContext,
                () -> routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.ADMINISTRATOR.getName()) || routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.HELPDESK.getName()),
                () -> {
                    JsonObject json = validateRequestBody(routingContext, JsonObjectKey.VALUE, JsonObjectKey.COMMENT);
                    JsonObject jsonObject = new JsonObject().put(JsonObjectKey.CONTEXT_ID, LogContextType.SBI_BLOCK_QUOTA_UPDATE.getValue())
                            .put(JsonObjectKey.TARGET_ID, 0)
                            .put(JsonObjectKey.OPERATOR_ID, routingContext.user().principal().getLong(JsonKey.NELS_ID))
                            .put(JsonObjectKey.TEXT, json.encode());
                    insertStructuredLog(routingContext, jsonObject);

                }
        );
    }

    void insertStructuredLog(RoutingContext routingContext, JsonObject jsonObject) {
        String sql = "INSERT INTO structured_log (logcontextid, targetid, operatorid, logtext, logtime)" +
                "VALUES (?, ?, ?, ?, to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'))";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = dateFormat.format(new Date());
        JsonArray params = new JsonArray().add(jsonObject.getLong(JsonObjectKey.CONTEXT_ID))
                .add(jsonObject.getLong(JsonObjectKey.TARGET_ID))
                .add(jsonObject.getLong(JsonObjectKey.OPERATOR_ID))
                .add(jsonObject.getString(JsonObjectKey.TEXT)).add(timestamp);
        Route.vertxDBHelper.insert(sql, params, id ->
                returnResponse(routingContext, HttpResponseStatus.CREATED, id.map(String::valueOf))
        );
    }
}
