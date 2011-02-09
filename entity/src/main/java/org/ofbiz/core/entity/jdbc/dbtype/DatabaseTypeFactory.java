package org.ofbiz.core.entity.jdbc.dbtype;

import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.util.Debug;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This class provides methods for finding the correct database type for a given connection object.
 */
public class DatabaseTypeFactory {

    static final Collection<DatabaseType> DATABASE_TYPES = new ArrayList<DatabaseType>();


    public static final DatabaseType DB2 = new DB2DatabaseType();
    public static final DatabaseType CLOUDSCAPE = new SimpleDatabaseType("Cloudscape", "cloudscape", new String[]{"Apache Derby"});
    public static final DatabaseType FIREBIRD = new SimpleDatabaseType("Firebird", "firebird", new String[]{"Firebird"});
    public static final DatabaseType HSQL = new SimpleDatabaseType("HSQL", "hsql", new String[]{"HSQL Database Engine"});
    public static final DatabaseType H2 = new SimpleDatabaseType("H2", "h2", new String[]{"H2"});
    public static final DatabaseType MYSQL = new SimpleDatabaseType("MySQL", "mysql", new String[]{"MySQL"});
    public static final DatabaseType MSSQL = new SimpleDatabaseType("MS SQL", "mssql", new String[]{"Microsoft SQL Server"});
    public static final DatabaseType ORACLE_10G = new Oracle10GDatabaseType();
    public static final DatabaseType ORACLE_8I = new Oracle8IDatabaseType();
    public static final DatabaseType POSTGRES_7_2 = new Postgres72DatabaseType();
    public static final DatabaseType POSTGRES_7_3 = new Postgres73DatabaseType();
    public static final DatabaseType POSTGRES = new PostgresDatabaseType();
    public static final DatabaseType SAP_DB = new SapDBDatabaseType();
    public static final DatabaseType SAP_DB_7_6 = new SapDB76DatabaseType();
    public static final DatabaseType SYBASE = new SimpleDatabaseType("Sybase", "sybase", new String[]{"Adaptive Server", "sql server"});

//    // ToDo: Test McKoiDB and find what product name string should be used
//    public static final DatabaseType MCKOI_DB = new AbstractDatabaseType("Mckoi SQL", "mckoidb", "All versions of Mckoi SQL", new String[]{"false"}) {
//        public boolean matchesConnection(Connection con) {
//            return false;
//        }
//    };
//// ToDo: Test Frontbase and see what product name string should be used
//    public static final DatabaseType FRONTBASE = new AbstractDatabaseType("Frontbase", "frontbase", "All versions of Frontbase", new String[]{"false"}) {
//        public boolean matchesConnection(Connection con) {
//            return false;
//        }
//    };


    /**
     * Database Types that wish to be registered for matching should call this method, and pass themselves.
     *
     * @see org.ofbiz.core.entity.jdbc.dbtype.AbstractDatabaseType#registerWithFactory()
     */
    public static void registerDatabaseType(DatabaseType databaseType)
    {
        DATABASE_TYPES.add(databaseType);
    }


    /**
     * This method finds the DatabaseType for the connection you pass in
     *
     * @return A matching DatabaseType for the connection passed in or null if no appropriate DatabaseType is found null
     */
    public static DatabaseType getTypeForConnection(Connection con) {

        for (DatabaseType databaseType : DATABASE_TYPES) {
            try {
                if (databaseType.matchesConnection(con)) {
                    Debug.logInfo("Returning DatabaseType " + databaseType);
                    return databaseType;
                }
            } catch (Exception e) {
                Debug.logError(e, "Exception occured while trying to match the database connection to a DatabaseType");
            }
        }

        logDatabaseNotMatchedError(con);
        return null;
    }

    private static void logDatabaseNotMatchedError(Connection con) {
        try {
            final DatabaseMetaData md = con.getMetaData();
            Debug.logError("Could not determine database type.  Database meta-data: " + md.getDatabaseProductName() +
                    " " + md.getDatabaseProductVersion() + ".  Driver meta-data: " + md.getDriverName() + " " + md.getDriverVersion() + ".");
            Debug.logError("Please specify field type name in entityengine.xml.  Replace " + DatasourceInfo.AUTO_FIELD_TYPE +
                    " with specific field type name.");
        } catch (SQLException e) {
            Debug.logError(e, "Could not determine database type. SQLException occured when trying to retrieve connection meta-data");
        }
    }

//    private static void printConnectionMetaData(Connection con) {
//        try {
//            DatabaseMetaData metaData = con.getMetaData();
//
//            System.out.println("Trying to sniff a connection with the following metadata");
//
//            System.out.println("Product Name:" + metaData.getDatabaseProductName());
//            System.out.println("Product Version:" + metaData.getDatabaseProductVersion());
//            System.out.println("Driver Name:" + metaData.getDriverName());
//            System.out.println("Driver Version:" + metaData.getDriverVersion());
//            System.out.println("Connection URL:" + metaData.getURL());
//
//            System.out.println("Product Major Version:" + metaData.getDatabaseMajorVersion());
//            System.out.println("Product Minor Version:" + metaData.getDatabaseMinorVersion());
//        } catch (SQLException se) {
//            se.printStackTrace(System.out);
//        } catch (AbstractMethodError ame) {
//            ame.printStackTrace(System.out);
//        }
//
//    }

}
