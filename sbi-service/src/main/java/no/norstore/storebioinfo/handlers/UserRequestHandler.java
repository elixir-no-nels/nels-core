package no.norstore.storebioinfo.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import no.norstore.storebioinfo.Route;
import no.norstore.storebioinfo.constants.JsonKey;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public final class UserRequestHandler implements HttpRequestHandler {
    private static Logger logger = LoggerFactory.getLogger(UserRequestHandler.class);

    public void searchUsers(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            JsonObject jsonObject = validateRequestBody(routingContext, JsonKey.FEDERATED_ID);

            JsonArray federatedIds = jsonObject.getJsonArray(JsonKey.FEDERATED_ID);
            int size = federatedIds.size();
            List<String> federatedIdLst = new ArrayList<>();
            String s;
            for (int i = 0; i < size; i++) {
                s = federatedIds.getString(i);
                federatedIdLst.add(s);
            }


            String sql = "select user_id, email, username, federated_id from esysbio_user where federated_id in (" + String.join(",", Collections.nCopies(federatedIdLst.size(), "?")) + ")";
            JsonArray params = new JsonArray(federatedIdLst);
            Route.vertxDBHelper.select(sql, params, res -> returnResponse(routingContext, HttpResponseStatus.OK, res));
        });
    }

    public void getUsers(RoutingContext routingContext) {

        validateRequest(routingContext, () -> {
            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);

            StringBuilder builder = new StringBuilder();
            builder.append("SELECT user_id, federated_id, firstname, surname, email FROM esysbio_user order by email");

            if (pair.getRight().isPresent()) {
                builder.append(" offset ");
                builder.append(pair.getRight().get());
            }
            builder.append(" limit ").append(pair.getLeft().get());

            Route.vertxDBHelper.select(builder.toString(), result -> returnResponseWithCount(routingContext, HttpResponseStatus.OK, result, "SELECT count(*) FROM esysbio_user"));
        });
    }

    public void createUser(RoutingContext routingContext) {

        validateRequest(routingContext, () -> {
            JsonObject body = validateRequestBody(routingContext, JsonKey.NAME, JsonKey.EMAIL, JsonKey.FEDERATED_ID);
            String name = body.getString(JsonKey.NAME);
            String email = body.getString(JsonKey.EMAIL);
            String federatedId = body.getString(JsonKey.FEDERATED_ID);

            Route.vertxDBHelper.existOne("select * from esysbio_user where email = ? and federated_id = ?",
                    new JsonArray().add(email).add(federatedId), booleanAsyncResult -> {
                        String s = "";
                        if(booleanAsyncResult.succeeded() && !booleanAsyncResult.result()) {
                            Route.vertxDBHelper.updateMultipleSqlInTransaction(finalAsyncResult -> {
                                if (finalAsyncResult.succeeded()) {
                                    routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
                                } else {
                                    logger.error("", finalAsyncResult.cause());
                                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                                }
                            }, (sqlConnection, result) -> {
                                String extRef = DigestUtils.shaHex(System.currentTimeMillis() + federatedId);
                                String insertToUser = "INSERT INTO esysbio_user (user_id, confirmationcode, email, externalref, firstname, limited_profile, password, salt, surname, username, valid, federated_id) VALUES " +
                                        "((select max(user_id) from esysbio_user ) + 1,  '', '" + email + "', '" + extRef + "', '" + name + "', true, '', '', '', '" + federatedId + "',  true, '" + federatedId + "')";

                                sqlConnection.update(insertToUser, insertToUserResult -> {
                                    if (insertToUserResult.succeeded()) {
                                        final long userId = insertToUserResult.result().getKeys().getLong(0);

                                        String insertToUserSystemRole = "INSERT INTO public.user_system_role (user_id, role_id) VALUES (?, ?)";

                                        sqlConnection.updateWithParams(insertToUserSystemRole, new JsonArray().add(userId).add(10), insertToUserSystemRoleResult -> {
                                            if (insertToUserSystemRoleResult.succeeded()) {

                                                String insertToMessageBoard = "INSERT INTO messageboard (id, externalref, user_id) " +
                                                        "VALUES ((select max(id) from messageboard) + 1, '" + UUID.randomUUID().toString() + "', " + userId + ")";

                                                sqlConnection.update(insertToMessageBoard, insertToMessageBoardResult -> {
                                                    if (insertToMessageBoardResult.succeeded()) {
                                                        final long messageboardId = insertToMessageBoardResult.result().getKeys().getLong(0);
                                                        String text = "Welcome to Storebioinfo!\n\nWithin the system you can add datasets to your Life Sciences projects you either own or have been invited to join. To get an introduction to the system, click on the ''Help Pages'' link on the top navigation bar.";
                                                        String insertToMessage = "INSERT INTO message (id, confirmationcode, created, createdby, externalref, issuer_id, \"type\", recipient, subject, messageboard_id, project_id, resource_id, resource_type, text) " +
                                                                "VALUES ((select max(id) from message) + 1, '', now(), 'Storebioinfo', '" + UUID.randomUUID().toString() + "', 'Storebioinfo system', 'SYSTEM_MESSAGE', '', 'Welcome to storebioinfo', " + messageboardId + ", '', '', '', '" + text + "')";

                                                        sqlConnection.update(insertToMessage, insertToMessageResult -> {
                                                            if (insertToMessageResult.succeeded()) {
                                                                result.handle(Future.succeededFuture());
                                                            } else {
                                                                result.handle(Future.failedFuture(insertToMessageResult.cause()));
                                                            }
                                                        });
                                                    } else {
                                                        result.handle(Future.failedFuture(insertToMessageBoardResult.cause()));
                                                    }
                                                });

                                            } else {
                                                result.handle(Future.failedFuture(insertToUserSystemRoleResult.cause()));
                                            }
                                        });
                                    } else {
                                        result.handle(Future.failedFuture(insertToUserResult.cause()));
                                    }
                                });
                            });

                        }else {
                            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                                    .end(new JsonObject().put(JsonKey.DESCRIPTION, new JsonArray().add("User already exists")).encodePrettily());
                        }
                    });
        });
    }
}
