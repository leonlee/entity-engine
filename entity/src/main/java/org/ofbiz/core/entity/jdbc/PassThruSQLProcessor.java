package org.ofbiz.core.entity.jdbc;

import java.sql.Connection;

/**
 * This version of the {@link org.ofbiz.core.entity.jdbc.SQLProcessor} that takes a connection on construction and does
 * not participate in committing on close.
 *
 * It will be created with {@link org.ofbiz.core.entity.jdbc.SQLProcessor.CommitMode#NOT_INVOLVED}
 *
 * This class is used to "clearly document" the mode the SQLProcessor is in
 */
public class PassThruSQLProcessor extends SQLProcessor
{
    public PassThruSQLProcessor(String helperName, Connection connection)
    {
        super(helperName, connection);
    }
}
