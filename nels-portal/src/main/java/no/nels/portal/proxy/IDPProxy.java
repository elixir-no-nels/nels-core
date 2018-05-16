package no.nels.portal.proxy;

import no.nels.client.Settings;
import no.nels.commons.utilities.StringUtilities;
import no.nels.idp.core.model.db.NeLSIdpUser;
import no.nels.portal.Config;
import no.nels.portal.facades.LoggingFacade;
import no.nels.portal.facades.MailFacade;
import no.nels.portal.facades.MessageFacade;
import no.nels.portal.model.enumerations.IDPKeys;
import no.nels.portal.model.enumerations.URLParameterNames;

/**
 * Created by Kidane on 06.10.2015.
 */
public class IDPProxy {

        public static boolean TriggerPasswordReset(NeLSIdpUser idpUser,String mailMessage) {
            //generate and save reset key
            String randomKey = StringUtilities.getRandomString(50);
            Settings.setSetting(IDPKeys.PASSWORD_RESET_KEY, idpUser.getId(), randomKey + "," + idpUser.getPassword());
            //send an email to the user
            String resetPwdLink = StringUtilities.appendUrlParameter(Config.getApplicationRootURL() + "/pages/resetpassword.xhtml", URLParameterNames.KEY, randomKey);
            resetPwdLink = StringUtilities.appendUrlParameter(resetPwdLink, URLParameterNames.ID, String.valueOf(idpUser.getId()));
            try {
                String msg = mailMessage;
                msg = msg.replace("?fullName", idpUser.getFullName())
                        .replace("?resetLinkUrl", resetPwdLink)
                        .replace("?webappUrl", Config.getApplicationRootURL());

                MailFacade.sendMail(Config.getSenderEmail(), new String[]{idpUser.getEmail()}, null, null,
                        "Reset password", msg, false);

            } catch (Exception ex) {
                LoggingFacade.logDebugInfo(ex);
                MessageFacade.addFatal(ex.getMessage());
                return false;
            }
            return true;
        }
}
