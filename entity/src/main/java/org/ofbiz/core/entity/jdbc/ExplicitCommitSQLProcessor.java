package org.ofbiz.core.entity.jdbc;

/**
 * This version of the {@link org.ofbiz.core.entity.jdbc.SQLProcessor} that
 * takes sets autocommit to false on its connection and then calls commit on
 * that connection when it is closed.
 *
 * It will be created with {@link org.ofbiz.core.entity.jdbc.SQLProcessor.CommitMode#EXPLICIT_COMMIT}.
 *
 * This class is used to "clearly document" the mode the SQLProcessor is in.
 */
public class ExplicitCommitSQLProcessor extends SQLProcessor {

    /**
     * Constructor.
     *
     * @param helperName the datasource helper name
     */
    public ExplicitCommitSQLProcessor(String helperName) {
        super(helperName, CommitMode.EXPLICIT_COMMIT);
    }
}