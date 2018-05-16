package no.nels.eup.core.model.db;


import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBProjectMapper implements RowMapper<DBProject> {
    @Override
    public DBProject mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        DBProject ret = new DBProject();
        ret.setId(resultSet.getLong("id"));
        ret.setName(resultSet.getString("name"));
        ret.setDescription(resultSet.getString("description"));
        ret.setCreationDate(resultSet.getDate("creation_date"));
        return ret;
    }
}
