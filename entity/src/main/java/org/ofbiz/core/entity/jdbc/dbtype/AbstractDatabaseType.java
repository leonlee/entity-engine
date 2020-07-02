package org.ofbiz.core.entity.jdbc.dbtype;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractDatabaseType implements DatabaseType {

    private final String name;
    private String sqlKeywords;

    protected static final String CHANGE_COLUMN_TYPE_CLAUSE_STRUCTURE_STANDARD_ALTER_COLUMN = "ALTER TABLE {0} ALTER COLUMN {1} {2}";
    protected static final String CHANGE_COLUMN_TYPE_CLAUSE_STRUCTURE_STANDARD_MODIFY = "ALTER TABLE {0} MODIFY {1} {2}";

    protected static final String DROP_INDEX_SCHEMA_DOT_INDEX = "DROP INDEX {schemaName_with_dot}{indexName}";
    protected static final String DROP_INDEX_SCHEMA_DOT_TABLE_DOT_INDEX = "DROP INDEX {schemaName_with_dot}{tableName}.{indexName}";
    protected static final String ALTER_TABLE_DROP_INDEX = "ALTER TABLE {schemaName_with_dot}{tableName} DROP INDEX {indexName}";

    protected static final String STANDARD_SELECT_FOR_UPDATE_SYNTAX = "SELECT {0} FROM {1} WHERE {2} FOR UPDATE";

    /**
     * The name that should be used in entityengine.xml (eg. postgres72, oracle10g
     */
    private final String fieldTypeName;

    private final String[] productNamePrefix;

    private final int constraintNameClipLength;

    private static final int STANDARD_CONSTRAINT_NAME_CLIP_LENGTH = 30;

    private DatabaseMetaData databaseMetadata;


    protected AbstractDatabaseType(String name, String fieldTypeName, String[] productNamePrefix, int constraintNameClipLength) {
        this.name = name;
        this.fieldTypeName = fieldTypeName;
        this.productNamePrefix = productNamePrefix;
        this.constraintNameClipLength = constraintNameClipLength;
        registerWithFactory();
    }

    protected AbstractDatabaseType(String name, String fieldTypeName, String[] productNamePrefix) {
        this(name, fieldTypeName, productNamePrefix, STANDARD_CONSTRAINT_NAME_CLIP_LENGTH);
    }

    /**
     * @return A human readable version of the database type name
     */
    public String getName() {
        return name;
    }

    /**
     * @return The value that should be used for the field-type-name for a database of this type.
     */
    public String getFieldTypeName() {
        return fieldTypeName;
    }

    public String toString() {
        return name;
    }

    public String getSchemaName(Connection con) {
        return null;
    }

    public int getConstraintNameClipLength() {
        return constraintNameClipLength;
    }

    /**
     * Checks whether the connection object passed in matches the database type represented
     * by this instance of the DatabaseType class.
     *
     * @return true if the Connection matches this DatabaseType instance.
     * @throws SQLException
     */
    public abstract boolean matchesConnection(Connection con) throws SQLException;


    /**
     * Register this database type with DatabaseTypeFactory
     *
     * @see DatabaseTypeFactory#registerDatabaseType(DatabaseType)
     */
    protected void registerWithFactory() {
        DatabaseTypeFactory.registerDatabaseType(this);
    }

    /**
     * Compares two version numbers and returns true if the
     * first version number is greater than the second
     *
     * @param major1 First major version number for comparison
     * @param minor1 First minor version number for comparison
     * @param major2 Second major version number for comparison
     * @param minor2 Second minor version number for comparison
     * @return True if (major1, minor1) > (major2, minor2) otherwise false
     */
    protected static boolean versionGreaterThanOrEqual(int major1, int minor1, int major2, int minor2) {
        return ((major1 > major2) || ((major1 == major2) && (minor1 >= minor2)));
    }

    protected static boolean productNamesMatch(String productNamePrefix, String testName) {
        return ((testName != null) &&
                (testName.length() >= productNamePrefix.length()) &&
                (testName.substring(0, productNamePrefix.length()).equalsIgnoreCase(productNamePrefix)));
    }

    protected static boolean isProductNameInPrefixList(String[] productNamePrefixes, String productName) {
        if (productName != null) {
            for (final String productNamePrefix : productNamePrefixes) {
                if (productNamesMatch(productNamePrefix, productName)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean productNameMatches(Connection con) throws SQLException {
        String productName = con.getMetaData().getDatabaseProductName();
        return productName != null && isProductNameInPrefixList(productNamePrefix, productName.trim());
    }

    protected boolean versionGreaterThanOrEqual(Connection con, int majorVersion, int minorVersion) throws SQLException {
        try {
            DatabaseMetaData metaData = con.getMetaData();
            return versionGreaterThanOrEqual(metaData.getDatabaseMajorVersion(), metaData.getDatabaseMinorVersion(), majorVersion, minorVersion);
        } catch (AbstractMethodError ame) {
            // if this exception is thrown then it means that the wrong version of the
            // database driver was used
            return false;
        }
    }

    protected boolean versionLessThanOrEqual(Connection con, int majorVersion, int minorVersion) throws SQLException {
        try {
            DatabaseMetaData metaData = con.getMetaData();
            return versionGreaterThanOrEqual(majorVersion, minorVersion, metaData.getDatabaseMajorVersion(), metaData.getDatabaseMinorVersion());
        } catch (AbstractMethodError ame) {
            // if this exception is thrown then it means that the wrong version of the
            // database driver was used
            return false;
        }

    }

    /**
     * @return a format string to compose an SQL query to change a column in DB.
     */
    protected String getChangeColumnTypeStructure() {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public final String getChangeColumnTypeSQL(final String tableName, final String columnName, final String targetSqlType) {
        final String clauseStructure = getChangeColumnTypeStructure();
        return clauseStructure == null ? null : MessageFormat.format(clauseStructure, tableName, columnName, targetSqlType);
    }

    /**
     * @return a format string to compose an SQL query to drop index in DB.
     */
    public String getDropIndexStructure() {
        return DROP_INDEX_SCHEMA_DOT_TABLE_DOT_INDEX;
    }

    /**
     * {@inheritDoc}
     */
    public String getDropIndexSQL(final String schemaName, final String tableName, final String indexName) {
        return getDropIndexStructure()
                .replaceAll("\\{schemaName_with_dot}", appendDotIfNotEmpty(schemaName))
                .replaceAll("\\{tableName}", tableName)
                .replaceAll("\\{indexName}", indexName);
    }

    private static String appendDotIfNotEmpty(final String schemaName) {
        return schemaName != null && !schemaName.isEmpty() ? schemaName + '.' : "";
    }

    @Override
    public DatabaseType initialize(Connection con) {
        try {
            this.sqlKeywords = Optional.ofNullable(con.getMetaData().getSQLKeywords())
                    .orElse("");
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
        return this;
    }

    @Override
    public String getSqlKeywords() {
        return sqlKeywords;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractDatabaseType that = (AbstractDatabaseType) o;
        return name.equals(that.name) &&
                fieldTypeName.equals(that.fieldTypeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, fieldTypeName);
    }
}

