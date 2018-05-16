package no.nels.client.model.responses;

/**
 * Created by Kidane on 04.12.2015.
 */
public class ActionResponse {
    public ActionResponse(){}

    private long exit;
    private String out;
    private String err;

    public long getExit() {
        return exit;
    }

    public void setExit(long exit) {
        this.exit = exit;
    }

    public String getOut() {
        return out;
    }

    public void setOut(String out) {
        this.out = out;
    }

    public String getErr() {
        return err;
    }

    public void setErr(String err) {
        this.err = err;
    }
}
