package no.norstore.storebioinfo.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import no.nels.commons.constants.OAuthConstant;
import no.norstore.storebioinfo.Config;
import no.norstore.storebioinfo.Route;
import no.norstore.storebioinfo.constants.ConfigName;
import no.norstore.storebioinfo.constants.JsonKey;
import no.norstore.storebioinfo.constants.UrlParam;
import no.norstore.storebioinfo.exceptions.IllegalBodyException;
import no.norstore.storebioinfo.exceptions.IllegalUrlParamException;
import no.norstore.storebioinfo.utils.CheckedAction;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;


/**
 * Created by xiaxi on 07/07/2017.
 */
public interface HttpRequestHandler {
    Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);

    default void returnResponse(RoutingContext routingContext, HttpResponseStatus status, AsyncResult<String> result) {
        if (result.succeeded()) {
            routingContext.response().setStatusCode(status.code()).end(result.result());
        } else {
            logger.error("", result.cause());
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .end(new JsonObject().put(JsonKey.DESCRIPTION, result.cause().getMessage()).encodePrettily());
        }
    }

    default void returnResponseForCreation(RoutingContext routingContext, AsyncResult<Long> result) {
        if (result.succeeded()) {
            routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end(new JsonObject().put(JsonKey.ID, result.result()).encode());
        } else {
            logger.error("", result.cause());
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .end(new JsonObject().put(JsonKey.DESCRIPTION, result.cause().getMessage()).encodePrettily());
        }
    }

    default void returnResponseWithCount(RoutingContext routingContext, HttpResponseStatus status, AsyncResult<String> result, String countSql) {
        if (result.succeeded()) {
            Route.vertxDBHelper.count(countSql, countResult -> {
                if (countResult.succeeded()) {
                    routingContext.response().setStatusCode(status.code()).end(new JsonObject().put(JsonKey.DATA, new JsonArray(result.result())).put(JsonKey.COUNT, countResult.result()).encode());
                } else {
                    logger.error("", countResult.cause());
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                }
            });
        } else {
            logger.error("", result.cause());
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .end(new JsonObject().put(JsonKey.DESCRIPTION, result.cause().getMessage()).encodePrettily());
        }
    }

    default void returnResponseWithExtraFunction(RoutingContext routingContext, HttpResponseStatus status, AsyncResult<String> result, Function<String, String> function) {
        if (result.succeeded()) {
            String responseString = function.apply(result.result());
            routingContext.response().setStatusCode(status.code()).end(responseString);
        } else {
            logger.error("", result.cause());
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .end(new JsonObject().put(JsonKey.DESCRIPTION, result.cause().getMessage()).encodePrettily());
        }
    }

    default void validateRequest(RoutingContext routingContext, CheckedAction action) {
        try {
            action.execute();
        } catch (IllegalBodyException e) {
            logger.error(e.getMessage());
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                    .end(new JsonObject().put(JsonKey.DESCRIPTION, e.getMessage()).encodePrettily());
        } catch (Exception e) {
            logger.error(e.getMessage(), e.getCause());
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
        }
    }

    default JsonObject validateRequestBody(RoutingContext routingContext, String... keys) throws IllegalBodyException {
        Optional<JsonObject> body = Optional.ofNullable(routingContext.getBodyAsJson());

        if (body.isPresent()) {
            for (String key : keys) {
                if (!body.get().containsKey(key)) {
                    throw new IllegalBodyException("JSON field " + key + " is missing");
                }
            }
        } else {
            throw new IllegalBodyException("Request body is missing");
        }
        return body.get();
    }

    default Pair<Optional<String>, Optional<String>>
    validateUrlParamForReturnPartialResult(RoutingContext routingContext) throws IllegalUrlParamException {
        String offsetParam = routingContext.request().getParam(UrlParam.OFFSET);
        String limitParam = routingContext.request().getParam(UrlParam.LIMIT);
        if (!StringUtils.isEmpty(offsetParam) && !StringUtils.isNumeric(offsetParam)) {
            throw new IllegalUrlParamException("offset parameter is invalid");
        }
        if (!StringUtils.isEmpty(limitParam) && !StringUtils.isNumeric(limitParam)) {
            throw new IllegalUrlParamException("limit parameter is invalid");
        }

        Optional<String> offset = StringUtils.isEmpty(offsetParam) ? Optional.empty() : Optional.of(offsetParam);
        Optional<String> limit = StringUtils.isEmpty(limitParam) ? Optional.of(Config.valueOf(ConfigName.DEFAULT_LIMIT)) : Optional.of(limitParam);

        return Pair.of(limit, offset);
    }

    default Optional<String> validateUrlParamForSorting(RoutingContext routingContext, String... sortingKeys) throws IllegalUrlParamException {
        String sort = routingContext.request().getParam(UrlParam.SORT);

        if (StringUtils.isEmpty(sort)) {
            return Optional.empty();
        } else {
            for (String sortingKey : sortingKeys) {
                if (sort.equals("-" + sortingKey) || sort.equals(sortingKey)) {
                    return Optional.of(sort);
                }
            }
            throw new IllegalUrlParamException("the sort parameter:" + sort + " is not supported");
        }
    }

    default Map<String, Long> validateNumericParamOfUrlPath(RoutingContext routingContext, String... params) throws IllegalUrlParamException{
        Map<String, Long> map = new HashMap<>(params.length);
        for (String param : params) {
            String value = routingContext.request().getParam(param);
            if (StringUtils.isNumeric(value)) {
                map.put(param, Long.valueOf(value));
            } else {
                throw new IllegalUrlParamException("The url param " + param + " should be numeric");
            }
        }
        return map;
    }

    default void sendJournal(RoutingContext routingContext, JsonObject journal) {
        RequestOptions options = new RequestOptions().setHost(Config.valueOf(no.nels.commons.constants.ConfigName.OAUTH_HOST))
                .setSsl(Boolean.parseBoolean(Config.valueOf(no.nels.commons.constants.ConfigName.OAUTH_SSL)))
                .setURI("/oauth2/token")
                .setPort(Integer.valueOf(Config.valueOf(no.nels.commons.constants.ConfigName.OAUTH_PORT)));
        HttpClientRequest request = Route.httpClient.post(options, res -> {
            res.exceptionHandler(throwable -> {
                logger.error("error:" + throwable.getMessage());
            });
            res.bodyHandler(body -> {

                JsonObject responseBody = body.toJsonObject();
                logger.debug("body info:" + responseBody.encodePrettily());
                String token = responseBody.getString(OAuthConstant.ACCESS_TOKEN);
                String url = Config.valueOf(no.nels.commons.constants.ConfigName.NELS_API_URL);
                HttpClientRequest addStructuredLogReq = Route.httpClient.postAbs( url + "/logs/add", response -> {
                    response.exceptionHandler(throwable -> logger.error("add log error:" + throwable.getLocalizedMessage()));
                    if (response.statusCode() == HttpResponseStatus.CREATED.code()) {
                        logger.debug("log is added");
                        //routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end();
                    } else {
                        logger.debug("failed to add log. response:" + response.statusMessage());
                        //routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(response.statusMessage());

                    }

                });
                addStructuredLogReq.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                addStructuredLogReq.putHeader(HttpHeaders.AUTHORIZATION, OAuthConstant.BEARER + " " + token);
                addStructuredLogReq.end(journal.encode());
            });
        });

        JsonObject requestBody = new JsonObject();
        requestBody.put(no.nels.commons.constants.ConfigName.CLIENT_ID, Config.valueOf(no.nels.commons.constants.ConfigName.CLIENT_ID))
                .put(no.nels.commons.constants.ConfigName.CLIENT_SECRET, Config.valueOf(no.nels.commons.constants.ConfigName.CLIENT_SECRET))
                .put(no.nels.commons.constants.ConfigName.GRANT_TYPE, Config.valueOf(no.nels.commons.constants.ConfigName.GRANT_TYPE))
                .put(no.nels.commons.constants.ConfigName.SCOPE, Config.valueOf(no.nels.commons.constants.ConfigName.SCOPE));
        request.putHeader(io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.end(requestBody.encode());
    }
}
