package no.norstore.storebioinfo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import no.nels.vertx.commons.db.DBHelper;
import no.nels.vertx.commons.db.VertxDBHelper;
import no.norstore.storebioinfo.constants.ConfigName;
import no.norstore.storebioinfo.handlers.DataSetTypeRequestHandler;
import no.norstore.storebioinfo.handlers.ProjectRequestHandler;
import no.norstore.storebioinfo.handlers.QuotaRequestHandler;
import no.norstore.storebioinfo.handlers.UserRequestHandler;
import no.norstore.storebioinfo.sec.BasicHttpAuthProvider;
import no.norstore.storebioinfo.utils.IrodsUtils;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

public final class Route extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(Route.class);
    public static VertxDBHelper vertxDBHelper;

    public static HttpClient httpClient;


    public void routeQuota(Router router) {
        //quota
        QuotaRequestHandler quotaRequestHandler = new QuotaRequestHandler();
        router.get("/quotas").produces(MediaType.APPLICATION_JSON).handler(quotaRequestHandler::getAllQuotas);
        router.post("/quotas/do").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).blockingHandler(quotaRequestHandler::doQuotas, false);
        router.post("/quotas/query").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(quotaRequestHandler::search);
        router.post("/quotas").produces(MediaType.APPLICATION_JSON).consumes(MediaType.APPLICATION_JSON).handler(quotaRequestHandler::createQuota);
        router.put("/quotas/:quotaId").consumes(MediaType.APPLICATION_JSON).handler(quotaRequestHandler::updateQuota);
        router.delete("/quotas/:quotaId").handler(quotaRequestHandler::deleteQuota);
        router.get("/quotas/:quotaId").produces(MediaType.APPLICATION_JSON).handler(quotaRequestHandler::getQuota);
        router.get("/quotas/:quotaId/projects").produces(MediaType.APPLICATION_JSON).handler(quotaRequestHandler::getProjectsInQuota);
    }

    public void routeProject(Router router) {
        ProjectRequestHandler projectRequestHandler = new ProjectRequestHandler();
        router.get("/projects").produces(MediaType.APPLICATION_JSON).handler(projectRequestHandler::getProjects);
        router.post("/projects").produces(MediaType.APPLICATION_JSON).consumes(MediaType.APPLICATION_JSON).handler(projectRequestHandler::createProject);
        router.post("/projects/query").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(projectRequestHandler::searchProjects);
        router.post("/projects/do").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(projectRequestHandler::doAction);
        router.put("/projects/:projectId").consumes(MediaType.APPLICATION_JSON).handler(projectRequestHandler::updateProject);
        router.get("/projects/:projectId").produces(MediaType.APPLICATION_JSON).handler(projectRequestHandler::getProject);
        router.post("/projects/:projectId").handler(projectRequestHandler::deleteProject);
        router.post("/projects/:projectId/users/do").consumes(MediaType.APPLICATION_JSON).blockingHandler(projectRequestHandler::changeProjectMembers, false);
        router.get("/projects/:projectId/users").produces(MediaType.APPLICATION_JSON).handler(projectRequestHandler::getProjectMembers);
        router.post("/projects/:projectId/datasets").consumes(MediaType.APPLICATION_JSON).blockingHandler(projectRequestHandler::createDataset, false);
        router.get("/projects/:projectId/datasets").produces(MediaType.APPLICATION_JSON).handler(projectRequestHandler::getDataSets);
        router.get("/projects/:projectId/datasets/:dataSetId").produces(MediaType.APPLICATION_JSON).handler(projectRequestHandler::getDataSet);
        router.delete("/projects/:projectId/datasets/:dataSetId").blockingHandler(projectRequestHandler::deleteDataSet, false);
        router.get("/projects/:projectId/datasets/:dataSetId/subtypes").produces(MediaType.APPLICATION_JSON).handler(projectRequestHandler::getSubtypes);
        router.get("/projects/:projectId/datasets/:dataSetId/subtypes/:subtypeId").produces(MediaType.APPLICATION_JSON).handler(projectRequestHandler::getSubtype);
        router.getWithRegex("\\/projects\\/([^\\/]+)\\/datasets\\/([^\\/]+)\\/subtypes\\/([^\\/]+)\\/(.+)").produces(MediaType.APPLICATION_JSON).handler(projectRequestHandler::getContent);
    }

    public void routeUsers(Router router){
        UserRequestHandler userRequestHandler = new UserRequestHandler();
        router.get("/users").produces(MediaType.APPLICATION_JSON).handler(userRequestHandler::getUsers);
        router.post("/users").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(userRequestHandler::createUser);
        router.post("/users/query").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(userRequestHandler::searchUsers);
    }

    public void routeDatasetTypes(Router router){
        DataSetTypeRequestHandler datasetTypeRequestHandler = new DataSetTypeRequestHandler();
        router.get("/datasettypes").produces(MediaType.APPLICATION_JSON).handler(datasetTypeRequestHandler::getDataSetTypes);
        router.post("/datasettypes").consumes(MediaType.APPLICATION_JSON).handler(datasetTypeRequestHandler::createDataSetType);
        router.delete("/datasettypes/:dataSetTypeId").handler(datasetTypeRequestHandler::deleteDataSetType);
        router.get("/datasettypes/:dataSetTypeId").produces(MediaType.APPLICATION_JSON).handler(datasetTypeRequestHandler::getDataSetType);
        router.post("/datasettypes/query").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(datasetTypeRequestHandler::searchDataSetTypes);
    }

    @Override
    public void start() throws Exception {
        try {
            vertxDBHelper = new VertxDBHelper(vertx, Config.valueOf(ConfigName.DB_URL), Config.valueOf(ConfigName.DB_USER), Config.valueOf(ConfigName.DB_DRIVER), Config.valueOf(ConfigName.DB_PASSWORD), 30);
            DBHelper.init( Config.valueOf(ConfigName.DB_DRIVER), Config.valueOf(ConfigName.DB_URL), Config.valueOf(ConfigName.DB_USER), Config.valueOf(ConfigName.DB_PASSWORD));
        } catch (IOException e) {
            logger.error(e);
            throw e;
        }
        httpClient = vertx.createHttpClient();
        HttpServer httpServer = vertx.createHttpServer();

        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        router.route().handler(BasicAuthHandler.create(new BasicHttpAuthProvider()));


        router.get("/version").handler(
                routingContext -> routingContext.response().end("version: 1")
        );

        router.get("/storage/version").handler(
                routingContext -> {
                    routingContext.response().end(IrodsUtils.getVersion());
                }
        );

        routeQuota(router);
        routeUsers(router);
        routeDatasetTypes(router);
        routeProject(router);

        httpServer.requestHandler(router::accept).listen(
                Integer.parseInt(Config.valueOf(ConfigName.PORT)),
                Config.valueOf(ConfigName.HOST));
    }
}
