package org.ofbiz.core.entity.jdbc.dbtype;

import org.ofbiz.core.entity.jdbc.DatabaseUtil;

import java.sql.Connection;
import java.sql.SQLException;

public class Oracle10GDatabaseType extends AbstractDatabaseType {
    public Oracle10GDatabaseType() {
        super("Oracle 9i and 10g", "oracle10g", new String[]{"ORACLE"});
    }

    public boolean matchesConnection(Connection con) throws SQLException {
        return productNameMatches(con) && versionGreaterThanOrEqual(con, 9, 0);
    }

    @Override
    protected String getChangeColumnTypeStructure()
    {
        return CHANGE_COLUMN_TYPE_CLAUSE_STRUCTURE_STANDARD_MODIFY;
    }

    /**
     * Warning: dirty hacks!
     * Detects oracle specific type extension. In oracle, VARCHAR2() type can be represented as:
     * <dl>
     *     <dt>VARCHAR2(x)</dt>
     *     <dt>VARCHAR2(x BYTE)</dt>
     *     <dd>Field with length in bytes, JDBC will attempt to put UTF-8 texts in there but lenght in characters cannot be
     *     guaranteed.</dd>
     *     <dt>VARCHAR(x CHAR)</dt>
     *     <dd>Field with length in characters, handling Unicode.</dd>
     * </dl>
     * @param typeName the configured field type (general, without field size).
     * @param ccInfo the JDBC supplied information on the column.
     * @param oracleSpecificExtension the declared mode of the VARCHAR2 extension, possible: "BYTE", "CHAR" or null.
     * @return true, if the column is an Oracle VARCHAR2 type column without Unicode support and the definition requires it.
     */
    public static boolean detectUnicodeWidening(final String typeName, final DatabaseUtil.ColumnCheckInfo ccInfo, String oracleSpecificExtension)
    {
        return ("VARCHAR2".equals(typeName.toUpperCase())               // only possible for VARCHAR2.
                && "CHAR".equals(oracleSpecificExtension)               // to widen, "CHAR" must be the desired extension.
                && !detectUnicodeExtension(ccInfo));                    // columns cannot be already widened.
    }

   /**
    * Unlike NVARCHAR2() the VARCHAR2(x CHAR) is Oracle specific and cannot be clearly seen through JDBC. The only
    * difference is in the declared CHAR_OCTET_LENGTH of the field, which is 4 times the declared field size. This is
    * used to detect those field types here.
    *
    * @param ccInfo the JDBC supplied information on the column.
    * @return true, if the column is an Oracle VARCHAR2 type column with Unicode support.
    */

   public static boolean detectUnicodeExtension(final DatabaseUtil.ColumnCheckInfo ccInfo)
    {
        return ("VARCHAR2".equals(ccInfo.typeName.trim().toUpperCase())
                && (4 * ccInfo.columnSize == ccInfo.maxSizeInBytes));
    }

}
