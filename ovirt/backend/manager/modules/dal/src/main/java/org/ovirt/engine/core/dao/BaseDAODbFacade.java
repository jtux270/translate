package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.CustomMapSqlParameterSource;
import org.ovirt.engine.core.dal.dbbroker.DbEngineDialect;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.SimpleJdbcCallsHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public abstract class BaseDAODbFacade {
    protected static final String SEPARATOR = ",";

    protected JdbcTemplate jdbcTemplate;
    protected DbEngineDialect dialect;
    protected DbFacade dbFacade;

    public void setTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setDialect(DbEngineDialect dialect) {
        this.dialect = dialect;
    }

    public static final String RETURN_VALUE_PARAMETER = "RETURN_VALUE";

    public BaseDAODbFacade() {
    }

    protected CustomMapSqlParameterSource getCustomMapSqlParameterSource() {
        return new CustomMapSqlParameterSource(dialect);
    }

    /**
     * @return A generic boolean result mapper which always takes the boolean result, regardless of how it's column is
     *         called.
     */
    protected RowMapper<Boolean> createBooleanMapper() {
        return new RowMapper<Boolean>() {

            @Override
            public Boolean mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getBoolean(1);
            }
        };
    }

    /**
     * @return A generic {@link Guid} result mapper which always takes the {@link Guid} result, regardless of how it's
     *         column is called.
     */
    protected RowMapper<Guid> createGuidMapper() {
        return new RowMapper<Guid>() {

            @Override
            public Guid mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new Guid((UUID) rs.getObject(1));
            }
        };
    }

    private static RowMapper<Long> longRowMapper = new RowMapper<Long>() {
        @Override
        public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getLong(1);
        };
    };

    protected RowMapper<Long> getLongMapper() {
        return longRowMapper;
    }

    private static RowMapper<Integer> integerRowMapper = new RowMapper<Integer>() {
        @Override
        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getInt(1);
        };
    };

    protected RowMapper<Integer> getIntegerMapper() {
        return integerRowMapper;
    }

    private static RowMapper<String> stringRowMapper = new RowMapper<String>() {
        @Override
        public String mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getString(1);
        };
    };

    protected RowMapper<String> getStringMapper() {
        return stringRowMapper;
    }

    protected SimpleJdbcCallsHandler getCallsHandler() {
        return dbFacade.getCallsHandler();
    }

    public void setDbFacade(DbFacade dbFacade) {
        this.dbFacade = dbFacade;
    }

    /**
     * Returns a Double or a null if the column was NULL.
     * @param resultSet the ResultSet to extract the result from
     * @param columnName
     * @return a Long or null
     * @throws SQLException
     */
    final static Long getLong(ResultSet resultSet, String columnName) throws SQLException {
        if (resultSet.getLong(columnName) == 0 && resultSet.wasNull()) {
            return null;
        } else {
            return resultSet.getLong(columnName);
        }
    }

    /**
     * Returns a Integer or a null if the column was NULL.
     * @param resultSet the ResultSet to extract the result from
     * @param columnName
     * @return a Integer or null
     * @throws SQLException
     */
    protected final static Integer getInteger(ResultSet resultSet, String columnName) throws SQLException {
        Integer value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    /**
     * Returns a {@link Guid} representing the column's value or a default value if the column was <code>null</code>.
     *
     * <b>Note:</b> Postgres' driver returns a {@link UUID} when {@link ResultSet#getObject(String)} is called on a
     * uuid column. This behavior is trusted to work with Postgres 8.x and above and is used to achieve the best
     * performance. If it is ever broken on Postgres' side, this method should be rewritten.
     *
     * @param resultSet the ResultSet to extract the result from.
     * @param columnName the name of the column.
     * @param defaultValue The value to return if the column is <code>null</code>.
     * @return a {@link Guid} representing the UUID in the column, or the default value if it was <code>null</code>.
     * @throws SQLException If resultSet does not contain columnName or its value cannot be cast to {@link UUID}.
     */
    private static Guid getGuid(ResultSet resultSet, String columnName, Guid defaultValue) throws SQLException {
        UUID uuid = (UUID) resultSet.getObject(columnName);
        if (uuid == null || resultSet.wasNull()) {
            return defaultValue;
        }
        return new Guid(uuid);
    }

    /**
     * Returns a {@link Guid} representing the column's value or a <code>null</code>
     * if the column was <code>null</code>.
     *
     * @param resultSet the ResultSet to extract the result from.
     * @param columnName the name of the column.
     * @return a {@link Guid} representing the UUID in the column, or the default value if it was <code>null</code>.
     * @throws SQLException If resultSet does not contain columnName or its value cannot be cast to {@link UUID}.
     */
    protected static Guid getGuid(ResultSet resultSet, String columnName) throws SQLException {
        return getGuid(resultSet, columnName, null);
    }

    /**
     * Returns a {@link Guid} representing the column's value or a {@link Guid#Empty}
     * if the column was <code>null</code>.
     *
     * @param resultSet the ResultSet to extract the result from.
     * @param columnName the name of the column.
     * @return a {@link Guid} representing the UUID in the column, or the default value if it was <code>null</code>.
     * @throws SQLException If resultSet does not contain columnName or its value cannot be cast to {@link UUID}.
     */
    protected static Guid getGuidDefaultEmpty(ResultSet resultSet, String columnName) throws SQLException {
        return getGuid(resultSet, columnName, Guid.Empty);
    }

    /**
     * Returns a {@link Guid} representing the column's value or a {@link Guid#newGuid()}
     * if the column was <code>null</code>.
     *
     * @param resultSet  the ResultSet to extract the result from.
     * @param columnName the name of the column.
     * @return a {@link Guid} representing the UUID in the column, or the default value if it was <code>null</code>.
     * @throws SQLException If resultSet does not contain columnName or its value cannot be cast to {@link UUID}.
     */
    protected static Guid getGuidDefaultNewGuid(ResultSet resultSet, String columnName) throws SQLException {
        return getGuid(resultSet, columnName, Guid.newGuid());
    }

    protected static ArrayList<String> split(String str) {
        if (StringUtils.isEmpty(str)) {
            return null;
        }

        return new ArrayList<String>(Arrays.asList(str.split(SEPARATOR)));
    }
}
