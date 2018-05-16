package no.nels.commons.model;

import no.nels.commons.abstracts.ANumberId;
import no.nels.commons.abstracts.ASystemUser;

public final class NelsUser extends ANumberId {
    public NelsUser() {

    }

    public NelsUser(IDPUser idpUser, long nelsId, ASystemUser systemUserType,
                    boolean isActive) {
        this.idpUser = idpUser;
        this.id = nelsId;
        this.systemUserType = systemUserType;
        this.isActive = isActive;
    }

    public IDPUser getIdpUser() {
        return idpUser;
    }

    public ASystemUser getSystemUserType() {
        return systemUserType;
    }

    public boolean isActive() {
        return isActive;
    }

    private IDPUser idpUser = null;
    private long id = -1;
    private ASystemUser systemUserType = null;
    private boolean isActive = true;

    @Override
    public long getId() {
        return id;
    }
}
