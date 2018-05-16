package no.nels.idp.core;

import no.nels.idp.core.utilities.IdpSpringUtilities;
import no.nels.commons.utilities.db.DBDAO;
import no.nels.idp.core.enumerations.IdpSpringBeanNames;

public class Config {
	static DBDAO idpDBHelper = IdpSpringUtilities.getSpringBean(IdpSpringBeanNames.idpDBHelper);

	public static DBDAO getIdpDBHelper(){
		return idpDBHelper;
	}
}
