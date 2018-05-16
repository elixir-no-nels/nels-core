package no.nels.storage;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import no.nels.storage.constants.ConfigName;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

public final class Utils {
    public static String getNelsUserRootPath(int nelsId) {
        return StringUtils.join(Config.valueOf(ConfigName.USER_ROOT), FileSystems.getDefault().getSeparator(), nelsIdToNelsName(String.valueOf(nelsId)));
    }

    public static String output(InputStream inputStream) throws IOException {
        //for writing the output of executing shell scripts

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    public static boolean isNumeric(String str)
    {
        for (char c : str.toCharArray())
        {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

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

    public static String nelsIdToNelsName(String nelsId) {
        return "u" + Integer.toHexString(Integer.parseInt(nelsId) + 4200);
    }
}
