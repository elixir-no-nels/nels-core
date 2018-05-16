package no.nels.client.model;

/**
 * Created by weizhang on 3/29/16.
 */
public class Item {
    private long id;
    private String name;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Item(long id, String name) {
        this.id = id;
        this.name = name;
    }
}
