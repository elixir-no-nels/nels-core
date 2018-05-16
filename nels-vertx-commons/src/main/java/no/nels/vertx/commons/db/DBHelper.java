package no.nels.vertx.commons.db;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public final class DBHelper {
    private static io.vertx.core.logging.Logger logger = LoggerFactory.getLogger(DBHelper.class);
    private static ComboPooledDataSource basicDataSource;

    public static ComboPooledDataSource getBasicDataSource() {
        return basicDataSource;
    }

    public static void init(String driver, String dbConnection, String user, String password) throws PropertyVetoException {
        basicDataSource = new ComboPooledDataSource();
        basicDataSource.setDriverClass(driver);
        basicDataSource.setJdbcUrl(dbConnection);
        basicDataSource.setUser(user);
        basicDataSource.setPassword(password);
        basicDataSource.setMaxStatements(180);
    }


    public static JsonObject getOne(String sql, String... columnNames) throws SQLException {
        try (Connection connection = basicDataSource.getConnection();
             Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet resultSet = statement.executeQuery(sql)) {
            JsonObject jsonObject = new JsonObject();
            if (resultSet.first()) {
                for (String column : columnNames) {
                    try {
                        if (resultSet.getObject(column) instanceof java.sql.Timestamp) {
                            jsonObject.put(column, ((java.sql.Timestamp) resultSet.getObject(column)).getTime());
                        } else {
                            jsonObject.put(column, resultSet.getObject(column));
                        }
                    } catch (Exception ex) {
                        logger.error(ex);
                    }
                }
            }
            return jsonObject;
        }
    }

    public static JsonObject select(String sql, String... columnNames) throws SQLException {
        try (Connection connection = basicDataSource.getConnection();
             Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet resultSet = statement.executeQuery(sql)) {

            JsonArray jsonArray = new JsonArray();
            while (resultSet.next()) {

                JsonObject jsonObject = new JsonObject();
                for (String column : columnNames) {
                    try {
                        if (resultSet.getObject(column) instanceof java.sql.Timestamp) {
                            jsonObject.put(column, ((java.sql.Timestamp) resultSet.getObject(column)).getTime());
                        } else {
                            jsonObject.put(column, resultSet.getObject(column));
                        }
                    } catch (Exception ex) {
                        logger.error(ex);
                    }
                }
                jsonArray.add(jsonObject);
            }
            JsonObject finalJsonObject = new JsonObject();
            for (String column : columnNames) {
                JsonArray values = new JsonArray();
                for (int i = 0; i < jsonArray.size(); i++) {
                    values.add(jsonArray.getJsonObject(i).getValue(column));
                }
                finalJsonObject.put(column, values);
            }
            return finalJsonObject;
        }
    }


}