package no.nels.master.api;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import no.nels.vertx.commons.db.DBHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by weizhang on 5/30/16.
 */
public class JDBCHelper {

    private static Logger logger = LoggerFactory.getLogger(JDBCHelper.class);


    public static void update(long jobId, long jobStatus, int completion) throws SQLException{

        logger.debug("JDBCHelper thread:" + Thread.currentThread());
        String updateTableSQL = "UPDATE job SET stateid = ?, completion=?, lastupdate=to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS')"
                + " WHERE id = ?";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = dateFormat.format(new Date());
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        try {
            connection = DBHelper.getBasicDataSource().getConnection();
            preparedStatement = connection.prepareStatement(updateTableSQL);

            preparedStatement.setLong(1, jobStatus);
            preparedStatement.setInt(2, completion);
            preparedStatement.setString(3, timestamp);
            preparedStatement.setLong(4, jobId);
            // execute update SQL statement
            int a = preparedStatement.executeUpdate();
            logger.debug("update int:" + a);
        } catch (SQLException e) {
            logger.error("update sql err:" + e.getLocalizedMessage());
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null){
                    connection.close();
                }
            }catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
