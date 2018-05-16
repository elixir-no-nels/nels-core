package no.nels.portal.facades;

import no.nels.portal.Config;
import no.nels.portal.model.DelayedMessage;
import no.nels.portal.model.enumerations.DeploymentMode;
import no.nels.portal.model.enumerations.SessionItemKeys;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

public class MessageFacade {

    public static void invalidInput(String message) {
        AddError("Invalid Input", message);
    }

    public static void deletedSuccessfully() {
        addInfo("Deleted successfully", "");
    }

    public static void savedSuccessfully() {
        addInfo("Saved successfully", "");
    }

    public static void noRowsSelected() {
        AddError("No rows selected",
                "You need to select at least one row to perform action");
    }

    public static void addInfo(String message) {
        addInfo(message, "");
    }

    public static void addInfo(String intro, String detail) {
        addInfo(intro, detail, false);
    }

    private static void addDelayedMessage(String intro, String detail, String messageTypeKey) {
        List<DelayedMessage> delayedMessages;
        if (SessionFacade.isSessionObjectSet(messageTypeKey)) {
            delayedMessages = (List<DelayedMessage>) SessionFacade.getSessionObject(messageTypeKey);
        } else {
            delayedMessages = new ArrayList<DelayedMessage>();
        }
        delayedMessages.add(new DelayedMessage(intro, detail));
        SessionFacade.setSessionObject(messageTypeKey, delayedMessages);
    }

    public static void addInfo(String intro, String detail, boolean isDelayed) {
        if (isDelayed) {
            addDelayedMessage(intro, detail, SessionItemKeys.DELAYED_MESSAGES_INFO);
        } else {
            FacesContext context = FacesContext.getCurrentInstance();
            context.getExternalContext().getFlash().setKeepMessages(true);
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, intro, detail));
        }
    }

    public static void AddWarning(String message) {
        AddWarning(message, "");
    }

    public static void AddWarning(String intro, String detail) {
        AddWarning(intro, detail, false);
    }

    public static void AddWarning(String intro, String detail, boolean isDelayed) {
        if (isDelayed) {
            addDelayedMessage(intro, detail, SessionItemKeys.DELAYED_MESSAGES_WARNING);
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, intro, detail));
        }
    }

    public static void AddError(String error) {
        AddError(error, "");
    }

    public static void AddError(String intro, String detail) {
        AddError(intro, detail, false);
    }

    public static void AddError(String intro, String detail, boolean isDelayed) {
        if (isDelayed) {
            addDelayedMessage(intro, detail, SessionItemKeys.DELAYED_MESSAGES_ERROR);
        } else {
            FacesContext context = FacesContext.getCurrentInstance();
            context.getExternalContext().getFlash().setKeepMessages(true);
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, intro, detail));
        }
    }

    public static void addFatal(String message) {
        addFatal(message, "");
    }

    public static void addFatal(String intro, String detail) {
        addFatal(intro, detail, false);
    }

    public static void addFatal(String intro, String detail, boolean isDelayed) {
        if (isDelayed) {
            addDelayedMessage(intro, detail, SessionItemKeys.DELAYED_MESSAGES_FATAL);
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_FATAL, intro, detail));
        }
    }

    public static void showException(Exception exception) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        exception.printStackTrace(printWriter);
        printWriter.flush();

        String stackTrace = writer.toString();
        if (Config.getDeploymentMode() == DeploymentMode.TEST) {
            addFatal(exception.getLocalizedMessage(), stackTrace);
        } else {
            addFatal("Error", "An error occured. Please contact NeLS adminsitrators if problem persists");
        }
    }

    public static void showWarning(String desc) {

        FacesContext context = FacesContext.getCurrentInstance();
        context.getExternalContext().getFlash().setKeepMessages(true);
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, desc, ""));
        LoggingFacade.logDebugInfo("showInfo, context:" + context.toString() + ", externalContext:" + context.getExternalContext());
    }

    public static void handleDelayedMessages() {
        if (SessionFacade.isSessionObjectSet(SessionItemKeys.DELAYED_MESSAGES_INFO)) {
            List<DelayedMessage> delayedMessages = (List<DelayedMessage>) SessionFacade.getSessionObject(SessionItemKeys.DELAYED_MESSAGES_INFO);
            for (DelayedMessage dm : delayedMessages) {
                addInfo(dm.title, dm.message, false);
            }
            delayedMessages.clear();
        }
        if (SessionFacade.isSessionObjectSet(SessionItemKeys.DELAYED_MESSAGES_WARNING)) {
            List<DelayedMessage> delayedMessages = (List<DelayedMessage>) SessionFacade.getSessionObject(SessionItemKeys.DELAYED_MESSAGES_WARNING);
            for (DelayedMessage dm : delayedMessages) {
                AddWarning(dm.title, dm.message, false);
            }
            delayedMessages.clear();
        }
        if (SessionFacade.isSessionObjectSet(SessionItemKeys.DELAYED_MESSAGES_FATAL)) {
            List<DelayedMessage> delayedMessages = (List<DelayedMessage>) SessionFacade.getSessionObject(SessionItemKeys.DELAYED_MESSAGES_FATAL);
            for (DelayedMessage dm : delayedMessages) {
                addFatal(dm.title, dm.message, false);
            }
            delayedMessages.clear();
        }
        if (SessionFacade.isSessionObjectSet(SessionItemKeys.DELAYED_MESSAGES_ERROR)) {
            List<DelayedMessage> delayedMessages = (List<DelayedMessage>) SessionFacade.getSessionObject(SessionItemKeys.DELAYED_MESSAGES_ERROR);
            for (DelayedMessage dm : delayedMessages) {
                AddError(dm.title, dm.message, false);
            }
            delayedMessages.clear();
        }
    }
}
