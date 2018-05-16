package no.nels.commons.utilities.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class DBDAO {
	private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @SuppressWarnings("unchecked")
	public <T> List<T> executeQuery(String cmd, RowMapper<T> mapper) {
		return jdbcTemplate.query(cmd, mapper);
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> executeQuery(String cmd, Object[] params,
			RowMapper<T> mapper) {
		return jdbcTemplate.query(cmd, params, mapper);
	}

	public <T> List<T> executeQueryBySingleColumn(String cmd, Object[] params, final String columnName, final Class<T> columnReturnType) {
		return jdbcTemplate.query(cmd, params, new RowMapper<T>() {
			@Override
			public T mapRow(ResultSet resultSet, int rowNum) throws SQLException {
				return columnReturnType.cast(resultSet.getObject(columnName));
			}
		});
	}

    public <T> T executeQueryForSingleResult(String cmd, Object[] args, RowMapper<T> rowMapper) {
        return jdbcTemplate.queryForObject(cmd, args, rowMapper);
    }

	public <T> T getById(String tableName, long id, RowMapper mapper) {
		return getByColumn(tableName, "id", id, mapper);
	}

	public <T> T getByColumn(String tableName, String columnName,
			Object columnValue, RowMapper mapper) {
		String cmd = "select * from " + tableName + " where " + columnName
				+ "=?";
		List<T> found = executeQuery(cmd, new Object[] { columnValue }, mapper);
		if (found.size() == 1) {
			return found.get(0);
		} else {
			return null;
		}
	}

    public long getCount(String tableName) {
        String cmd = "select count(*) from " + tableName;
        return executeScalar(cmd);
    }

	public boolean executeNonQuery(String cmd) {
		return executeNonQuery(new String[] {cmd});
	}

    @Transactional
    public boolean executeNonQuery(String[] cmds) {
        for (String cmd : cmds) {
            if (jdbcTemplate.update(cmd) == 0) {
                throw new RuntimeException("failed to execute non query");
            }
        }
        return true;
    }

	public boolean executeNonQuery(String cmd, final Object[] params) {
        return executeNonQuery(new String[] {cmd}, new ArrayList<Object[]>(){{
            this.add(params);
        }});
	}

    @Transactional
    public boolean executeNonQuery(String[] cmds, List<Object[]> paramsList) {
        for (int i = 0; i < cmds.length; i++) {
            if (jdbcTemplate.update(cmds[i], (Object[]) paramsList.get(i)) == 0) {
                throw new RuntimeException("failed to execute non query");
            }
        }
        return true;
    }

	public long executeScalar(String cmd) {
		return jdbcTemplate.queryForLong(cmd);
	}

	public long executeScalar(String cmd, Object[] params) {
		return jdbcTemplate.queryForLong(cmd, params);
	}

}
