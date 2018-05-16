package no.nels.client.model;
/**
 * Created by weizhang on 5/4/16.
 */
public class NewJobRequest {
    private long nelsId;
    private long jobTypeId;
    private Object parameters;

    public NewJobRequest() {
    }

    public NewJobRequest(long nelsId, long jobTypeId,  Object parameters) {
        this.nelsId = nelsId;
        this.jobTypeId = jobTypeId;
        this.parameters = parameters;
    }

    public long getNelsId() {
        return nelsId;
    }

    public void setNelsId(long nelsId) {
        this.nelsId = nelsId;
    }

    public long getJobTypeId() {
        return jobTypeId;
    }

    public void setJobTypeId(long jobTypeId) {
        this.jobTypeId = jobTypeId;
    }

    public Object getParameters() {
        return parameters;
    }

    public void setParameters(Object parameters) {
        this.parameters = parameters;
    }
}
