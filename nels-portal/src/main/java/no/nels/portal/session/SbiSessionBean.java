package no.nels.portal.session;

import no.nels.client.sbi.models.SbiProject;
import no.nels.portal.model.enumerations.ManagedBeanNames;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import java.util.List;

@ManagedBean(name = ManagedBeanNames.session_sbiBean)
@SessionScoped
public final class SbiSessionBean {

    private String projectName;
    private String projectId;
    private String dataSetId;
    private String subTypeId;
    private String dataSetName;
    private String refDataSetId;
    private String subtypeType;
    private List<SbiProject> cachedSbiProjectList;
    private List<String> fetchedDataListFromSbi;
    private String parentPathOfDataFromSbi;
    private String relativePathUnderSubType;

    public String getHomeFolder() {
        return homeFolder;
    }

    public void setHomeFolder(String homeFolder) {
        this.homeFolder = homeFolder;
    }

    private String homeFolder;


    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getSubtypeType() {
        return subtypeType;
    }

    public void setSubtypeType(String subtypeType) {
        this.subtypeType = subtypeType;
    }


    public List<String> getFetchedDataListFromSbi() {
        return fetchedDataListFromSbi;
    }

    public void setFetchedDataListFromSbi(List<String> fetchedDataListFromSbi) {
        this.fetchedDataListFromSbi = fetchedDataListFromSbi;
    }

    public String getRelativePathUnderSubType() {
        return relativePathUnderSubType;
    }

    public void setRelativePathUnderSubType(String relativePathUnderSubType) {
        this.relativePathUnderSubType = relativePathUnderSubType;
    }

    public String getParentPathOfDataFromSbi() {
        return parentPathOfDataFromSbi;
    }

    public void setParentPathOfDataFromSbi(String parentPathOfDataFromSbi) {
        this.parentPathOfDataFromSbi = parentPathOfDataFromSbi;
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public void setDataSetName(String dataSetName) {
        this.dataSetName = dataSetName;
    }

    public String getRefDataSetId() {
        return refDataSetId;
    }

    public void setRefDataSetId(String refDataSetId) {
        this.refDataSetId = refDataSetId;
    }


    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(String dataSetId) {
        this.dataSetId = dataSetId;
    }

    public String getSubTypeId() {
        return subTypeId;
    }

    public void setSubTypeId(String subTypeId) {
        this.subTypeId = subTypeId;
    }
}
