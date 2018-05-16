package no.nels.eup.core.model.db;

import no.nels.commons.abstracts.*;

import java.util.Date;

public class DBProject extends ANumberId{
    private long id;
    private String name;
    private String description;
    private Date creationDate;

    @Override
    public long getId() {
        return this.id;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
