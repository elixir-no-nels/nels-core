package no.nels.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import no.nels.client.model.Job;
import no.nels.client.model.JobFeed;
import no.nels.vertx.commons.constants.MqJobType;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Created by weizhang on 5/2/16.
 */
public class Jobs extends ApiClient {

    public static void addCopyJob(long nelsId, String[] srcPath, String dstPath) {
        addJob(MqJobType.STORAGE_COPY.getValue(), nelsId, srcPath, dstPath);
    }

    public static void addMoveJob(long nelsId, String[] srcPath, String dstPath) {
        addJob(MqJobType.STORAGE_MOVE.getValue(), nelsId, srcPath, dstPath);
    }

    public static void addJob(int jobType, long nelsId, String[] srcPath, String dstPath) {

        String url = Config.getMasterApiUrl() + "/jobs/add";

        JsonArray src = new JsonArray();
        Arrays.asList(srcPath).stream().forEach(src::add);
        JsonObject parameters = new JsonObject();

        parameters.add("source", src);
        parameters.addProperty("destination", dstPath);
        JsonObject json = new JsonObject();
        json.addProperty("nels_id", nelsId);
        json.addProperty("job_type_id", jobType);

        json.add("parameters", parameters);
        String payload =  new Gson().toJson(json);


        Client client = getMasterClient();
        Response response = client.target(url).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(payload));

        int status = response.getStatus();
        if (status != 201) {
            throw new RuntimeException("Not able to add job for url: " + url + ",response code:" + status);
        }

    }

    public static String getJobsJson(long nelsId, Optional<Long> since) throws IOException {

        Client client = getMasterClient();
        String url;
        if(since.isPresent()){
            url = Config.getMasterApiUrl() + "/jobs/user/" + String.valueOf(nelsId) + "?since=" + since.get();
        }else {
            url = Config.getMasterApiUrl() + "/jobs/user/" + String.valueOf(nelsId);
        }
        Response response = client.target(url).request(MediaType.APPLICATION_JSON_TYPE).get();
        int status = response.getStatus();
        if (status != 200) {
            throw new RuntimeException("Not able to get jobs for url: " + Config.getMasterApiUrl() + ",response code:" + status);
        }

        return response.readEntity(String.class);
    }

    public static List<Job> getJobs(long nelsId, Optional<Long> since) throws IOException {
        String json = getJobsJson(nelsId, since);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, Job.class));

    }

    public static String getJobJson(long jobId) throws IOException {

        Client client = getMasterClient();
        Response response = client.target(Config.getMasterApiUrl() + "/jobs/" + String.valueOf(jobId)).request(MediaType.APPLICATION_JSON_TYPE).get();
        int status = response.getStatus();
        if (status != 200) {
            throw new RuntimeException("Not able to get jobs for url: " + Config.getMasterApiUrl() + ",response code:" + status);
        }
        return response.readEntity(String.class);
    }

    public static Job getJob(long jobId) throws IOException {
        String json = getJobJson(jobId);
        ObjectMapper mapper = new ObjectMapper();
        return  mapper.readValue(json, Job.class);
    }

    public static void deleteJob(long nelsId, long jobId) throws IOException {

        Client client = getMasterClient();
        Response response = client.target(Config.getMasterApiUrl() + "/jobs/"+ String.valueOf(jobId)).request().delete();
        int status = response.getStatus();
        if (status != 204) {
            throw new RuntimeException("Not able to delete job for url: " + Config.getMasterApiUrl() + ",response code:" + status);
        }

    }

    public static int getCompletion(long jobId) throws IOException {

        Client client = getMasterClient();
        Response response = client.target(Config.getMasterApiUrl() + "/completion/" + String.valueOf(jobId)).request().get();
        int status = response.getStatus();
        if (status != 200) {
            throw new RuntimeException("Not able to get jobs for url: " + Config.getMasterApiUrl() + ",response code:" + status);
        }
        String completion = response.readEntity(String.class);

        return Integer.valueOf(completion);
    }

    public static String getJobFeedsJson(long jobId) throws IOException {

        Client client = getMasterClient();
        Response response = client.target(Config.getMasterApiUrl() + "/jobs/" + String.valueOf(jobId) + "/feeds").request(MediaType.APPLICATION_JSON_TYPE).get();
        int status = response.getStatus();
        if (status != 200) {
            throw new RuntimeException("Not able to get jobfeed for url: " + Config.getMasterApiUrl() + ",response code:" + status);
        }
        return response.readEntity(String.class);
    }

    public static List<JobFeed> getJobFeeds(long jobId) throws IOException {
        String json = getJobFeedsJson(jobId);
        ObjectMapper mapper = new ObjectMapper();
        List<JobFeed> feeds = mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, JobFeed.class));
        return feeds;
    }

}
