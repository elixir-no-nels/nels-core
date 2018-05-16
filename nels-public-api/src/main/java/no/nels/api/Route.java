package no.nels.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import no.nels.api.constants.ConfigName;
import no.nels.api.facades.StatisticsFacade;
import no.nels.api.handlers.DashboardHandler;
import no.nels.api.handlers.LogHandler;
import no.nels.api.handlers.NeLSHandler;
import no.nels.api.handlers.SbiHandler;
import no.nels.api.oauth.OAuthIntrospectionProvider;
import no.nels.api.sec.BearerAuthHandler;
import no.nels.vertx.commons.db.DBHelper;
import no.nels.vertx.commons.db.VertxDBHelper;

import javax.ws.rs.core.MediaType;

public final class Route extends AbstractVerticle {

    public static VertxDBHelper vertxDBHelper;
    private static Logger logger = LoggerFactory.getLogger(Route.class);


    public void routeSBIQuotas(Router router, SbiHandler sbiHandler) {
        router.getWithRegex("\\/sbi\\/quotas\\/all").produces(MediaType.APPLICATION_JSON).handler(sbiHandler::getSbiQuotas);
        router.post("/sbi/quotas/query").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(sbiHandler::querySbiQuotas);
        router.post("/sbi/quotas").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(sbiHandler::createSbiQuota);
        router.put("/sbi/quotas/:quotaId").consumes(MediaType.APPLICATION_JSON).handler(sbiHandler::updateSbiQuota);
        router.delete("/sbi/quotas/:quotaId").handler(sbiHandler::deleteSbiQuota);
        router.get("/sbi/quotas/:quotaId").produces(MediaType.APPLICATION_JSON).handler(sbiHandler::getSbiQuota);
        router.get("/sbi/quotas/:quotaId/projects").produces(MediaType.APPLICATION_JSON).handler(sbiHandler::getSbiProjectsInQuota);

    }

    public void routeStructureLogs(Router router) {
        LogHandler logHandlerFacade = new LogHandler();
        router.get("/logs/contexts/:contextId").produces(MediaType.APPLICATION_JSON).handler(logHandlerFacade::getLogs);
        router.post("/logs/add").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(logHandlerFacade::addLog);
        router.get("/sbi/blockquota").produces(MediaType.APPLICATION_JSON).handler(logHandlerFacade::getSbiBlockQuota);
        router.post("/sbi/blockquota").consumes(MediaType.APPLICATION_JSON).handler(logHandlerFacade::updateSbiBlockQuota);
    }

    public void routeNeLSUser(Router router) {
        NeLSHandler nelsHandlerFacade = new NeLSHandler();
        router.get("/user-info").produces(MediaType.APPLICATION_JSON).handler(nelsHandlerFacade::getUserInfo);
        router.get("/nels/users").produces(MediaType.APPLICATION_JSON).handler(nelsHandlerFacade::getNelsUsers);
        router.post("/nels/users/query").produces(MediaType.APPLICATION_JSON).handler(nelsHandlerFacade::searchNelsUsers);
    }

    public void routeNeLSProject(Router router) {
        NeLSHandler nelsHandlerFacade = new NeLSHandler();
        router.get("/nels/projects/all").produces(MediaType.APPLICATION_JSON).handler(nelsHandlerFacade::getNelsAllProjects);
        router.get("/nels/projects").produces(MediaType.APPLICATION_JSON).handler(nelsHandlerFacade::getNelsProjects);
        router.post("/nels/projects/query").produces(MediaType.APPLICATION_JSON).handler(nelsHandlerFacade::searchNelsProjects);
    }

    public void routeGeneric(Router router) {
        router.route().handler(new BearerAuthHandler(new OAuthIntrospectionProvider(vertx.createHttpClient(new HttpClientOptions().setSsl(Boolean.valueOf(Config.valueOf(no.nels.commons.constants.ConfigName.OAUTH_SSL))).setTrustAll(true)))));
        router.get("/version").produces(MediaType.APPLICATION_JSON).handler(routingContext -> {
            JsonObject jsonObject = new JsonObject();
            jsonObject.put("version", 1);
            routingContext.response().end(jsonObject.encode());
        });
    }

    public void routeSeek(Router router, SbiHandler sbiHandler) {
        router.get("/seek/sbi/projects/:projectId/datasets/:datasetId/:subtypeName/metadata").handler(sbiHandler::downloadSbiMetadata);
        router.post("/seek/sbi/projects/:projectId/datasets/:datasetId/:subtypeName/metadata").handler(sbiHandler::uploadSbiMetadata);
        router.delete("/seek/sbi/projects/:projectId/datasets/:datasetId/:subtypeName/metadata").handler(sbiHandler::deleteSbiMetadata);
        router.post("/seek/sbi/projects/:projectId/datasets/:datasetId/:subtypeName/metadata/do").consumes(MediaType.APPLICATION_JSON).handler(sbiHandler::doActionInSbiMetadata);
    }

    public void routeSbiProjects(Router router, SbiHandler sbiHandler) {
        router.postWithRegex("\\/sbi\\/projects\\/([0-9]+)\\/datasets\\/([0-9]+)\\/list").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(sbiHandler::navigateInSubtype);
        router.postWithRegex("\\/sbi\\/projects\\/([0-9]+)\\/datasets\\/([0-9]+)\\/do").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(sbiHandler::doAction);
        router.get("/sbi/projects/:projectId/datasets/:datasetId").produces(MediaType.APPLICATION_JSON).handler(sbiHandler::getSbiProjectDataset);
        router.delete("/sbi/projects/:projectId/datasets/:datasetId").blockingHandler(sbiHandler::deleteSbiProjectDataSet);
        router.get("/sbi/projects/:projectId/datasets").produces(MediaType.APPLICATION_JSON).handler(sbiHandler::getSbiProjectDatasets);
        router.get("/sbi/projects/:projectId").produces(MediaType.APPLICATION_JSON).handler(sbiHandler::getSbiProject);
        router.delete("/sbi/projects/:projectId").handler(sbiHandler::deleteSbiProject);
        router.get("/sbi/projects/all").produces(MediaType.APPLICATION_JSON).handler(sbiHandler::getSbiAllProjects);
        router.post("/sbi/projects/all/query").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(sbiHandler::querySbiProjects);
        router.get("/sbi/projects").produces(MediaType.APPLICATION_JSON).handler(sbiHandler::getSbiProjects);
        router.post("/sbi/projects").produces(MediaType.APPLICATION_JSON).consumes(MediaType.APPLICATION_JSON).handler(sbiHandler::createSbiProject);
        router.put("/sbi/projects/:projectId").consumes(MediaType.APPLICATION_JSON).handler(sbiHandler::updateSbiProject);
        router.post("/sbi/projects/:projectId/users/do").consumes(MediaType.APPLICATION_JSON).blockingHandler(sbiHandler::changeProjectMembers, false);
        router.get("/sbi/projects/:projectId/users").produces(MediaType.APPLICATION_JSON).handler(sbiHandler::getProjectMembers);
    }

    public void routeSbiUsers(Router router, SbiHandler sbiHandler) {
        router.get("/sbi/users").produces(MediaType.APPLICATION_JSON).handler(sbiHandler::getSbiUsers);
        router.post("/sbi/users").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(sbiHandler::createSbiUser);
        router.get("/sbi/sample-metadata").handler(sbiHandler::downloadSbiSampleMetaData);
        router.post("/sbi/users/query").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(sbiHandler::querySbiUsers);
    }

    public void routeSbiDatasetTypes(Router router, SbiHandler sbiHandler) {
        router.get("/sbi/datasettypes").produces(MediaType.APPLICATION_JSON).handler(sbiHandler::getSbiDataSetTypes);
        router.get("/sbi/datasettypes/:dataSetTypeId").produces(MediaType.APPLICATION_JSON).handler(sbiHandler::getSbiDataSetType);
        router.delete("/sbi/datasettypes/:dataSetTypeId").handler(sbiHandler::deleteSbiDataSetType);
        router.post("/sbi/datasettypes").consumes(MediaType.APPLICATION_JSON).handler(sbiHandler::createSbiDataSetType);
        router.post("/sbi/datasettypes/query").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(sbiHandler::querySbiDataSetTypes);
    }

    public void routeDashboard(Router router) {
        DashboardHandler dashboardHandler = new DashboardHandler();
        router.get("/dashboard/").produces(MediaType.APPLICATION_JSON).handler(dashboardHandler::getDashboardInfo);
    }

    public void initDBHelper() throws Exception {
        try {
            vertxDBHelper = new VertxDBHelper(vertx, Config.valueOf(no.nels.vertx.commons.constants.ConfigName.DB_URL),
                    Config.valueOf(no.nels.vertx.commons.constants.ConfigName.DB_USER),
                    Config.valueOf(no.nels.vertx.commons.constants.ConfigName.DB_DRIVER_CLASS),
                    Config.valueOf(no.nels.vertx.commons.constants.ConfigName.DB_PASSWORD), 30);

            DBHelper.init(Config.valueOf(no.nels.vertx.commons.constants.ConfigName.DB_DRIVER_CLASS), Config.valueOf(no.nels.vertx.commons.constants.ConfigName.DB_URL), Config.valueOf(no.nels.vertx.commons.constants.ConfigName.DB_USER), Config.valueOf(no.nels.vertx.commons.constants.ConfigName.DB_PASSWORD));
        } catch (Exception e) {
            logger.error(e);
            throw e;
        }
    }

    @Override
    public void start() throws Exception {

        initDBHelper();

        HttpServer httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route().consumes(MediaType.APPLICATION_JSON).handler(BodyHandler.create());
        //generic
        routeGeneric(router);
        routeDashboard(router);
        //nels
        routeNeLSUser(router);
        routeNeLSProject(router);
        routeStructureLogs(router);
        //sbi
        SbiHandler sbiHandler = new SbiHandler();
        routeSeek(router, sbiHandler);
        routeSbiProjects(router, sbiHandler);
        routeSBIQuotas(router, sbiHandler);
        routeSbiUsers(router, sbiHandler);
        routeSbiDatasetTypes(router, sbiHandler);

        httpServer.requestHandler(router::accept).listen(
                Integer.parseInt(Config.valueOf(ConfigName.PORT)), Config.valueOf(ConfigName.HOST));
    }
}
