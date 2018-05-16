package no.nels.portal.utilities;

import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.facades.MessageFacade;
import org.apache.commons.lang.StringUtils;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;

public class JSFUtils {

    public static <T> T getManagedBean(final String beanName,
                                       final Class<T> clazz) {
        T ret = null;
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            ret = context.getApplication().evaluateExpressionGet(context,
                    "#{" + beanName + "}", clazz);
        } catch (Exception ex) {
        }
        return ret;
    }

    public static String getApplicationRoot() {
        return FacesContext.getCurrentInstance().getExternalContext()
                .getRequestContextPath();
    }

    public static String getAbsoluteUrl(String relativeUrl) {
        return getApplicationRoot() + relativeUrl;
    }

    public static String getReferrerUrl() {
        return FacesContext.getCurrentInstance().getExternalContext()
                .getRequestHeaderMap().get("referer");
    }

    public static String getRequestUrl() {
        Object request = FacesContext.getCurrentInstance().getExternalContext()
                .getRequest();
        if (request instanceof HttpServletRequest) {
            return ((HttpServletRequest) request).getRequestURL().toString();
        }
        return "";
    }



    public static void sendHtmlToUser(String html) throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext
                .getResponse();
        response.getOutputStream().write(html.getBytes());
        facesContext.responseComplete();
    }

    public static void sendJsonToUser(String jsonContent) throws Exception{
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext
                .getResponse();

        response.reset();
        response.setContentType("application/json");
        OutputStream output = response.getOutputStream();
        output.write(jsonContent.getBytes());
        output.close();

        // Inform JSF to not take the response in hands.
        facesContext.responseComplete();
    }

    public static void sendStringAsFile(String fileName,String fileContent) throws Exception{
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext
                .getResponse();

        response.reset();
        response.setContentType("text/plain");
        response.setHeader("Content-disposition", "attachment; filename=\""
                + fileName + "\"");

        OutputStream output = response.getOutputStream();

        output.write(fileContent.getBytes());
        output.close();
        // Inform JSF to not take the response in hands.
        facesContext.responseComplete();
    }

    public static void streamFileFolderToUser(String fileUrl) throws Exception {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext
                .getResponse();

        response.reset();
        response.setContentType(getContentType(fileUrl));
        response.setHeader("Content-disposition", "attachment; filename=\""
                + getFileName(fileUrl) + "\"");

        OutputStream output = response.getOutputStream();

        byte[] buffer = new byte[4096];
        InputStream inputStream = new URL(fileUrl).openStream();
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, len);
        }
        output.close();
        // Inform JSF to not take the response in hands.
        facesContext.responseComplete();
    }

    public static String getFileName(String filePath) {
        // this function assumes the file path separator "/"
        String withoutLastSlash = filePath.endsWith("/") ? StringUtils.substringBeforeLast(filePath, "/") : filePath;
        String name = filePath.contains("/") ? StringUtils.substringAfterLast(
                withoutLastSlash, "/") : withoutLastSlash;
        return filePath.endsWith("/") ? name + ".tgz" : name;
    }

    ///Caution: This function is for legacy purposes. Should be removed
    public static String getLocalFilePath(String path){
        return path;
    }

    public static String getContentType(String filePath) {
        String ret = "application/octet-stream";
        if(filePath.endsWith("/")){
            ret = "application/zip";
        }
        else if (filePath.toLowerCase().endsWith(".txt")) {
            ret = "text/plain";
        } else if (filePath.toLowerCase().endsWith(".pdf")) {
            ret = "application/pdf";
        }

        return ret;
    }

    /*
    * minimum password length - 6
    * maximum password length - 12
    * password must contain at least one number digit
    * password must contain at least one small letter
    * password must contain at least one capital letter
    */
    public static boolean checkPasswordStrength(String pwd) {
        final int MIN_PWD_LEN = 6;
        final int MAX_PWD_LEN = 12;
        final int ERROR_TYPE_LEN_LESS_THAN_SIX = 1;
        final int ERROR_TYPE_LEN_MORE_THAN_TWELVE = 2;
        final int ERROR_TYPE_NO_DIGIT = 4;
        final int ERROR_TYPE_NO_LOWERCASE_LETTER = 8;
        final int ERROR_TYPE_NO_UPPERCASE_LETTER = 16;

        int errType = 0;
        boolean ret = true;
        int len = pwd.length();
        if (len < MIN_PWD_LEN) {
            ret = false;
            errType = (errType | ERROR_TYPE_LEN_LESS_THAN_SIX);
        }
        if (len > MAX_PWD_LEN) {
            ret = false;
            errType = errType | ERROR_TYPE_LEN_MORE_THAN_TWELVE;
        }
        if (!StringUtilities.containsDigit(pwd)) {
            ret = false;
            errType = errType | ERROR_TYPE_NO_DIGIT;
        }
        if (!StringUtilities.containsLowercaseLetter(pwd)) {
            ret = false;
            errType = errType | ERROR_TYPE_NO_LOWERCASE_LETTER;
        }
        if (!StringUtilities.containsUppercaseLetter(pwd)) {
            ret = false;
            errType = errType | ERROR_TYPE_NO_UPPERCASE_LETTER;
        }

        if (!ret) {
            if (ERROR_TYPE_LEN_LESS_THAN_SIX == (errType & ERROR_TYPE_LEN_LESS_THAN_SIX)) {

                MessageFacade.invalidInput("password length is less than 6");
            }
            if (ERROR_TYPE_LEN_MORE_THAN_TWELVE == (errType & ERROR_TYPE_LEN_MORE_THAN_TWELVE)) {

                MessageFacade.invalidInput("password length is more than 12");
            }
            if (ERROR_TYPE_NO_DIGIT == (errType & ERROR_TYPE_NO_DIGIT)) {

                MessageFacade.invalidInput("password does not contain one number digit");
            }
            if (ERROR_TYPE_NO_LOWERCASE_LETTER == (errType & ERROR_TYPE_NO_LOWERCASE_LETTER)) {

                MessageFacade.invalidInput("password does not contain one lowercase letter");
            }
            if (ERROR_TYPE_NO_UPPERCASE_LETTER == (errType & ERROR_TYPE_NO_UPPERCASE_LETTER)) {

                MessageFacade.invalidInput("password does not contain one capital letter");
            }

        }
        return ret;
    }
}
