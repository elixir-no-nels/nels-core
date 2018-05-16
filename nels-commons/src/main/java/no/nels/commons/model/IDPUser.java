package no.nels.commons.model;

import no.nels.commons.abstracts.AIDp;
import org.apache.commons.lang.StringUtils;


public final class IDPUser {

    public IDPUser(AIDp idp, String idpUsername, String name, String email, String affiliation) {
        this.idp = idp;
        this.idpUsername = idpUsername;
        this.firstname = StringUtils.substringBefore(
                name, " ");
        this.lastname = StringUtils.substringAfter(
                name, " ");
        this.email = email;
        this.affiliation = affiliation;
    }

    public String getIdpUsername() {
        return idpUsername;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getEmail() {
        return email;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public AIDp getIdp() {
        return idp;
    }

    public String getFullname() {
        return this.firstname + " " + this.lastname;
    }

    private AIDp idp;
    private String idpUsername;
    private String firstname;
    private String lastname;
    private String email;
    private String affiliation;
}
