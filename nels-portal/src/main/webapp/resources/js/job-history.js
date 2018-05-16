
function getJobFeeds(jobId, rootUrl) {
    $.ajax({
        url: rootUrl + "/pages/job-feeds.xhtml?id=" + jobId,
        type: "get",
        dataType: 'json',
        success: function (jobFeeds) {
            $("#job-feeds-" + jobId).empty();
            jobFeeds.forEach(function (feed) {
                var jobFeedDiv = $("<div>").html("<span style='font-size:11px;'>" + new Date(feed.feedtime) + "</span><br/> - " + feed.feedtext);
                jobFeedDiv.attr({
                    'class': 'job-feed'
                });
                $("#job-feeds-" + jobId).append(jobFeedDiv);
            });

        },
        error: function (xhr, status, error) {
            //alert("error" + xhr.responseText);
        }
    });
}

function getJobUpdate(rootUrl) {
    $.ajax({
        url: rootUrl + "/pages/jobs.xhtml",
        type: "get",
        dataType: 'json',
        success: function (data) {
            var jobsContainer = $('#newJobs');
            var needForRefresh = false;
            var jobs = data.jobs;
            //jobsContainer.empty();
            if (jobs.length != 0) {
                jobs.forEach(function (job) {
                    var identifier = 'jb-' + job.jobId;
                    if ($('#' + identifier).length) {
                        //update state css
                        $('#' + identifier).attr({
                            'class': 'job-wrapper ' + getJobStatusCss(job)
                        });
                        $('#' + identifier).find('.job-extra-info').html("Status: " + getJobStatus(job) + "<br/>Submitted time: " + job.createTime + "<br/>Completed: " + job.completion + " %");
                        $("#progress-" + job.jobId).html('');
                        var bar1 = $("#progress-" + job.jobId).progressbar();
                        bar1.progress(job.completion);
                        if (isJobTerminated(job)) {
                            //hide progress
                            $("#progress-" + job.jobId).remove();
                            //show job-close button
                            $("#job-close-" + job.jobId).removeAttr('style');
                            needForRefresh = true;
                        }
                    }
                    else {
                        needForRefresh = true;
                        //add new job display
                        var jobDisplay = $("<div>").attr({
                            'id': identifier,
                            'class': 'job-wrapper ' + getJobStatusCss(job)
                        });
                        var jobHeader = $('<div>').attr({
                            'class': 'job-header',
                            'id': 'job-header-' + job.jobId
                        });
                        var jobClose = $('<a>').html('&times;').attr({
                            id: 'job-close-' + job.jobId,
                            class: 'job-close'
                        });
                        //hide job close button if job not yet done
                        if(!isJobTerminated(job)) {
                            jobClose.attr({style: 'display:none;'});
                        }

                        jobHeader.append(jobClose);
                        jobHeader.append($('<div>').html(job.jobId + " : " + getJobType(job)).attr({
                                'id': 'job-header-' + job.jobId + "-title"
                            })
                        );
                        if (job.completion < 100 && !isJobTerminated(job)) {
                            var progressBar = $("<div>").attr({
                                'id': "progress-" + job.jobId
                            });

                            var bar1 = progressBar.progressbar();
                            bar1.progress(job.completion);
                            jobHeader.append(progressBar);
                        }
                        var jobExtraInfo = $('<div>').html("Status: " + getJobStatus(job) + "<br/>Submitted time: " + job.createTime + "<br/>Completed: " + job.completion + "&nbsp;%");
                        jobExtraInfo.attr({
                            'class': 'job-extra-info'
                        });

                        var jobFeeds = $('<div>').html("");
                        jobFeeds.attr({
                            'id': 'job-feeds-' + job.jobId,
                            'class': 'job-feeds'
                        });
                        jobDisplay.append(jobHeader);
                        jobDisplay.append(jobExtraInfo);
                        jobDisplay.append(jobFeeds);
                        jobsContainer.append(jobDisplay);

                        //attach toggle effect on feeds
                        $("#job-header-" + job.jobId + '-title').click(function () {
                            if (!$("#job-feeds-" + job.jobId).is(":visible")) {
                                getJobFeeds(job.jobId, rootUrl);
                            }
                            $("#job-feeds-" + job.jobId).slideToggle("slow");

                        });
                        //start job-feeds collapsed
                        $("#job-feeds-" + job.jobId).hide();

                        //attach the delete job button
                        $("#job-close-" + job.jobId).click(function () {
                            deleteJob(job.jobId, rootUrl);
                        });
                    }
                });
            }
            console.log("needForRefresh: " + needForRefresh);
            if(needForRefresh){

                $("#refresh_foleFoldersButton").click();
                console.log("reloaded attempted ");
            }
        },
        error: function (xhr, status, error) {
            console.log("error:" + xhr.responseText + ",status:" + status);
        }
    });
}

function isJobTerminated(job){
    return job.stateId == 101 || job.stateId == 102;
}

function deleteJob(id, rootUrl) {
    $.ajax({
        url: rootUrl + "/pages/job.xhtml?job_op=delete&id=" + id,
        type: "get",
        success: function (data) {
            $("#jb-" + id).slideUp("slow", function(){ $(this).remove(); })
        },
        error: function (xhr, status, error) {
            console.log("delete job " + id + " failed");
        }
    });
}

function getJobType(jb) {
    switch (jb.jobTypeId) {
        case 100:
            return "Copy";
            break;
        case 101:
            return "Move";
            break;
        case 102:
        case 106:
            return "NeLS << StoreBioinfo"
            break;
        case 103:
        case 107:
            return "NeLS >> StoreBioinfo"
            break;
        case 104:
            return "NELS << TSD"
            break;
        case 105:
            return "NeLS >> TSD"
            break;
    }
    return "";
}

function getJobStatusCss(jb) {
    switch (jb.stateId) {
        case 100:
            return "bootstrap-gray";
            break;
        case 101:
            return "bootstrap-success";
            break;
        case 102:
            return "bootstrap-danger";
            break;
        case 103:
            return "bootstrap-warning";
            break;
    }
    return "";
}

function getJobStatus(job) {
    switch (job.stateId) {
        case 100:
            return "Submitted";
            break;
        case 101:
            return "Completed successfully";
            break;
        case 102:
            return "Failed";
            break;
        case 103:
            return "Processing";
            break;
        default:
            return "";
    }
}

function isJobTerminated(job){
    return job.stateId == 101 || job.stateId == 102;
}

