package no.nels.portal.utilities;


import no.nels.commons.abstracts.ASystemUser;
import no.nels.commons.model.systemusers.AdministratorUser;
import no.nels.commons.model.systemusers.HelpDeskUser;
import no.nels.commons.model.systemusers.NormalUser;

public class UserTypeUtilities {
	
	 public static String getUserTypeName(ASystemUser systemUser) {
	        if (systemUser instanceof AdministratorUser) {
	            return "Administrator";
	        } else if (systemUser instanceof HelpDeskUser) {
	            return "Help Desk";
	        } else if(systemUser instanceof NormalUser) {
	            return "Normal User";
	        }
	        else {
	        	return "";
	        }
	    }
	public static String getUserTypeName(long id){
		return getUserTypeName(no.nels.commons.utilities.UserTypeUtilities.getUserType(id));
	}
}
