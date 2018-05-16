package no.nels.api.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import no.nels.api.Config;
import no.nels.api.constants.ConfigName;
import no.nels.api.constants.JsonKey;
import no.nels.api.constants.UrlParam;
import no.nels.api.exceptions.IllegalBodyException;
import no.nels.api.exceptions.IllegalUrlParamException;
import no.nels.api.sec.CheckedAction;
import no.nels.api.sec.CheckedSupplier;
import no.nels.client.sbi.SbiException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Created by xiaxi on 23/05/2017.
 */
public interface IHttpRequestHandler {
    Logger logger = LoggerFactory.getLogger(IHttpRequestHandler.class);

    default Pair<Optional<String>, Optional<String>> validateUrlParamForReturnPartialResult(RoutingContext routingContext) throws IllegalUrlParamException {
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
            throw new IllegalUrlParamException("the sort parameter is not supported");
        }
    }

    default Map<String, Long> validateNumericParamOfUrlPath(RoutingContext routingContext, String... params) throws IllegalUrlParamException {
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
        logger.debug("json body: " + body.map(x -> x.toString()));

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

    default void returnResponseFunction(RoutingContext routingContext, Supplier<Boolean> isAuthorized, CheckedSupplier<Pair<HttpResponseStatus, Optional<String>>> supplier) {
        if (isAuthorized.get()) {
            try {
                Pair<HttpResponseStatus, Optional<String>> pair = supplier.get();
                Optional<String> optional = pair.getRight();
                if (optional.isPresent()) {
                    routingContext.response().putHeader("content-type", "application/json").setStatusCode(pair.getLeft().code()).end(optional.get());
                } else {
                    routingContext.response().putHeader("content-type", "application/json").setStatusCode(pair.getLeft().code()).end();
                }
            } catch (IllegalUrlParamException | IllegalBodyException e) {
                logger.error(e.getMessage());
                routingContext.response().putHeader("content-type", "application/json").setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end(new JsonObject().put(JsonKey.DESCRIPTION, e.getMessage()).encode());
            } catch (SbiException e) {
                logger.error(e.getMessage());
                if (e.getStatusCode() == HttpResponseStatus.INTERNAL_SERVER_ERROR.code()) {
                    routingContext.response().putHeader("content-type", "application/json").setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                } else {
                    routingContext.response().putHeader("content-type", "application/json").setStatusCode(e.getStatusCode()).end(e.getMessage());
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e.fillInStackTrace());
                routingContext.response().putHeader("content-type", "application/json").setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        } else {
            routingContext.response().putHeader("content-type", "application/json").setStatusCode(HttpResponseStatus.FORBIDDEN.code()).end();
        }
    }

    default void returnResponse(RoutingContext routingContext, HttpResponseStatus status, AsyncResult<String> result) {
        if (result.succeeded()) {
            routingContext.response().putHeader("content-type", "application/json").setStatusCode(status.code()).end(result.result());
        } else {
            logger.error("", result.cause());
            routingContext.response().putHeader("content-type", "application/json").setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .end(new JsonObject().put(JsonKey.DESCRIPTION, result.cause().getMessage()).encodePrettily());
        }
    }

    default void validateRequest(RoutingContext routingContext, Supplier<Boolean> isAuthorized, CheckedAction action) {
        if (isAuthorized.get()) {
            try {
                action.execute();
            } catch (IllegalBodyException | IllegalUrlParamException e) {
                logger.error(e.getMessage());
                routingContext.response().putHeader("content-type", "application/json").setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                        .end(new JsonObject().put(JsonKey.DESCRIPTION, e.getMessage()).encodePrettily());
            } catch (Exception e) {
                logger.error(e.getMessage(), e.getCause());
                routingContext.response().putHeader("content-type", "application/json").setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        } else {
            routingContext.response().putHeader("content-type", "application/json").setStatusCode(HttpResponseStatus.FORBIDDEN.code()).end();
        }
    }

}
