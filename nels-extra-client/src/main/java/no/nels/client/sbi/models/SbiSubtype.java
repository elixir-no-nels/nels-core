package no.nels.client.sbi.models;

import no.nels.commons.abstracts.ANumberId;

import java.util.Date;

public final class SbiSubtype extends ANumberId {

    private long id;
    private String name;
    private String type;
    private long size;
    private Date creationDate;

    public SbiSubtype(long id, String name, String type, long size, Date creationDate) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.size = size;
        this.creationDate = creationDate;
    }

    @Override
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public long getSize() {
        return size;
    }

    public Date getCreationDate() {
        return creationDate;
    }
}
