package no.nels.idp.core.model.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

/**
 * Created by Kidane on 27.05.2015.
 */
public class DBIdpMapper implements RowMapper<NeLSIdpUser> {

    @Override
    public NeLSIdpUser mapRow(ResultSet rs, int rowNum) throws SQLException{
        NeLSIdpUser ret = new NeLSIdpUser();
        ret.setId(rs.getLong("id"));
        ret.setUsername(rs.getString("username"));
        ret.setPassword(rs.getString("password"));
        ret.setCreationDate(rs.getDate("creationdate"));
        ret.setActive(rs.getBoolean("isactive"));
        ret.setFirstName(rs.getString("first_name"));
        ret.setLastName(rs.getString("last_name"));
        ret.setEmail((rs.getString("email")));
        ret.setAffiliation(rs.getString("affiliation"));
        return ret;
    }
}
