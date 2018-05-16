package no.nels.eup.core.model.db;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBProjectUserMapper implements RowMapper<DBProjectUser> {
    @Override
    public DBProjectUser mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        DBProjectUser ret = new DBProjectUser();
        ret.setId(resultSet.getLong("id"));
        ret.setProjectId(resultSet.getLong("project_id"));
        ret.setUserId(resultSet.getLong("user_id"));
        ret.setMembershipType(resultSet.getLong("membership_type"));
        return ret;
    }
}
