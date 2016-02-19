package org.ofbiz.core.entity;

import org.apache.log4j.Logger;
import org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptor;
import org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptorFactory;
import org.ofbiz.core.entity.jdbc.interceptors.connection.ConnectionPoolState;
import org.ofbiz.core.entity.jdbc.interceptors.connection.SQLConnectionInterceptor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * An SQLInterceptorFactory that creates SQLInterceptor instances that simply log their inputs.
 */
public class LoggingSQLInterceptorFactory implements SQLInterceptorFactory {

    private static final Logger LOGGER = Logger.getLogger(LoggingSQLInterceptorFactory.class);

    private static final String BEFORE_TEMPLATE = "About to execute '%s'\n... with parameter values: %s";
    private static final String CONNECTION_TAKEN_TEMPLATE = "Took connection %s, pool state = %s";
    private static final String CONNECTION_REPLACED_TEMPLATE = "Replaced connection %s, pool state = %s";
    private static final String AFTER_SUCCESS_TEMPLATE = "Successfully executed '%s'\n... updated %d rows";
    private static final String ON_EXCEPTION_TEMPLATE = "Error executing '%s'\n... error = %s";

    @Override
    public SQLInterceptor newSQLInterceptor(final String ofbizHelperName) {
        return new SQLConnectionInterceptor() {
            @Override
            public void beforeExecution(final String sqlString, final List<String> parameterValues,
                                        final Statement statement) {
                LOGGER.debug(String.format(BEFORE_TEMPLATE, sqlString, parameterValues));
            }

            @Override
            public void afterSuccessfulExecution(final String sqlString, final List<String> parameterValues,
                                                 final Statement statement, final ResultSet resultSet, final int rowsUpdated) {
                LOGGER.debug(String.format(AFTER_SUCCESS_TEMPLATE, sqlString, rowsUpdated));
            }

            @Override
            public void onException(final String sqlString, final List<String> parameterValues,
                                    final Statement statement, final SQLException sqlException) {
                LOGGER.debug(String.format(ON_EXCEPTION_TEMPLATE, sqlString, sqlException));
            }

            @Override
            public void onConnectionTaken(final Connection connection, final ConnectionPoolState connectionPoolState) {
                LOGGER.debug(String.format(CONNECTION_TAKEN_TEMPLATE, connection, connectionPoolState));
            }

            @Override
            public void onConnectionReplaced(final Connection connection, final ConnectionPoolState connectionPoolState) {
                LOGGER.debug(String.format(CONNECTION_REPLACED_TEMPLATE, connection, connectionPoolState));
            }
        };
    }
}
