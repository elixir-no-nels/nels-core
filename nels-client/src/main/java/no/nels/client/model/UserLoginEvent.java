package no.nels.client.model;

/**
 * Created by weizhang on 3/4/16.
 */
public class UserLoginEvent {
    private String name;
    private String ip;

    public UserLoginEvent(String name, String ip) {
        this.name = name;
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
