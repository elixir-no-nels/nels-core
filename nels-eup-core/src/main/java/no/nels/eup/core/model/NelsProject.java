package no.nels.eup.core.model;

import no.nels.commons.abstracts.*;

import java.util.Date;

public class NelsProject extends ANumberId{
    private long id = -1;
    private String name;
    private String description;
    private Date creationDate;

    public NelsProject() {}

    public NelsProject(long id, String name, String description, Date creationDate) {
        this.name = name;
        this.description = description;
        this.creationDate = creationDate;
        this.id = id;
    }

    @Override
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Date getCreationDate() {
        return creationDate;
    }
}
