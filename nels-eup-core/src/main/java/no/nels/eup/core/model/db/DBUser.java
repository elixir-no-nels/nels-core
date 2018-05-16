package no.nels.eup.core.model.db;

import java.util.Date;

import no.nels.commons.abstracts.*;

public class DBUser extends ANumberId {
	long id;
	int idpNumber;
	String idpUsername;
	int userType;
	Date registrationDate;
	boolean isActive;
	String name;
	String email;
	String affiliation;

	public String getAffiliation() {
		return affiliation;
	}

	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}

	@Override
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getIdpNumber() {
		return idpNumber;
	}

	public void setIdpNumber(int idpNumber) {
		this.idpNumber = idpNumber;
	}

	public String getIdpUsername() {
		return idpUsername;
	}

	public void setIdpUsername(String idpUsername) {
		this.idpUsername = idpUsername;
	}

	public int getUserType() {
		return userType;
	}

	public void setUserType(int userType) {
		this.userType = userType;
	}

	public Date getRegistrationDate() {
		return registrationDate;
	}

	public void setRegistrationDate(Date registrationDate) {
		this.registrationDate = registrationDate;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}
