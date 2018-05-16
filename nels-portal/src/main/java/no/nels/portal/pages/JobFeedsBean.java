package no.nels.portal.pages;

import no.nels.client.Jobs;
import no.nels.portal.abstracts.ANelsBean;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.facades.URLParametersFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.utilities.JSFUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean(name = ManagedBeanNames.pages_job_feeds)
@ViewScoped
public class JobFeedsBean extends ANelsBean {

    @Override
	public String getPageTitle() {
		SecurityFacade.requireLogin();
		long jobId = URLParametersFacade.getIDParameter();
		try {
			sendUserJobFeedsJson(jobId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "jobFeed";
	}

	private void sendUserJobFeedsJson(long jobId) throws Exception{
		String json = Jobs.getJobFeedsJson(jobId);
		JSFUtils.sendJsonToUser(json);
	}
}
