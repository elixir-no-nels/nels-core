package no.nels.master.api.facades;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import no.nels.master.api.Utils;
import no.nels.master.api.db.DAOService;

import java.util.Optional;

/**
 * Created by weizhang on 12/7/16.
 */
public class SettingFacade {
    private static Logger logger = LoggerFactory.getLogger(SettingFacade.class);

    public static void getIds(RoutingContext routingContext) {
        DAOService.getInstance().settingIds(result -> routingContext.response().putHeader("content-type", "application/json").end(result));
    }

    public static void getSettingById(RoutingContext routingContext) {
        long id = Utils.getId(routingContext);
        if (id < 0) {
            logger.error("invalid id");
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
            return;
        }

        DAOService.getInstance().querySetting(Long.valueOf(id), result -> {
            if (null != result) {
                routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).putHeader("content-type", "application/json").end(result);
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
            }
        });
    }

    public static void getCount(RoutingContext routingContext) {
        DAOService.getInstance().getDbHelper().count("select count(*) from setting", result -> {
            if (result.succeeded()) {
                routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).putHeader("content-type", "application/json")
                        .end(new JsonObject().put("count", result.result()).encodePrettily());
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });
    }

    public static void getSettings(RoutingContext routingContext) {
        Optional<JsonObject> body = Optional.ofNullable(routingContext.getBodyAsJson());
        Optional<String> key = body.map(x -> x.getString("key"));
        if (key.isPresent()) {
            DAOService.getInstance().querySettings(key.get(), res -> {
                if (null != res) {
                    routingContext.response().putHeader("content-type", "application/json").end(res);
                } else {
                    routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
                }
            });
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
            logger.error("Some json fields are missing.");
        }

    }

    public static void getAllSettings(RoutingContext routingContext) {
        DAOService.getInstance().settings(result -> routingContext.response().putHeader("content-type", "application/json").end(result));
    }

    public static void deleteSettingById(RoutingContext routingContext) {

        long id = Utils.getId(routingContext);
        if (id < 0) {
            logger.error("invalid id");
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
            return;
        }

        DAOService.getInstance().deleteSetting(id, res -> {
            if (null != res) {
                routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end(res);
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
            }
        });
    }

    public static void updateSettingById(RoutingContext routingContext) {
        long id = Utils.getId(routingContext);
        if (id < 0) {
            logger.error("invalid id");
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
            return;
        }

        Optional<JsonObject> body = Optional.ofNullable(routingContext.getBodyAsJson());
        logger.debug("json body: " + body.map(x -> x.toString()));
        Optional<String> key = body.map(x -> x.getString("key"));
        Optional<String> value = body.map(x -> x.getString("value"));

        if (!key.isPresent() && !value.isPresent()) {
            logger.error("Some json fields are missing.");
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
            return;
        }

        DAOService.getInstance().updateSetting(id, key, value, result -> {
            /*
             * raw result:
             * {
             *       "keys" : [ 17 ],
             *       "updated" : 1
             * }
             */
            JsonObject jsonObject = new JsonObject(result);
            if (jsonObject.getLong("updated") == 1) {
                routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
            }
        });

    }



    public static void manipulateSetting(RoutingContext routingContext) {
        Optional<JsonObject> body = Optional.ofNullable(routingContext.getBodyAsJson());
        logger.debug("json body: " + body.map(x -> x.toString()));
        Optional<String> key = body.map(x -> x.getString("key"));
        Optional<String> value = body.map(x -> x.getString("value"));
        Optional<String> method = body.map(x -> x.getString("method"));
        if (!key.isPresent() && !method.isPresent()) {
            logger.error("Some json fields are missing.");
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
            return;
        }
        if(method.get().equalsIgnoreCase("update") && value.isPresent()){
            DAOService.getInstance().updateSetting(key.get(), value.get(), result -> {
                JsonObject jsonObject = new JsonObject(result);
                if (jsonObject.getLong("updated") == 1) {
                    routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
                } else {
                    routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
                }
            });
        }else if(method.get().equalsIgnoreCase("delete")){
            DAOService.getInstance().deleteSetting(key.get(), res -> {
                if (null != res) {
                    routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
                } else {
                    routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
                }
            });
        }else {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
            return;
        }
    }



    public static void addSetting(RoutingContext routingContext) {


        Optional<JsonObject> body = Optional.ofNullable(routingContext.getBodyAsJson());
        logger.debug("json body: " + body.map(x -> x.toString()));
        Optional<String> key = body.map(x -> x.getString("key"));
        Optional<String> value = body.map(x -> x.getString("value"));


        if (!key.isPresent() || !value.isPresent()) {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
            logger.error("Some json fields are missing.");
            return;
        }

        DAOService.getInstance().insertSetting(key.get(), value.get(), result -> {
            if (null != result) {
                routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end(result.toString());
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });

    }

}
