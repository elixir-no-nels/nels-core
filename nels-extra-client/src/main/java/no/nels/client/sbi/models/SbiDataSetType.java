package no.nels.client.sbi.models;

import no.nels.commons.abstracts.ANumberId;

/**
 * Created by weizhang on 1/17/17.
 */
public final class SbiDataSetType extends ANumberId{

    private long id;
    private String name;
    private String description;

    public SbiDataSetType(long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
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
}
