package no.nels.client.model;

/**
 * Created by weizhang on 3/4/16.
 */
public abstract class Event {
    private String subject;
    private String verb;
    private String object;

    public Event(String subject, String verb, String object) {
        this.subject = subject;
        this.verb = verb;
        this.object = object;
    }

    public String getSubject() {
        return subject;
    }


    public String getVerb() {
        return verb;
    }

    public String getObject() {
        return object;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public void setObject(String object) {
        this.object = object;
    }
}
