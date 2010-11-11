package org.ofbiz.core.entity.jdbc.dbtype;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.StringTokenizer;

/**
 * The super-class for all PostgreSQL Database Types. Allows a number
 * of utility methods relating to version number extraction to be
 * re-used.
 */

public abstract class AbstractPostgresDatabaseType extends AbstractDatabaseType
{

    private static final int MAJOR = 0;
    private static final int MINOR = 1;

    protected AbstractPostgresDatabaseType(String name, String fieldTypeName) {
        super(name, fieldTypeName, new String[] {"POSTGRESQL"});
    }

    public String getSchemaName (Connection con) {
        return "public";
    }

     protected int[] parseVersionStr(String version) {
        int[] versionNumber = new int[2];

         StringTokenizer versionTokens = new StringTokenizer(version, ".");

         if(versionTokens.hasMoreElements()) {
             versionNumber[MAJOR] = Integer.parseInt(versionTokens.nextToken());


             if(versionTokens.hasMoreElements()) {
                 versionNumber[MINOR] = Integer.parseInt(versionTokens.nextToken());

                 return versionNumber;
             }
         }

         return null;
    }

    protected int[] getPostgresVersion(Connection con) throws SQLException {
        DatabaseMetaData metaData = con.getMetaData();
        try {
            int[] versionNumber = new int[2];
            versionNumber[MAJOR] = metaData.getDatabaseMajorVersion();
            versionNumber[MINOR] = metaData.getDatabaseMinorVersion();
            return versionNumber;
        } catch (AbstractMethodError ame){
            return parseVersionStr(metaData.getDatabaseProductVersion());
        }
    }


    protected boolean postgresVersionGreaterThanOrEqual(Connection con,int major, int minor) throws SQLException {
        int[] vers = getPostgresVersion(con);

        return versionGreaterThanOrEqual(vers[MAJOR], vers[MINOR], major, minor);
    }

    protected boolean postgresVersionLessThanOrEqual(Connection con, int major, int minor) throws SQLException {
        int[] vers = getPostgresVersion(con);

        return versionGreaterThanOrEqual(major, minor, vers[MAJOR], vers[MINOR]);
    }
}
