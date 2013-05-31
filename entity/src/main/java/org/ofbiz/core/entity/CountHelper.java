package org.ofbiz.core.entity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Creates a count query
 *
 * @since v1.0.35
 */
public class CountHelper {
    private final static String SELECT = "SELECT COUNT(";
    private final static String DISTINCT= "DISTINCT ";
    private final static String WHERE = " WHERE ";
    private final static String FROM = " FROM ";

    /**
     * returns the select statement as SELECT COUNT([DISTINCT] fieldName) FROM tableName WHERE whereClause
     * @param tableName the table to query
     * @param field    the field to query - if it is null use * as the fieldName
     * @param whereClause   the WHERE clause, if specified
     * @param isDistinct  whether to add distinct to the query
     * @return
     */


    public String buildCountSelectStatement(final @Nonnull String tableName, final @Nullable String field,
                          final @Nullable String whereClause, final boolean isDistinct) {
        final StringBuilder builder = new StringBuilder(SELECT);
        if (field == null) {
            builder.append('*');
        } else {
            if (isDistinct) {
                builder.append(DISTINCT);
            }
            builder.append(field);
        }
        builder.append(')');
        builder.append(FROM);
        builder.append(tableName);
        if (whereClause != null) {
            builder.append(WHERE);
            builder.append(whereClause);
        }
        return builder.toString();
    }
}
