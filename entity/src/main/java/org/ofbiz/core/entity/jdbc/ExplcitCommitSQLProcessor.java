package org.ofbiz.core.entity.jdbc;

/**
 * This version of the {@link SQLProcessor} that takes sets autocommit to false on its connection and then calls commit
 * on that connection when it is closed.
 *
 * It will be created with {@link org.ofbiz.core.entity.jdbc.SQLProcessor.CommitMode#EXPLICIT_COMMIT}
 *
 * This class is used to "clearly document" the mode the SQLProcessor is in
 *
 * @deprecated use ExplicitCommitSQLProcessor instead (fixes typo in name).  Since v1.0.41.
 */
@Deprecated
public class ExplcitCommitSQLProcessor extends SQLProcessor
{
    public ExplcitCommitSQLProcessor(String helperName)
    {
        super(helperName, CommitMode.EXPLICIT_COMMIT);
    }
}