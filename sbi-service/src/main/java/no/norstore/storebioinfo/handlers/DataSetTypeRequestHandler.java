package no.norstore.storebioinfo.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import no.norstore.storebioinfo.Route;
import no.norstore.storebioinfo.constants.JsonKey;
import no.norstore.storebioinfo.constants.UrlParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public final class DataSetTypeRequestHandler implements HttpRequestHandler {

    public void deleteDataSetType(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.DATASET_TYPE_ID);

            JsonArray params = new JsonArray();
            params.add(map.get(UrlParam.DATASET_TYPE_ID));

            Route.vertxDBHelper.count("SELECT count(*) FROM data_set WHERE type IN (SELECT name FROM data_set_type WHERE id=?)", params, countResult -> {
                if (countResult.succeeded()) {
                    int count = countResult.result();
                    if (count > 0) {
                        logger.debug("Can't delete this data set type");
                        routingContext.response().setStatusCode(HttpResponseStatus.PRECONDITION_FAILED.code())
                                .end(new JsonObject().put(JsonKey.DESCRIPTION, "You can't delete this type, because there are data sets referring to this type.").encodePrettily());
                    } else {
                        Route.vertxDBHelper.updateMultipleSqlInTransaction(finalResult -> {
                                    if (finalResult.succeeded()) {
                                        routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
                                    } else {
                                        logger.error("Creating data set type failed", finalResult.cause());
                                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                                .end(new JsonObject().put(JsonKey.DESCRIPTION, finalResult.cause().getMessage()).encodePrettily());
                                    }
                                }, (connection, asyncResultHandler) ->
                                        connection.updateWithParams("DELETE FROM data_set_type_subtypes WHERE datasettype_id=?", params, deleteDataSetTypeSubtypesResult -> {
                                            if (deleteDataSetTypeSubtypesResult.succeeded()) {
                                                connection.updateWithParams("DELETE FROM data_set_type WHERE id=?", params, deleteDataSetTypeResult -> {
                                                    if (deleteDataSetTypeResult.succeeded()) {
                                                        asyncResultHandler.handle(Future.succeededFuture());
                                                    } else {
                                                        asyncResultHandler.handle(Future.failedFuture(deleteDataSetTypeResult.cause()));
                                                    }
                                                });
                                            } else {
                                                asyncResultHandler.handle(Future.failedFuture(deleteDataSetTypeSubtypesResult.cause()));
                                            }
                                        })
                        );
                    }
                } else {
                    logger.error("Creating data set type failed", countResult.cause());
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                }
            });
        });
    }

    public void getDataSetTypes(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {

            String federatedId = routingContext.request().getHeader(JsonKey.FEDERATED_ID_IN_HEADER);

            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);

            Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.NAME, JsonKey.CREATOR);

            String sql = "SELECT data_set_type.id, data_set_type.adder, data_set_type.description, data_set_type.name, array_to_string(array(SELECT data_set_type_subtypes.element FROM data_set_type_subtypes WHERE data_set_type.id=data_set_type_subtypes.datasettype_id), ',') AS elements FROM data_set_type";

            if (sort.isPresent()) {
                switch (sort.get()) {
                    case "-name":
                        sql = sql + " order by name desc";
                        break;
                    case "name":
                        sql = sql + " order by name asc";
                        break;
                    case "-creator":
                        sql = sql + " order by adder desc";
                        break;
                    case "creator":
                        sql = sql + " order by adder asc";
                        break;
                }
            }

            if (pair.getRight().isPresent()) {
                sql = sql + " offset " + pair.getRight().get();
            }

            if (StringUtils.isEmpty(federatedId)) {
                sql = sql + " limit " + pair.getLeft().get();
            }
            Route.vertxDBHelper.select(sql, result -> returnResponseWithCount(routingContext, HttpResponseStatus.OK, result, "SELECT count(*) FROM data_set_type"));
        });
    }

    public void getDataSetType(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.DATASET_TYPE_ID);

            String sql = "SELECT data_set_type.id, data_set_type.adder, data_set_type.description, data_set_type.name, array_to_string(array(SELECT data_set_type_subtypes.element FROM data_set_type_subtypes WHERE data_set_type.id=data_set_type_subtypes.datasettype_id), ',') AS elements FROM data_set_type WHERE id=?";

            Route.vertxDBHelper.getOne(sql, new JsonArray().add(map.get(UrlParam.DATASET_TYPE_ID)), result -> returnResponse(routingContext, HttpResponseStatus.OK, result));
        });
    }

    public void createDataSetType(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            JsonObject body = validateRequestBody(routingContext, JsonKey.NAME, JsonKey.DESCRIPTION, JsonKey.SUBTYPE, JsonKey.CREATOR);

            String name = body.getString(JsonKey.NAME);
            String description = body.getString(JsonKey.DESCRIPTION);
            JsonArray subtype = body.getJsonArray(JsonKey.SUBTYPE);
            String creator = body.getString(JsonKey.CREATOR);
            String typeId = UUID.randomUUID().toString();

            Route.vertxDBHelper.updateMultipleSqlInTransaction(finalResult -> {
                        if (finalResult.succeeded()) {
                            routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
                        } else {
                            logger.error("Creating data set type failed", finalResult.cause());
                            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                    .end(new JsonObject().put(JsonKey.DESCRIPTION, finalResult.cause().getMessage()).encodePrettily());
                        }
                    }, (connection, asyncResultHandler) ->
                            connection.updateWithParams(
                                    "INSERT INTO data_set_type (adder, description, name, type_id) VALUES (?, ?, ?, ?)",
                                    new JsonArray().add(creator).add(description).add(name).add(typeId),
                                    insertDataSetTypeResult -> {
                                        if (insertDataSetTypeResult.succeeded()) {
                                            long dataSetTypeId = insertDataSetTypeResult.result().getKeys().getLong(0);
                                            List<String> params = new ArrayList<>(subtype.size());
                                            subtype.stream().map(String.class::cast).forEach(element -> params.add("(" + dataSetTypeId + ",'" + element + "')"));

                                            connection.update(
                                                    "INSERT INTO data_set_type_subtypes (datasettype_id, element) VALUES " + StringUtils.join(params, ","),
                                                    insertDataSetTypeSubtypesResult -> {
                                                        if (insertDataSetTypeSubtypesResult.succeeded()) {
                                                            asyncResultHandler.handle(Future.succeededFuture());
                                                        } else {
                                                            asyncResultHandler.handle(Future.failedFuture(insertDataSetTypeSubtypesResult.cause()));
                                                        }
                                                    });
                                        } else {
                                            asyncResultHandler.handle(Future.failedFuture(insertDataSetTypeResult.cause()));
                                        }
                                    })
            );
        });
    }

    public void searchDataSetTypes(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);

            Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.NAME, JsonKey.CREATOR);

            JsonObject body = validateRequestBody(routingContext, JsonKey.QUERY);

            String query = body.getString(JsonKey.QUERY);

            StringBuilder builder = new StringBuilder();
            builder.append("SELECT data_set_type.id, data_set_type.adder, data_set_type.description, data_set_type.name, array_to_string(array(SELECT data_set_type_subtypes.element FROM data_set_type_subtypes WHERE data_set_type.id=data_set_type_subtypes.datasettype_id), ',') AS elements FROM data_set_type ");
            builder.append("WHERE LOWER(data_set_type.name) LIKE LOWER('%" + query + "%') OR LOWER(data_set_type.adder) LIKE LOWER('%" + query + "%') ");
            if (sort.isPresent()) {
                switch (sort.get()) {
                    case "-name":
                        builder.append("order by name desc");
                        break;
                    case "name":
                        builder.append("order by name asc");
                        break;
                    case "-creator":
                        builder.append("order by adder desc");
                        break;
                    case "creator":
                        builder.append("order by adder asc");
                        break;
                    default:
                        builder.append("order by name");
                }
            } else {
                builder.append("ORDER BY name");
            }
            if (pair.getRight().isPresent()) {
                builder.append(" offset ").append(pair.getRight().get());
            }
            builder.append(" limit ").append(pair.getLeft().get());

            String countSql = "SELECT count(*) FROM data_set_type WHERE LOWER(name) LIKE LOWER('%" + query + "%') OR LOWER(adder) LIKE LOWER('%" + query + "%')";

            Route.vertxDBHelper.select(builder.toString(), result -> returnResponseWithCount(routingContext, HttpResponseStatus.OK, result, countSql));
        });
    }
}
