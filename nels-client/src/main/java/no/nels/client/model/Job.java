package no.nels.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nels.client.Jobs;
import no.nels.vertx.commons.constants.MqJobStatus;

import java.io.IOException;
import java.util.Date;

/**
 * Created by weizhang on 5/2/16.
 */

public class Job {

    @JsonProperty("id")
    private long jobId;
    @JsonProperty("nelsid")
    private long nelsId;
    @JsonProperty("jobtypeid")
    private long jobTypeId;
    private String params;
    @JsonProperty("stateid")
    private long stateId;
    @JsonProperty("createtime")
    private Date createTime;
    @JsonProperty("lastupdate")
    private Date lastUpdate;
    private int completion;


    public Job() {
    }

    public Job(long jobId, long nelsId, long jobTypeId, String params, long stateId, Date createTime, Date lastUpdate, int completion) {
        this.jobId = jobId;
        this.nelsId = nelsId;
        this.jobTypeId = jobTypeId;
        this.params = params;
        this.stateId = stateId;
        this.createTime = createTime;
        this.lastUpdate = lastUpdate;
        this.completion = completion;

    }



    public int getCompletion() throws IOException {
        if(completion != 100){
            return Jobs.getCompletion(this.jobId);
        }else {
            return completion;
        }

    }

    public void setCompletion(int completion) {
        this.completion = completion;
    }

    public long getJobId() {
        return jobId;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
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

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public long getStateId() {
        return stateId;
    }

    public void setStateId(long stateId) {
        this.stateId = stateId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public int getUpdatedCompletion() throws IOException {
        return Jobs.getCompletion(this.jobId);

    }

    public boolean isTerminated(){
        return getStateId() == MqJobStatus.SUCCESS.getValue() || getStateId() == MqJobStatus.FAILURE.getValue();
    }
}
