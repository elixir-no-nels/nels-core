package no.nels.storage;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.RoutingContext;
import no.nels.storage.constants.ConfigName;
import no.nels.storage.constants.JsonConstant;
import no.nels.storage.constants.UrlParamName;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class HttpHandlerFacade {
    private static Logger logger = LoggerFactory.getLogger(HttpHandlerFacade.class);

    private static Map<String, String> downloadReference = new HashMap<>();

    private static Map<String, String> uploadReference = new HashMap<>();

    public static void navigationHandler(RoutingContext routingContext) {
        String nelsId = routingContext.request().getParam(UrlParamName.NELS_ID);

        JsonObject requestJsonBody = routingContext.getBodyAsJson();
        if (requestJsonBody == null) {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        } else {
            String relativePath = requestJsonBody.getString(JsonConstant.PATH);

            String nelsName = Utils.nelsIdToNelsName(nelsId);

            String absolutePath;
            if (StringUtils.isEmpty(relativePath)) {
                absolutePath = StringUtils.join(new String[]{Config.valueOf(ConfigName.USER_ROOT), nelsName}, FileSystems.getDefault().getSeparator());
            } else {
                absolutePath = StringUtils.join(new String[]{Config.valueOf(ConfigName.USER_ROOT), nelsName, relativePath}, FileSystems.getDefault().getSeparator());
            }

            logger.debug(StringUtils.join("Nels user ", nelsName, " wants to navigate to ", absolutePath));


            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(Config.valueOf(ConfigName.PYTHON_PATH), Config.valueOf(ConfigName.FOLDER_SIZE_SCRIPT), "-c", absolutePath);


            try {
                Process process = processBuilder.start();

                String errorOutput = Utils.output(process.getErrorStream());
                if (!StringUtils.isEmpty(errorOutput)) {
                    logger.error(errorOutput);
                }
                String result = Utils.output(process.getInputStream());
                result = result.substring(result.indexOf("{"));
                logger.debug(result);
                if (process.waitFor() != 0) {
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                } else {
                    JsonObject jsonObject = new JsonObject(result);
                    logger.debug(jsonObject.encode());


                    JsonArray response = new JsonArray();

                    for (File f : dirListByName(absolutePath)) {
                        JsonObject item = new JsonObject();
                        if (f.isDirectory()) {
                            item.put(JsonConstant.SIZE, jsonObject.getLong(f.getName()));
                            item.put(JsonConstant.TYPE, JsonConstant.FOLDER);
                        } else {
                            item.put(JsonConstant.TYPE, JsonConstant.FILE);
                            item.put(JsonConstant.SIZE, f.length());
                        }
                        item.put(JsonConstant.NAME, f.getName());
                        item.put(JsonConstant.MODIFIED_TIME, f.lastModified());
                        response.add(item);
                    }
                    routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end(response.encode());

                }
            } catch (IOException | InterruptedException e) {
                logger.error(e.getMessage(), e.getCause());
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            } catch (Exception e) {
                logger.error(e.getMessage(), e.getCause());
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static File[] dirListByName(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.isDirectory()) {
            return null;
        }
        File files[] = folder.listFiles();
        Arrays.sort(files, new Comparator() {
            public int compare(final Object o1, final Object o2) {
                return ((File) o1).getName().toLowerCase().compareTo(((File) o2).getName().toLowerCase());
            }
        });
        return files;
    }

    public static void getUserSshInfoHandler(RoutingContext routingContext) {
        String nelsId = routingContext.request().getParam(UrlParamName.NELS_ID);
        logger.debug("Nels id: " + nelsId);

        String nelsName = Utils.nelsIdToNelsName(nelsId);
        logger.debug(StringUtils.join("Nels user ", nelsName, " wants ssh info"));

        String sshKeyFile = StringUtils.join(new String[]{Config.valueOf(ConfigName.USER_ROOT), nelsName, ".ssh", "nels"}, FileSystems.getDefault().getSeparator());

        JsonObject response = new JsonObject();
        try {
            response.put(JsonConstant.USER_NAME, nelsName).put(JsonConstant.SSH_HOST, Config.valueOf(ConfigName.SSH_HOST)).put(JsonConstant.SSH_KEY, new String(Files.readAllBytes(Paths.get(sshKeyFile))));
            routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end(response.encode());
        } catch (IOException e) {
            logger.error(e);
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
        }
    }

    public static void checkNelsIdHandler(RoutingContext routingContext) {
        String nelsId = routingContext.request().getParam(UrlParamName.NELS_ID);
        if (Utils.isNumeric(nelsId)) {
            routingContext.next();
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        }
    }

    public static void createDownloadReferenceHandler(RoutingContext routingContext) {
        addReference(routingContext, downloadReference::put);
    }

    public static void createUploadReferenceHandler(RoutingContext routingContext) {
        addReference(routingContext, uploadReference::put);
    }

    private static void addReference(RoutingContext routingContext, BiConsumer<String, String> consumer) {
        String nelsId = routingContext.request().getParam(UrlParamName.NELS_ID);
        logger.debug("Nels id: " + nelsId);
        JsonObject requestJsonBody = routingContext.getBodyAsJson();
        if (requestJsonBody == null) {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        } else {
            String relativePath = requestJsonBody.getString(JsonConstant.PATH);
            logger.debug("Relative Path: " + relativePath);

            String nelsName = Utils.nelsIdToNelsName(nelsId);
            logger.debug("Nels name: " + nelsName);

            String absolutePath;
            if (StringUtils.isEmpty(relativePath)) {
                absolutePath = StringUtils.join(new String[]{Config.valueOf(ConfigName.USER_ROOT), nelsName}, FileSystems.getDefault().getSeparator());
            } else {
                absolutePath = StringUtils.join(new String[]{Config.valueOf(ConfigName.USER_ROOT), nelsName, relativePath}, FileSystems.getDefault().getSeparator());
            }
            logger.debug("Absolute path: " + absolutePath);


            String key = UUID.randomUUID().toString();
            consumer.accept(key, absolutePath);
            routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end(key);
        }
    }

    public static void downloadHandler(RoutingContext routingContext) {

        String key = routingContext.request().getParam(UrlParamName.REFERENCE);
        if (downloadReference.containsKey(key)) {
            String absolutePath = downloadReference.get(key);
            logger.debug("The absolute path of downloading: " + absolutePath);
            downloadReference.remove(key);

            if (Files.isDirectory(Paths.get(absolutePath))) {

                String parentFolder = absolutePath.substring(absolutePath.lastIndexOf(FileSystems.getDefault().getSeparator()) + 1);
                HttpServerResponse response = routingContext.response();
                response.setWriteQueueMaxSize(2 * 4096);
                response.setChunked(true);
                response.putHeader("Content-Disposition", "attachment; filename=\""
                        + absolutePath.substring(absolutePath.lastIndexOf(FileSystems.getDefault().getSeparator()) + 1) + ".zip" + "\"");
                response.putHeader("Content-Type", "application/zip");

                Path folder = Paths.get(absolutePath);
                int bufferSize = 2 * 4096;
                byte[] bytes = new byte[bufferSize];
                AtomicBoolean isWritable = new AtomicBoolean(true);
                try (Stream<Path> filesStream = Files.walk(folder);
                     ZipOutputStream zipOutputStream = new ZipOutputStream(new DownloadFolderOutputStream(response, isWritable))) {
                    response.exceptionHandler(throwable ->
                            logger.error(throwable)).drainHandler(Void -> isWritable.set(true));

                    filesStream.filter(file -> !Files.isDirectory(file))
                            .forEach(file -> {


                                try (InputStream fileInputStream = new BufferedInputStream(new FileInputStream(file.toFile()), 5120)) {
                                    zipOutputStream.putNextEntry(new ZipEntry(StringUtils.join(parentFolder, FileSystems.getDefault().getSeparator(), folder.relativize(file).toString())));
                                    int bytesRead;
                                    while ((bytesRead = fileInputStream.read(bytes)) != -1) {
                                        while (!isWritable.get()) {
                                            try {
                                                Thread.sleep(3);
                                            } catch (InterruptedException e) {
                                            }
                                        }
                                        if (bytesRead >= bufferSize) {
                                            zipOutputStream.write(bytes);
                                        } else {
                                            zipOutputStream.write(bytes, 0, bytesRead);
                                        }
                                    }
                                    zipOutputStream.closeEntry();

                                } catch (IOException e) {
                                    logger.error(e);
                                }
                            });

                    zipOutputStream.flush();

                } catch (IOException e) {
                    logger.error(e);

                }

            } else {
                routingContext.response().putHeader("Content-disposition", "attachment; filename=\""
                        + absolutePath.substring(absolutePath.lastIndexOf(FileSystems.getDefault().getSeparator()) + 1) + "\"").sendFile(absolutePath);
            }
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        }
    }

    public static void uploadFileHandler(RoutingContext routingContext) {
        routingContext.request().pause();
        String key = routingContext.request().getParam(UrlParamName.REFERENCE);

        if (uploadReference.containsKey(key)) {
            String absolutePath = uploadReference.get(key);
            logger.debug("The absolute path of uploading: " + absolutePath);
            uploadReference.remove(key);

            String temp = absolutePath.substring(Config.valueOf(ConfigName.USER_ROOT).length() + 1);
            String userName = temp.substring(0, temp.indexOf(FileSystems.getDefault().getSeparator()));

            try {
                if (runCommand("sudo", "-u", userName, "touch", absolutePath)) {
                    routingContext.vertx().fileSystem().open(absolutePath, new OpenOptions(), asyncResult -> {
                        if (asyncResult.succeeded()) {
                            final AsyncFile file = asyncResult.result();
                            final Pump pump = Pump.pump(routingContext.request(), file);
                            routingContext.request().endHandler(Void -> file.close(result -> {
                                if (result.succeeded()) {
                                    routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end();
                                } else {
                                    logger.error(result.cause());
                                }
                            }));
                            pump.start();
                            routingContext.request().resume();
                        } else {
                            logger.error(asyncResult.cause());
                        }
                    });
                } else {
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                }
            } catch (IOException | InterruptedException e) {
                logger.error(e);
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        }
    }

    public static void deleteHandler(RoutingContext routingContext) {
        String nelsId = routingContext.request().getParam(UrlParamName.NELS_ID);
        JsonArray requestJsonArray = routingContext.getBodyAsJsonArray();

        if (requestJsonArray == null || requestJsonArray.size() == 0) {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        } else {
            String nelsName = Utils.nelsIdToNelsName(nelsId);
            String rootPath = StringUtils.join(Config.valueOf(ConfigName.USER_ROOT), FileSystems.getDefault().getSeparator(), nelsName);
            logger.debug(StringUtils.join("Nels user ", nelsName, " wants to delete content of ", rootPath));
            Path path;
            try {
                for (Object item : requestJsonArray) {
                    path = Paths.get(rootPath, item.toString());
                    if (Files.isDirectory(path)) {
                        logger.debug("Deleting folder " + path.toString());
                        Files.walkFileTree(path, new SimpleFileVisitor());
                    } else {
                        logger.debug("Deleting file " + path.toString());
                        Files.deleteIfExists(path);
                    }
                }
                routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
            } catch (IOException e) {
                logger.error(e);
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        }
    }

    public static void renameHandler(RoutingContext routingContext) {
        String nelsId = routingContext.request().getParam(UrlParamName.NELS_ID);

        JsonObject requestJsonBody = routingContext.getBodyAsJson();
        if (requestJsonBody == null) {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        } else {
            String relativePath = requestJsonBody.getString(JsonConstant.PARENT_PATH);

            String nelsName = Utils.nelsIdToNelsName(nelsId);

            Path oldPath = Paths.get(StringUtils.join(new String[]{Config.valueOf(ConfigName.USER_ROOT), nelsName, relativePath, requestJsonBody.getString(JsonConstant.OLD)}, FileSystems.getDefault().getSeparator()));
            String newName = requestJsonBody.getString(JsonConstant.NEW);
            logger.debug(StringUtils.join("Nels user ", nelsName, " wants to rename ", oldPath, " to ", newName));

            try {
                Files.move(oldPath, oldPath.resolveSibling(newName));
                routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
            } catch (IOException e) {
                logger.error(e);
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(e.toString());
            }
        }
    }

    public static void createFolderHandler(RoutingContext routingContext) {
        String nelsId = routingContext.request().getParam(UrlParamName.NELS_ID);

        JsonObject requestJsonBody = routingContext.getBodyAsJson();
        if (requestJsonBody == null) {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        } else {
            String relativePath = requestJsonBody.getString(JsonConstant.PATH);

            String nelsName = Utils.nelsIdToNelsName(nelsId);

            String absolutePath = StringUtils.join(new String[]{Config.valueOf(ConfigName.USER_ROOT), nelsName, relativePath}, FileSystems.getDefault().getSeparator());

            if (!Files.exists(Paths.get(absolutePath))) {
                try {
                    if (runCommand("sudo", "-u", nelsName, "mkdir", absolutePath)) {
                        routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
                    } else {
                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                    }
                } catch (IOException | InterruptedException e) {
                    logger.error(e);
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(e.getMessage());
                }
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
            }
        }
    }

    public static void createUserHandler(RoutingContext routingContext) {
        String nelsId = routingContext.request().getParam(UrlParamName.NELS_ID);

        try {
            if (runCommand(Config.valueOf(ConfigName.PYTHON_PATH), Config.valueOf(ConfigName.USER_ADD_SCRIPT), nelsId)) {
                routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        } catch (IOException | InterruptedException e) {
            logger.error(e);
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
        }
    }

    public static void deleteUserHandler(RoutingContext routingContext) {
        String nelsId = routingContext.request().getParam(UrlParamName.NELS_ID);

        try {
            if (runCommand(Config.valueOf(ConfigName.PYTHON_PATH), Config.valueOf(ConfigName.USER_DELETE_SCRIPT), nelsId)) {
                routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        } catch (IOException | InterruptedException e) {
            logger.error(e);
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
        }
    }

    public static void renameProjectHandler(RoutingContext routingContext) {
        String projectId = routingContext.request().getParam(UrlParamName.PROJECT_ID);

        JsonObject requestJsonBody = routingContext.getBodyAsJson();
        if (requestJsonBody == null) {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        } else {
            String projectName = requestJsonBody.getString(JsonConstant.PROJECT_NAME);
            try {
                if (runCommand(Config.valueOf(ConfigName.PYTHON_PATH), Config.valueOf(ConfigName.PROJECT_RENAME_SCRIPT), projectId, projectName)) {
                    routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
                } else {
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                }
            } catch (IOException | InterruptedException e) {
                logger.error(e);
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        }
    }

    public static void createProjectHandler(RoutingContext routingContext) {
        String projectId = routingContext.request().getParam(UrlParamName.PROJECT_ID);

        JsonObject requestJsonBody = routingContext.getBodyAsJson();
        if (requestJsonBody == null) {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        } else {
            String projectName = requestJsonBody.getString(JsonConstant.PROJECT_NAME);
            try {
                if (runCommand(Config.valueOf(ConfigName.PYTHON_PATH), Config.valueOf(ConfigName.PROJECT_ADD_SCRIPT), projectId, projectName)) {
                    routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
                } else {
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                }
            } catch (IOException | InterruptedException e) {
                logger.error(e);
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        }
    }

    public static void deleteProjectHandler(RoutingContext routingContext) {
        String projectId = routingContext.request().getParam(UrlParamName.PROJECT_ID);
        logger.debug("Deleting project " + projectId);
        try {
            if (runCommand(Config.valueOf(ConfigName.PYTHON_PATH), Config.valueOf(ConfigName.PROJECT_DELETE_SCRIPT), projectId)) {
                logger.debug("Project " + projectId + " has been deleted");
                routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        } catch (IOException | InterruptedException e) {
            logger.error(e);
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
        }
    }

    public static void addUserToProjectHandler(RoutingContext routingContext) {
        String projectId = routingContext.request().getParam(UrlParamName.PROJECT_ID);
        String nelsId = routingContext.request().getParam(UrlParamName.NELS_ID);

        JsonObject requestJsonBody = routingContext.getBodyAsJson();
        if (requestJsonBody == null) {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        } else {
            String projectRole = requestJsonBody.getString(JsonConstant.PROJECT_ROLE);
            try {
                if (runCommand(Config.valueOf(ConfigName.PYTHON_PATH), Config.valueOf(ConfigName.ADD_USER_TO_PROJECT), projectId, nelsId, projectRole)) {
                    routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
                } else {
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                }
            } catch (IOException | InterruptedException e) {
                logger.error(e);
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        }
    }

    public static void deleteUserInProjectHandler(RoutingContext routingContext) {
        String projectId = routingContext.request().getParam(UrlParamName.PROJECT_ID);
        String nelsId = routingContext.request().getParam(UrlParamName.NELS_ID);

        try {
            if (runCommand(Config.valueOf(ConfigName.PYTHON_PATH), Config.valueOf(ConfigName.DELETE_USER_IN_PROJECT), projectId, nelsId)) {
                routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        } catch (IOException | InterruptedException e) {
            logger.error(e);
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
        }
    }

    private static boolean runCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(command);


        Process process = processBuilder.start();
        String errorOutput = Utils.output(process.getErrorStream());
        if (!StringUtils.isEmpty(errorOutput)) {
            logger.error(errorOutput);
        }
        String debugOutput = Utils.output(process.getInputStream());
        logger.debug(debugOutput);
        if (process.waitFor() != 0) {
            return false;
        } else {
            return true;
        }

    }
}
