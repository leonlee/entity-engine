package org.ofbiz.core.entity.jdbc;

/**
 * A versionf of {@link org.ofbiz.core.entity.jdbc.SQLProcessor} that will implcitly
 * auto commit any updates that occur
 *
 *
 * It will be created with {@link org.ofbiz.core.entity.jdbc.SQLProcessor.CommitMode#AUTO_COMMIT}
 *
 * This class is used to "clearly document" the mode the SQLProcessor is in
 */
public class AutoCommitSQLProcessor extends SQLProcessor {
    public AutoCommitSQLProcessor(String helperName) {
        super(helperName, CommitMode.AUTO_COMMIT);
    }
}
