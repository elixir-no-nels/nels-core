package no.nels.portal;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import no.nels.client.sbi.SbiConfig;
import no.nels.client.tsd.TsdConfig;
import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.facades.MailFacade;
import no.nels.portal.model.Oauth2Config;
import no.nels.portal.model.enumerations.DeploymentMode;
import no.nels.portal.model.enumerations.LoginMode;
import org.apache.commons.io.IOUtils;

import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Config {

    public static LoginMode getLoginMode() {
        return loginMode;
    }

    public static void setLoginMode(LoginMode loginMode) {
        Config.loginMode = loginMode;
    }

    public static int getUrlRandomKeyLength() {
        return urlRandomKeyLength;
    }

    public static void setUrlRandomKeyLength(int urlRandomKeyLength) {
        Config.urlRandomKeyLength = urlRandomKeyLength;
    }

    public static String getMailHost() {
        return mailHost;
    }

    public static void setMailHost(String mailHost) {
        Config.mailHost = mailHost;
    }

    public static String getMailUser() {
        return mailUser;
    }

    public static void setMailUser(String mailUser) {
        Config.mailUser = mailUser;
    }

    public static String getMailPassword() {
        return mailPassword;
    }

    public static void setMailPassword(String mailPassword) {
        Config.mailPassword = mailPassword;
    }

    public static String getMessageFrom() {
        return messageFrom;
    }

    public static void setMessageFrom(String messageFrom) {
        Config.messageFrom = messageFrom;
    }

    public static String getHelpdeskEmail() {
        return helpdeskEmail;
    }

    public static void setHelpdeskEmail(String helpdeskEmail) {
        Config.helpdeskEmail = helpdeskEmail;
    }

    public static String getApplicationRootURL() {
        return applicationRootURL;
    }

    public static String[] getRegistrationNotificationEmails() {
        return registrationNotificationEmails;
    }

    public static String getMailRegistrationRequest() {
        return mailRegistrationRequest;
    }

    public static String getMailResetPassword() {
        return mailResetPassword;
    }

    public static String getMailNewNeLSIdpUser() {
        return mailNewNeLSIdpUser;
    }

    public static String getMailAdminResetNeLSIdpUser() {
        return mailAdminResetNeLSIdpUser;
    }

    public static String getSenderEmail() {
        return senderEmail;
    }

    public static DeploymentMode getDeploymentMode() {
        return deploymentMode;
    }

    public static String getEncryptionSalt() {
        return encryptionSalt;
    }

    public static String getDownloadUrl() {
        return downloadUrl;
    }

    public static Oauth2Config getOauth2Config() {
        return oauth2Config;
    }

    public static String getFingerprint() {
        return fingerprint;
    }

    public static String getPublicApiUrl() {
        return publicApiUrl;
    }


    private static String publicApiUrl;
    private static String downloadUrl;
    private static String mailHost = "smtp.uib.no";
    private static String mailUser = "";
    private static String mailPassword = "";
    private static String messageFrom = "contact@bioinfo.no";
    private static String helpdeskEmail = "kidane.tekle@cbu.uib.no";
    private static String senderEmail = "kidane.tekle@cbu.uib.no";
    private static String[] registrationNotificationEmails = new String[]{"kidane.tekle@cbu.uib.no"};
    private static LoginMode loginMode = LoginMode.NONDEVELOPMENT;
    private static int urlRandomKeyLength = 30;
    private static Properties portalProperties;
    private static String encryptionSalt = "something-non-empty";
    private static String applicationRootURL = "https://nels.bioinfo.no";
    private static String mailRegistrationRequest = "";
    private static String mailResetPassword = "";
    private static String mailNewNeLSIdpUser = "";
    private static String mailAdminResetNeLSIdpUser = "";
    private static DeploymentMode deploymentMode = DeploymentMode.PROD;
    private static String fingerprint = "";
    private static Oauth2Config oauth2Config = null;

    static {
        portalProperties = new Properties();
        try {
            // load properties
            portalProperties.load(Config.class.getClassLoader()
                    .getResourceAsStream("no.nels.portal.properties"));

            //fingerprint of storage server
            fingerprint = portalProperties.getProperty("fingerprint");
            //download url
            downloadUrl = portalProperties.getProperty("downloadUrl");
            //portal url
            applicationRootURL = portalProperties.getProperty("applicationRootURL");
            //emails
            helpdeskEmail = portalProperties.getProperty("helpdeskEmail");
            senderEmail = portalProperties.getProperty("senderEmail");
            //mail templates
            mailResetPassword = IOUtils.toString(Config.class.getClassLoader().getResourceAsStream("mail-password-reset.txt")).replace("?webappUrl", getApplicationRootURL());
            mailRegistrationRequest = IOUtils.toString(Config.class.getClassLoader().getResourceAsStream("mail-registration-request.txt")).replace("?webappUrl", getApplicationRootURL());
            mailNewNeLSIdpUser = IOUtils.toString(Config.class.getClassLoader().getResourceAsStream("mail-new-idpuser.txt")).replace("?webappUrl", getApplicationRootURL());
            mailAdminResetNeLSIdpUser = IOUtils.toString(Config.class.getClassLoader().getResourceAsStream("mail-admin-reset.txt")).replace("?webappUrl", getApplicationRootURL());
            //deployment mode
            String deploymentModeString = portalProperties.getProperty("deploymentMode");
            deploymentMode = deploymentModeString.equalsIgnoreCase("PROD") ? DeploymentMode.PROD : (deploymentModeString.equalsIgnoreCase("TEST") ? DeploymentMode.TEST : (deploymentModeString.equalsIgnoreCase("PRODPARALLEL") ? DeploymentMode.PRODPARALLEL : DeploymentMode.DEV));
            //idp - security
            encryptionSalt = portalProperties.getProperty("encryptionSalt");
            //http request timeout

            //public api url
            publicApiUrl = portalProperties.getProperty("publicApiUrl");

            //storage api
            no.nels.client.Config.setTimeout(portalProperties.getProperty("timeout"));
            no.nels.client.Config.setApiUrl(portalProperties.getProperty("storageapi"));
            no.nels.client.Config.setExtraApiUrl(portalProperties.getProperty("extraStorageApi"));
            no.nels.client.Config.setExtraApiUsername(portalProperties.getProperty("extraStorageUsername"));
            no.nels.client.Config.setExtraApiPassword(portalProperties.getProperty("extraStoragePassword"));

            no.nels.client.Config.setMasterApiUrl(portalProperties.getProperty("masterApiUrl"));
            no.nels.client.Config.setMasterApiUsername(portalProperties.getProperty("masterApiUsername"));
            no.nels.client.Config.setMasterApiPassword(portalProperties.getProperty("masterApiPassword"));

            SbiConfig.setTimeout(portalProperties.getProperty("timeout"));
            SbiConfig.setSbiRootUrl(portalProperties.getProperty("sbiApiUrl"));
            SbiConfig.setSbiApiUsername(portalProperties.getProperty("sbiApiUsername"));
            SbiConfig.setSbiApiPassword(portalProperties.getProperty("sbiApiPassword"));
            SbiConfig.setMasterApiUrl(portalProperties.getProperty("masterApiUrl"));
            SbiConfig.setMasterApiUsername(portalProperties.getProperty("masterApiUsername"));
            SbiConfig.setMasterApiPassword(portalProperties.getProperty("masterApiPassword"));

            TsdConfig.setTimeout(portalProperties.getProperty("timeout"));
            TsdConfig.setTsdRootUrl(portalProperties.getProperty("tsdApiUrl"));
            TsdConfig.setTsdApiUsername(portalProperties.getProperty("tsdApiUsername"));
            TsdConfig.setTsdApiPassword(portalProperties.getProperty("tsdApiPassword"));
            TsdConfig.setMasterApiUrl(portalProperties.getProperty("masterApiUrl"));
            TsdConfig.setMasterApiUsername(portalProperties.getProperty("masterApiUsername"));
            TsdConfig.setMasterApiPassword(portalProperties.getProperty("masterApiPassword"));

            //initialize oauth2 things
            Gson gson = new Gson();
            String jsonFile = Config.class.getClassLoader().getResource("oauth2.json").getPath();
            oauth2Config = gson.fromJson(new JsonReader(new FileReader(jsonFile)),Oauth2Config.class);
            oauth2Config.indexClients();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
