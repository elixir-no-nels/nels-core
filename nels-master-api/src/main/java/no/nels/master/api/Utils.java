package no.nels.master.api;

import io.vertx.ext.web.RoutingContext;

/**
 * Created by weizhang on 1/9/17.
 */
public class Utils {
    public static long getId(RoutingContext routingContext) {
        long id;
        try {
            id = Long.valueOf(routingContext.request().getParam("id"));
        } catch (NumberFormatException ex) {
            id = -1;

        }
        return id;
    }
}