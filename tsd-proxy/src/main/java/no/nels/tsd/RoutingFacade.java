package no.nels.tsd;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.ext.web.RoutingContext;
import no.nels.tsd.constants.ConfigConstant;
import no.nels.tsd.constants.JsonConstant;
import no.nels.tsd.constants.UrlParamConstant;

import java.nio.file.FileSystems;
import java.util.Vector;

public final class RoutingFacade {
    private static Logger logger = LoggerFactory.getLogger(RoutingFacade.class);

    public static void connectTo(RoutingContext routingContext) {
        String userName = routingContext.request().getParam(UrlParamConstant.USER_NAME);
        logger.debug("Tsd User Name: " + userName);
        JsonObject requestJsonBody = routingContext.getBodyAsJson();
        String password = requestJsonBody.getString(JsonConstant.PASSWORD);
        String otc = requestJsonBody.getString(JsonConstant.OTC);

        try {
            String reference = TsdSessionManager.getManager().createTsdSession(userName, password, otc, Config.valueOf(ConfigConstant.TSD_HOST), Integer.parseInt(Config.valueOf(ConfigConstant.TSD_PORT)));
            logger.debug("Reference: " + reference);
            routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end(reference);
        } catch (JSchException e) {
            logger.error(e);
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(e.getMessage());
        }
    }

    public static void disconnectTsdSession(RoutingContext routingContext) {
        String reference = routingContext.request().getHeader("reference");
        String userName = routingContext.request().getParam(UrlParamConstant.USER_NAME);

        TsdSessionManager.getManager().removeTsdSession(userName, reference);
        routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
    }

    public static void navigateTo(RoutingContext routingContext) {

        String reference = routingContext.request().getHeader("reference");
        String userName = routingContext.request().getParam("param0");

        logger.debug("Tsd User Name: " + userName);
        StringBuilder path = new StringBuilder();
        path.append(FileSystems.getDefault().getSeparator()).append(userName.split("-")[0]).append(FileSystems.getDefault().getSeparator())
                .append(routingContext.request().getParam("param1"));

        logger.debug("Navigate to path " + path);
        navigateToFolder(routingContext, reference, userName, path);
    }

    public static void navigateToHome(RoutingContext routingContext) {
        String userName = routingContext.request().getParam(UrlParamConstant.USER_NAME);
        String reference = routingContext.request().getHeader("reference");

        StringBuilder path = new StringBuilder();
        path.append(FileSystems.getDefault().getSeparator()).append(userName.split("-")[0]);

        navigateToFolder(routingContext, reference, userName, path);
    }

    private static void navigateToFolder(RoutingContext routingContext, String reference, String userName, StringBuilder path) {
        ChannelSftp channelSftp = null;
        try {
            channelSftp = TsdSessionManager.getManager().openChannel(userName, reference);
            Vector<ChannelSftp.LsEntry> entryVector = channelSftp.ls(path.toString());

            JsonArray jsonArray = new JsonArray();
            entryVector.stream().filter(entry -> !entry.getFilename().startsWith(".")).forEach(entry -> {
                JsonObject jsonObject = new JsonObject();
                jsonObject.put("name", entry.getFilename());
                jsonObject.put("size", entry.getAttrs().getSize());
                if (entry.getAttrs().isDir()) {
                    jsonObject.put("type", "folder");
                } else {
                    jsonObject.put("type", "file");
                }
                jsonArray.add(jsonObject);
            });

            routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end(jsonArray.encode());
        } catch (JSchException | SftpException | TsdException e) {
            logger.error(e);
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(e.getMessage());
        } finally {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
        }
    }
}
