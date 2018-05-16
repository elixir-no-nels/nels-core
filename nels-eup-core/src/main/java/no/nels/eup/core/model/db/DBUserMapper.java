package no.nels.eup.core.model.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class DBUserMapper implements RowMapper<DBUser> {
	@Override
	public DBUser mapRow(ResultSet rs, int rowNum) throws SQLException {
		DBUser ret = new DBUser();
		ret.setId(rs.getLong("id"));
		ret.setIdpNumber(rs.getInt("idpnumber"));
		ret.setIdpUsername(rs.getString("idpusername"));
		ret.setUserType(rs.getInt("usertype"));
		ret.setRegistrationDate(rs.getDate("registrationdate"));
		ret.setActive(rs.getBoolean("isactivie"));
		ret.setName(rs.getString("name"));
		ret.setEmail(rs.getString("email"));
		ret.setAffiliation(rs.getString("affiliation"));
		return ret;
	}

}
