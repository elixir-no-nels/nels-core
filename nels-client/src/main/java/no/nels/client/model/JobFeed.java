package no.nels.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

/**
 * Created by weizhang on 5/2/16.
 */

public class JobFeed {

    @JsonProperty("id")
    private long feedId;
    @JsonProperty("jobid")
    private long jobId;
    @JsonProperty("feedtext")
    private String feedText;
    @JsonProperty("feedtime")
    private Date feedTime;
    private int status;


    public JobFeed() {
    }

    public JobFeed(long feedId, long jobId, String feedText, Date feedTime, int status) {
        this.feedId = feedId;
        this.jobId = jobId;
        this.feedText = feedText;
        this.feedTime = feedTime;
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getFeedId() {
        return feedId;
    }

    public void setFeedId(long feedId) {
        this.feedId = feedId;
    }

    public long getJobId() {
        return jobId;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

    public String getFeedText() {
        return feedText;
    }

    public void setFeedText(String feedText) {
        this.feedText = feedText;
    }

    public Date getFeedTime() {
        return feedTime;
    }

    public void setFeedTime(Date feedTime) {
        this.feedTime = feedTime;
    }
}
