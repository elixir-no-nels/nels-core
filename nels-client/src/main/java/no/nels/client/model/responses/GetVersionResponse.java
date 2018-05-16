package no.nels.client.model.responses;

/**
 * Created by Kidane on 30.11.2015.
 */
public class GetVersionResponse{

    public GetVersionResponse(){}

    private String version;
    private String author;
    private String production;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getProduction() {
        return production;
    }

    public void setProduction(String production) {
        this.production = production;
    }
}
