package no.nels.portal.facades;

import com.google.gson.JsonArray;
import no.nels.client.sbi.MasterApiConsumer;
import no.nels.client.sbi.SbiApiConsumer;
import no.nels.client.sbi.SbiException;
import no.nels.client.sbi.models.SbiData;
import no.nels.client.sbi.models.SbiDataSet;
import no.nels.client.sbi.models.SbiProject;
import no.nels.client.sbi.models.SbiSubtype;
import no.nels.commons.model.NumberIndexedList;
import no.nels.vertx.commons.constants.MqJobType;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Comparator.comparing;

public final class SbiFacade {
    private static final Logger logger = LogManager.getLogger(SbiFacade.class);

    public static NumberIndexedList getSbiProjects(String federatedId) {
        List<SbiProject> projectsList = null;
        try {
            projectsList = SbiApiConsumer.getProjects(federatedId);
        } catch (ParseException e) {
            MessageFacade.addFatal(e.getLocalizedMessage());
        } catch (SbiException e) {
            MessageFacade.AddError("Internal error");
        }

        final NumberIndexedList ret = new NumberIndexedList();
        if (projectsList != null && projectsList.size() != 0) {
            projectsList.stream().forEach(ret::add);
        }
        return ret;
    }

    public static SbiProject getSbiProject(long projectId) {
        SbiProject sbiProject = null;
        try {
            logger.debug("trying to getProject. projectId:" + projectId);
            sbiProject = SbiApiConsumer.getProject(projectId);
            logger.debug("sbiProject name" + sbiProject.getName());
        } catch (SbiException e) {
            MessageFacade.AddError("Internal error");
        }
        return sbiProject;
    }

    public static JsonArray getSbiProjectMembers(long projectId) {
        try {
            return SbiApiConsumer.getProjectMembersJson(projectId);
        } catch (SbiException e) {
            MessageFacade.AddError("Internal error");
        }
        return null;
    }

    public static SbiDataSet getSbiDataSet(String federatedId, long projectId, long datasetId) {
        SbiDataSet sbiDataSet = null;
        try {
            sbiDataSet = SbiApiConsumer.getDataSet(federatedId, projectId, datasetId);
        } catch (SbiException e) {
            MessageFacade.AddError("Internal error");
        } catch (ParseException e) {
            MessageFacade.addFatal(e.getLocalizedMessage());
        }
        return sbiDataSet;
    }

    public static List<SbiSubtype> getSbiSubtypes(String federatedId, long projectId, long datasetId) {
        List<SbiSubtype> sbiSubtypes = null;
        try {
            sbiSubtypes = SbiApiConsumer.getSubtypes(federatedId, projectId, datasetId);
        } catch (SbiException e) {
            MessageFacade.AddError("Internal error");
        } catch (ParseException e) {
            MessageFacade.addFatal(e.getLocalizedMessage());
        }
        return sbiSubtypes;
    }

    public static NumberIndexedList getSbiDataSetListForProject(String federatedId, String projectId) {
        LoggingFacade.logDebugInfo("federatedId:" + federatedId + ",projectId:" + projectId);
        List<SbiDataSet> dataSets = null;
        try {
            dataSets = SbiApiConsumer.getDataSets(federatedId, Long.valueOf(projectId));
        } catch (ParseException e) {
            MessageFacade.addFatal(e.getLocalizedMessage());
        } catch (SbiException e) {
            MessageFacade.AddError("Internal error");
        }

        final NumberIndexedList ret = new NumberIndexedList();
        if (dataSets.size() != 0) {
            dataSets.stream().forEach(ret::add);
        }
        return ret;
    }

    public static NumberIndexedList getSubTypeListForDataSet(String federatedId, String projectId, String dataSetId) {

        List<SbiSubtype> subtypes = null;
        try {
            subtypes = SbiApiConsumer.getSubtypes(federatedId, Long.valueOf(projectId), Long.valueOf(dataSetId));
        } catch (ParseException e) {
            MessageFacade.addFatal(e.getLocalizedMessage());
        } catch (SbiException e) {
            MessageFacade.AddError("Internal error");
        }
        final NumberIndexedList ret = new NumberIndexedList();
        if (subtypes != null && subtypes.size() != 0) {
            subtypes.stream().forEach(ret::add);
        }

        return ret;
    }

    public static List<String> assembleAllFilesPath(String federatedId, String projectId, String dataSetId, String subtypeId, String parentPath) {
        List<String> filesPath = new ArrayList<>();
        try {
            List<SbiData> list = SbiApiConsumer.getContent(federatedId, Long.valueOf(projectId), Long.valueOf(dataSetId), Long.valueOf(subtypeId), parentPath);
            list.stream().forEach(file -> {
                if (!file.isFolder()) {
                    filesPath.add(StringUtils.join(new String[]{parentPath, file.getName()}, File.separator));
                } else {
                    filesPath.addAll(assembleAllFilesPath(federatedId, projectId, dataSetId, subtypeId, parentPath + File.separator + file.getName()));
                }
            });
            return filesPath;
        } catch (SbiException e) {
            MessageFacade.AddError("Internal error");
            return Collections.emptyList();
        }
    }


    public static NumberIndexedList getSubTypeContent(String federatedId, String projectId, String dataSetId, String subTypeId) {

        final NumberIndexedList ret = new NumberIndexedList();
        try {
            List<SbiData> list = SbiApiConsumer.getSubtype(federatedId, Long.valueOf(projectId), Long.valueOf(dataSetId), Long.valueOf(subTypeId));
            list.sort(comparing(SbiData::getName, String.CASE_INSENSITIVE_ORDER));
            if (list != null && list.size() != 0) {
                list.stream().forEach(ret::add);
            }

        } catch (SbiException e) {
            MessageFacade.AddError("Internal error");
        }


        return ret;
    }

    public static NumberIndexedList fetchFiles(String federatedId, String projectId, String dataSetId, String subTypeId, String parentPath) {

        final NumberIndexedList ret = new NumberIndexedList();
        try {
            List<SbiData> list = SbiApiConsumer.getContent(federatedId, Long.valueOf(projectId), Long.valueOf(dataSetId), Long.valueOf(subTypeId), parentPath);
            list.sort(comparing(SbiData::getName, String.CASE_INSENSITIVE_ORDER));
            if (list != null && list.size() != 0) {
                list.stream().forEach(ret::add);
            }

        } catch (SbiException e) {
            MessageFacade.AddError("Internal error");
        }
        return ret;
    }

    public static void fetchDataFromSbi(long nelsId, String remoteHost, String userName, String identityKey,
                                        String refDataSetId, String dataSet, String subType, long subtypeId, List<String> files, List<String> folders, String destinationPath, String parentPathOfSource) {

        try {
            MasterApiConsumer.addPushDataToNelsJob(nelsId, MqJobType.NIRD_SBI_PUSH.getValue(), remoteHost, userName, identityKey, refDataSetId, dataSet, subType, subtypeId, parentPathOfSource, destinationPath, files, folders);
        } catch (SbiException e) {
            MessageFacade.AddError("Internal error");
        }

    }


    /**
     * @param nelsId
     * @param remoteHost
     * @param userName
     * @param identityKey
     * @param refDataSetId: DataSet reference id
     * @param subtype:      subtype name
     * @param sourceFolder: The path above the selected file. For instance, there is a file "hello.txt" under /Personal/a. Then the parentPathOfSource is /Personal/a.
     * @param relativePath: path trimmed "StoreBioinfo/Project/DataSet/Subtype".
     * @param fileNames:    file name.
     * @param folderNames:  folder name.
     */
    public static void transferDataToSbi(long nelsId, String remoteHost, String userName, String identityKey, String refDataSetId, String dataSet,
                                         String subtype, long subtypeId, String sourceFolder, String relativePath, List<String> fileNames, List<String> folderNames) {


        try {
            MasterApiConsumer.addPullDataFromNelsJob(nelsId, MqJobType.NIRD_SBI_PULL.getValue(), remoteHost, userName, identityKey, refDataSetId, dataSet, subtype, subtypeId, sourceFolder, relativePath, fileNames, folderNames);
        } catch (SbiException e) {
            MessageFacade.AddError("Internal error");
        }
        //refDataSetId, remoteHost, userName, identityKey, subType, transferredDataInfoList, (relativePath != null) ? relativePath : "");

    }

}
