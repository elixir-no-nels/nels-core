package no.nels.portal.pages;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import no.nels.client.Jobs;
import no.nels.client.model.Job;
import no.nels.client.model.JobFeed;
import no.nels.portal.facades.LoggingFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.facades.SessionFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.SessionItemKeys;
import no.nels.portal.utilities.JSFUtils;
import no.nels.vertx.commons.constants.MqJobStatus;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ManagedBean(name = ManagedBeanNames.pages_jobs)
@ViewScoped
public class JobsBean extends InteractionBean implements Serializable {

    @Override
    public String getPageTitle() {
        return "Jobs";
    }

    @Override
    public void secure() {
        SecurityFacade.requireLogin();
    }

    public void populateJobs() {
        try {

        } catch (Exception ex) {
        }
    }

    public List<Job> getJobs() throws Exception {
        long nelsId = SecurityFacade.getUserBeingViewed().getId();
        return Jobs.getJobs(nelsId, Optional.empty());
    }

    public List<JobFeed> getJobFeeds(long jobId) throws IOException {
        return Jobs.getJobFeeds(jobId);
    }

    public String getJobTypeText(Job jb) {
        String ret = "";
        try {
            JsonNode params = new ObjectMapper().readTree(jb.getParams());
            if (jb.getJobTypeId() == 100) {
                ret = "Copy (";
                ret += params.get("source").toString().contains("Personal") ? "Personal" : "Projects";
                ret += " -> ";
                ret += params.get("destination").toString().contains("Personal") ? "Personal" : "Projects";
                ret += " )";
            } else if (jb.getJobTypeId() == 101) {
                ret = "Move (";
                ret += params.get("source").toString().contains("Personal") ? "Personal" : "Projects";
                ret += " -> ";
                ret += params.get("destination").toString().contains("Personal") ? "Personal" : "Projects";
                ret += ")";
            }
        } catch (Exception ex) {
            LoggingFacade.logDebugInfo(ex);
        }
        return ret;
    }

    public String getJobStatusCss(Job jb) {
        String ret = "bootstrap-info";
        if (jb.getStateId() == MqJobStatus.SUBMITTED.getValue()) {
            ret = "bootstrap-gray";
        } else if (jb.getStateId() == MqJobStatus.SUCCESS.getValue()) {
            ret = "bootstrap-success";
        } else if (jb.getStateId() == MqJobStatus.FAILURE.getValue()) {
            ret = "bootstrap-danger";
        }
        return ret;
    }

    public boolean isJobDone(Job jb) {
        return jb.getStateId() == MqJobStatus.SUCCESS.getValue() || jb.getStateId() == MqJobStatus.FAILURE.getValue();
    }

    public String getJobStatusText(Job jb) {
        String ret = "";
        if (jb.getStateId() == MqJobStatus.SUBMITTED.getValue()) {
            ret = "Submitted";
        } else if (jb.getStateId() == MqJobStatus.SUCCESS.getValue()) {
            ret = "Completed Successfully";
        } else if (jb.getStateId() == MqJobStatus.FAILURE.getValue()) {
            ret = "Failure";
        }
        return ret;
    }

    public void sendUserJobsJson() throws Exception {
        secure();

        Optional<Long> lastSeenJobUpdateTime = SessionFacade.isSessionObjectSet(SessionItemKeys.LAST_JOB_FETCH_TIME) ? Optional.of((Long) SessionFacade.getSessionObject(SessionItemKeys.LAST_JOB_FETCH_TIME)) : Optional.of(new Long(0));
        SessionFacade.setSessionObject(SessionItemKeys.LAST_JOB_FETCH_TIME, new Date().getTime() / 1000 - 1);

        List<Job> jobs = Jobs.getJobs(SecurityFacade.getUserBeingViewed().getId(), lastSeenJobUpdateTime);

        JsonObject jsonObject = new JsonObject();
        if (jobs.size() == 0) {
            com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();
            jsonObject.add("jobs", jsonArray);
            JSFUtils.sendJsonToUser(jsonObject.toString());
        } else {
            JsonElement element = new Gson().toJsonTree(jobs, new TypeToken<List<Job>>() {
            }.getType());
            jsonObject.add("jobs", element.getAsJsonArray());
            JSFUtils.sendJsonToUser(jsonObject.toString());
        }


    }

    @Override
    public String getTargetPage() {
        return null;
    }

    @Override
    public Map<String, String> getUrlParameters() {
        return null;
    }

    @Override
    public void transferToOther() {

    }

    @Override
    public boolean transferButtonVisible() {
        return false;
    }

    @Override
    public String getDestinationForView() {
        return null;
    }
}
