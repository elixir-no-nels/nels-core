package no.nels.client.sbi.models;

import no.nels.commons.abstracts.ANumberId;


import java.util.Date;

public final class SbiDataSet extends ANumberId {
    private long id;
    private String dataSetId;
    private String name;
    private String type;
    private boolean locked;
    private String owner;
    private Date creationDate;
    private String description;


    public SbiDataSet(long id, String dataSetId, String name, String type, boolean locked, String owner, String description, Date creationDate) {
        this.id = id;
        this.dataSetId = dataSetId;
        this.name = name;
        this.type = type;
        this.locked = locked;
        this.owner = owner;
        this.creationDate = creationDate;
        this.description = description;
    }

    @Override
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isLocked() {
        return locked;
    }

    public String getType() {
        return type;
    }

    public String getDataSetId() {
        return dataSetId;
    }

    public String getOwner() {
        return owner;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public String getDescription() {return description;}

}
