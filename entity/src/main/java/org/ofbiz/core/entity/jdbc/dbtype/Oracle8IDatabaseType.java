package org.ofbiz.core.entity.jdbc.dbtype;

import org.ofbiz.core.util.Debug;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.StringTokenizer;

class Oracle8IDatabaseType extends AbstractDatabaseType {
    final String PRODUCT_VERSION_PREFIX = "Oracle8i Enterprise Edition Release ";

    public Oracle8IDatabaseType() {
        super("Oracle 8i", "oracle", new String[]{"Oracle"});
    }

    public boolean matchesConnection(Connection con) throws SQLException {
        DatabaseMetaData metaData = con.getMetaData();

        int majorVersion = 0;
        int minorVersion = 0;

        try {
            majorVersion = metaData.getDatabaseMajorVersion();
            minorVersion = metaData.getDatabaseMinorVersion();
        } catch (AbstractMethodError ame) {
            // The standard oracle8 driver was written to the JDK 1.3 DatabaseMetaData interface
            //  which doesn't have the major and minor version numbers. We need to parse the
            //  version string ourselves.
            try {
                System.out.println("DatabaseType.matchesConnection: AbstractMethodException encountered " + ame);
                String dbVersion = metaData.getDatabaseProductVersion();

                if ((dbVersion != null) && (dbVersion.length() > PRODUCT_VERSION_PREFIX.length())) {
                    StringTokenizer versionTokens = new StringTokenizer(dbVersion.substring(PRODUCT_VERSION_PREFIX.length()), ".");

                    if (versionTokens.hasMoreElements()) {
                        majorVersion = parseVersionToken(versionTokens.nextToken());
                    }

                    if (versionTokens.hasMoreElements()) {
                        minorVersion = parseVersionToken(versionTokens.nextToken());
                    }
                }
            } catch (Exception e) {
                System.out.println("DatabaseType.matchesConnection: Exception occured while parsing version number string." + e);
                return false;
            }
        }

        // smaller than 9 is the same as smaller or equal to 8.99999999 :)
        return productNameMatches(con) && versionGreaterThanOrEqual(8, Integer.MAX_VALUE, majorVersion, minorVersion);
    }

    /**
     * Wraps the Integer.parseInt() method and returns 0 if there is a NumberFormatException thrown.
     *
     * @param token
     */
    private static int parseVersionToken(String token) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException nfe) {
            Debug.log(nfe, "Unable to parse version number token " + token + ". Returning 0.");
            return 0;
        }
    }

    @Override
    public String getSimpleSelectSqlSyntax(boolean clusterMode) {
        if (clusterMode) {
            return STANDARD_SELECT_FOR_UPDATE_SYNTAX;
        } else {
            return STANDARD_SELECT_SYNTAX;
        }
    }

}
