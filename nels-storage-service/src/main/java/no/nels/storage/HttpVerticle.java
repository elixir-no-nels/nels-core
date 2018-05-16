package no.nels.storage;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import no.nels.storage.constants.ConfigName;

public class HttpVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(HttpVerticle.class);

    @Override
    public void start() throws Exception {
        HttpServer httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);

        router.route().consumes("application/json").handler(BodyHandler.create());

        router.route().pathRegex("\\/(users|projects)\\/.").handler(BasicAuthHandler.create(new BasicHttpAuthProvider()));

        router.route("/").produces("application/json").handler(routingContext -> {
            JsonObject jsonObject = new JsonObject();
            jsonObject.put("version", 1);
            HttpServerResponse response = routingContext.response();
            response.end(jsonObject.encode());

        });

        router.get("/download/:reference").blockingHandler(HttpHandlerFacade::downloadHandler, false);

        router.post("/upload/:reference").handler(HttpHandlerFacade::uploadFileHandler);

        router.route("/users/:nelsId").handler(HttpHandlerFacade::checkNelsIdHandler);

        router.post("/users/:nelsId/download/reference").handler(HttpHandlerFacade::createDownloadReferenceHandler);

        router.post("/users/:nelsId/upload/reference").handler(HttpHandlerFacade::createUploadReferenceHandler);

        router.post("/users/:nelsId/list").blockingHandler(HttpHandlerFacade::navigationHandler, false);

        router.get("/users/:nelsId").handler(HttpHandlerFacade::getUserSshInfoHandler);

        router.post("/users/:nelsId/delete").blockingHandler(HttpHandlerFacade::deleteHandler, false);

        router.post("/users/:nelsId/rename").handler(HttpHandlerFacade::renameHandler);

        router.post("/users/:nelsId/create").handler(HttpHandlerFacade::createFolderHandler);

        router.put("/users/:nelsId").blockingHandler(HttpHandlerFacade::createUserHandler, false);

        router.delete("/users/:nelsId").blockingHandler(HttpHandlerFacade::deleteUserHandler, false);

        router.post("/projects/:projectId/rename").blockingHandler(HttpHandlerFacade::renameProjectHandler, false);

        router.put("/projects/:projectId").blockingHandler(HttpHandlerFacade::createProjectHandler, false);

        router.delete("/projects/:projectId").blockingHandler(HttpHandlerFacade::deleteProjectHandler, false);

        router.put("/projects/:projectId/users/:nelsId").blockingHandler(HttpHandlerFacade::addUserToProjectHandler, false);

        router.delete("/projects/:projectId/users/:nelsId").blockingHandler(HttpHandlerFacade::deleteUserInProjectHandler, false);

        httpServer.requestHandler(router::accept).listen(
                Integer.parseInt(Config.valueOf(ConfigName.PORT)), Config.valueOf(ConfigName.HOST));
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }
}
