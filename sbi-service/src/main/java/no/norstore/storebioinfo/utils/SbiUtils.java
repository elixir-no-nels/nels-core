package no.norstore.storebioinfo.utils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import no.norstore.storebioinfo.Config;
import no.norstore.storebioinfo.constants.ConfigName;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

public final class SbiUtils {
    private static Logger logger = LoggerFactory.getLogger(SbiUtils.class);

    public static Connection createMqConnection() throws IOException, TimeoutException, NoSuchAlgorithmException, KeyManagementException{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(Config.valueOf(ConfigName.MQ_HOST));
        factory.setUsername(Config.valueOf(ConfigName.MQ_USER));
        factory.setPassword(Config.valueOf(ConfigName.MQ_PASSWORD));
        factory.setVirtualHost(Config.valueOf(ConfigName.MQ_VIRTUAL_HOST));
        factory.setRequestedHeartbeat(60);
        factory.setNetworkRecoveryInterval(10000);
        factory.setConnectionTimeout(5000);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setTopologyRecoveryEnabled(true);
        factory.setPort(Integer.parseInt(Config.valueOf(ConfigName.MQ_PORT)));
        if (Config.valueOf(ConfigName.MQ_SSL).equals("true")) {
            factory.useSslProtocol();
        }

        return factory.newConnection();
    }

    public static Channel createMqPublisherChannel(Connection connection, String exchangeName) throws IOException {
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(exchangeName, "topic", true);
        return channel;
    }

    public static void sendIrodsExceptionByEmail(String datasetId, String action, String message) {
        try {
            HtmlEmail email = new HtmlEmail();
            email.setHostName(Config.valueOf(ConfigName.MAIL_SMTP_HOST));

            String[] receiverList = Config.valueOf(ConfigName.ADMIN_EMAIL).split(",");

            for (String receiver : receiverList) {
                email.addTo(receiver);
            }
            receiverList = Config.valueOf(ConfigName.PEOPLE_INVOLVED_EMAIL).split(",");
            for (String receiver : receiverList) {
                email.addCc(receiver);
            }

            email.setFrom(Config.valueOf(ConfigName.FROM_ADDRESS), "StoreBioinfo");
            email.setSubject("Irods exception occurred");

            String text = " dataset: " + datasetId;

            email.setHtmlMsg("<html> <head><style>"
                    + "body {margin:3px 0px; padding:0px;} #Content {width:800px; margin:0px auto;text-align:left;padding:15px;} </style>"
                    + "</head><body><div id='Content'><h3>Irods exception occurred when " + action + text + "</h3> <p>"
                    + message
                    + "</p><p></p><p>Please look into details in catalina.out and storebioinfo.log</p>"
                    + "</div</body></html>");
            email.send();
        } catch (EmailException e) {
            logger.error(e);
        }
    }
}

