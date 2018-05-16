package no.nels.eup.core.model.db;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SettingMapper implements RowMapper<Setting> {
	@Override
	public Setting mapRow(ResultSet rs, int rowNum) throws SQLException {
		Setting ret = new Setting();
		ret.setId(rs.getLong("id"));
		ret.setSettingKey(rs.getString("setting_key"));
		ret.setSettingValue(rs.getString("setting_value"));
		ret.setLastUpdate(rs.getDate("lastupdate"));
		return ret;
	}

}
