package no.nels.portal.facades;


import no.nels.client.StructuredLogger;
import no.nels.portal.utilities.GenericUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class LoggingFacade {
    private static final Logger logger = LogManager.getLogger(LoggingFacade.class);


    public static void logDebugInfo(Object obj) {
        if (logger.isDebugEnabled()) {
            logger.debug(obj);
        }
    }

    public static boolean logUserLoginEvent(long nelsId, String fullName) {

        String ip = GenericUtils.getClientIp();
        logger.debug("logUserLoginEvent:nelsId:" + nelsId + ",fullName:" + fullName + ",ip:" + ip);
        return  StructuredLogger.addUserLoginLog(nelsId, fullName, ip);

    }


}
