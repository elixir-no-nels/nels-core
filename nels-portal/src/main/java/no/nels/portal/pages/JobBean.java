package no.nels.portal.pages;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nels.client.Jobs;
import no.nels.client.model.Job;
import no.nels.client.model.JobFeed;
import no.nels.portal.abstracts.ANelsBean;
import no.nels.portal.facades.LoggingFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.facades.URLParametersFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.URLParameterNames;
import no.nels.vertx.commons.constants.MqJobStatus;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@ManagedBean(name = ManagedBeanNames.pages_job)
@ViewScoped
public class JobBean extends ANelsBean {

    @Override
	public String getPageTitle() {
		return "Jobs";
	}

	public void populateJobs() {
		try {

		}catch(Exception ex){}
	}

	public List<Job> getJobs() throws Exception{
		long nelsId = SecurityFacade.getUserBeingViewed().getId();
		return Jobs.getJobs(nelsId, Optional.empty());
	}

    public List<JobFeed> getJobFeeds(long jobId) throws IOException {
        return Jobs.getJobFeeds(jobId);
    }

	public String getJobTypeText(Job jb){
		String ret ="";
		try {
			JsonNode params = new ObjectMapper().readTree(jb.getParams());
			if (jb.getJobTypeId() == 100) {
				ret = "Copy (";
				ret += params.get("source").toString().contains("Personal")? "Personal":"Projects";
				ret +=" -> ";
				ret += params.get("destination").toString().contains("Personal")? "Personal":"Projects";
				ret +=" )";
			} else if (jb.getJobTypeId() == 101) {
				ret = "Move (";
				ret += params.get("source").toString().contains("Personal")? "Personal":"Projects";
				ret +=" -> ";
				ret += params.get("destination").toString().contains("Personal")? "Personal":"Projects";
				ret +=")";
			}
		}catch(Exception ex){
			LoggingFacade.logDebugInfo(ex);}
		return ret;
	}

	public String getJobStatusCss(Job jb){
		String ret = "bootstrap-info";
		if(jb.getStateId() == MqJobStatus.SUBMITTED.getValue()){
			ret = "bootstrap-gray";
		}
		else if(jb.getStateId() == MqJobStatus.SUCCESS.getValue()){
			ret = "bootstrap-success";
		}
		else if (jb.getStateId() == MqJobStatus.FAILURE.getValue()){
			ret = "bootstrap-danger";
		}
		return ret;
	}

	public boolean isJobDone(Job jb){
		return jb.getStateId() == MqJobStatus.SUCCESS.getValue() || jb.getStateId() == MqJobStatus.FAILURE.getValue();
	}

	public String getJobStatusText(Job jb){
		String ret = "";
		if(jb.getStateId() == MqJobStatus.SUBMITTED.getValue()){
			ret = "Submitted";
		}
		else if(jb.getStateId() == MqJobStatus.SUCCESS.getValue()){
			ret = "Completed Successfully";
		}
		else if (jb.getStateId() == MqJobStatus.FAILURE.getValue()){
			ret = "Failure";
		}
		return ret;
	}

	public void process() throws Exception{
		SecurityFacade.requireLogin();
		long nelsId = SecurityFacade.getLoggedInUser().getId();
		long jobId = URLParametersFacade.getIDParameter();
		String operation = URLParametersFacade.getMustUrLParameter(URLParameterNames.JOB_OPERATION);
		if(operation.equals(URLParameterNames.JOB_OPERATION_DELETE)){
			Jobs.deleteJob(nelsId, jobId);
		}
	}
}
