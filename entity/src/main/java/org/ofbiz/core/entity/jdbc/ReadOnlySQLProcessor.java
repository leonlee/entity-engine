package org.ofbiz.core.entity.jdbc;

/**
 * A version of {@link org.ofbiz.core.entity.jdbc.SQLProcessor} will only issue read only SELECT sql and will never
 * touch the connections autocommit status or call commit() on the connection
 * <p/>
 * It will be created with {@link org.ofbiz.core.entity.jdbc.SQLProcessor.CommitMode#READONLY}
 * <p/>
 * This class is used to "clearly document" the mode the SQLProcessor is in
 */
public class ReadOnlySQLProcessor extends SQLProcessor {
    public ReadOnlySQLProcessor(String helperName) {
        super(helperName, CommitMode.READONLY);
    }
}
