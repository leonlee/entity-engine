package org.ofbiz.core.entity.config;

import org.ofbiz.core.entity.ConnectionFactory;
import org.ofbiz.core.entity.GenericHelperDAO;
import org.ofbiz.core.entity.external.ExternalDataHandler;
import org.ofbiz.core.entity.external.ExternalDataHandler.ExternalData;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseType;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseTypeFactory;
import org.ofbiz.core.entity.util.ClassLoaderUtils;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.GeneralRuntimeException;
import org.ofbiz.core.util.UtilXml;
import org.w3c.dom.Element;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;

import static org.ofbiz.core.util.UtilValidate.isEmpty;

/**
 * Info about a datasource
 */
public class DatasourceInfo {
    static final int DEFAULT_POOL_MAX_SIZE = 50;

    private final String name;
    private final String helperClass;
    private String fieldTypeName;
    private DatabaseType databaseType;

    private JndiDatasourceInfo jndiDatasource;
    private Element tyrexDataSourceElement;
    private JdbcDatasourceInfo jdbcDatasource;

    // These are set to JIRA defaults
    private String schemaName = null;
    private boolean checkOnStart = true;
    private boolean addMissingOnStart = true;
    private boolean useFks = false;
    private boolean useFkIndices = false;
    private boolean checkForeignKeysOnStart = false;
    private boolean checkFkIndicesOnStart = false;
    private boolean usePkConstraintNames = true;
    private String constraintNameClipLengthStr;
    private Integer constraintNameClipLength = null;
    private String fkStyle = null;
    private boolean useFkInitiallyDeferred = true;
    private boolean useIndices = true;
    private boolean useFunctionBasedIndices = true;
    private String joinStyle = "ansi";

    protected static final Properties CONFIGURATION;


    static {
        CONFIGURATION = new Properties();
        try {
            CONFIGURATION.load(ClassLoaderUtils.getResourceAsStream("ofbiz-database.properties", EntityConfigUtil.class));
        } catch (Exception e) {
            Debug.logError("Unable to find ofbiz-database.properties file. Using default values for ofbiz configuration.");
        }
    }

    /**
     * If the field-type-name property matches this string we will try and guess the field-type-name by using the
     * metadata returned by the database connection.
     */
    public static final String AUTO_FIELD_TYPE;

    public static final String AUTO_SCHEMA_NAME;

    public static final String AUTO_CONSTRAINT_NAME_CLIP_LENGTH;

    static {
        AUTO_FIELD_TYPE = getNonNullProperty("fieldType.autoConfigue", "${auto-field-type-name}");

        AUTO_SCHEMA_NAME = getNonNullProperty("schemaName.autoConfigure", "${auto-schema-name}");

        AUTO_CONSTRAINT_NAME_CLIP_LENGTH = getNonNullProperty("constraintNameClipLength.autoConfigure", "${auto-constraint-name-clip-length}");
    }

    public static final int DEFAULT_CONSTRAINT_NAME_CLIP_LENGTH = 20;

    /**
     * A method for getting properties from the configuration file. Uses the default value passed in if the key as
     * read from the property file was null.
     */
    private static String getNonNullProperty(String propertyKey, String defaultValue) {
        String fieldValue = CONFIGURATION.getProperty(propertyKey);
        if (fieldValue != null) {
            return fieldValue;
        } else {
            Debug.logError(propertyKey + " not set in the ofbiz-database.properties file. Using default value: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Constructor for JIRAs use.  The defaults are set such that these properties are the only ones that need
     * setting.
     *
     * @param name           The name of the datasource
     * @param fieldTypeName  The type of the datasource
     * @param schemaName     The schema to use, may be null
     * @param jdbcDatasource The JDBC datasource
     */
    public DatasourceInfo(String name, String fieldTypeName, String schemaName, JdbcDatasourceInfo jdbcDatasource) {
        // Default constructor, for dynamic instantiation
        this.name = name;
        this.fieldTypeName = fieldTypeName;
        this.helperClass = GenericHelperDAO.class.getName();
        this.jdbcDatasource = jdbcDatasource;
        this.schemaName = schemaName;
    }

    /**
     * Constructor for JIRAs use.  The defaults are set such that these properties are the only ones that need
     * setting.
     *
     * @param name           The name of the datasource
     * @param fieldTypeName  The type of the datasource
     * @param schemaName     The schema to use, may be null
     * @param jndiDatasource The JNDI datasource
     */
    public DatasourceInfo(String name, String fieldTypeName, String schemaName, JndiDatasourceInfo jndiDatasource) {
        // Default constructor, for dynamic instantiation
        this.name = name;
        this.fieldTypeName = fieldTypeName;
        this.helperClass = GenericHelperDAO.class.getName();
        this.jndiDatasource = jndiDatasource;
        this.schemaName = schemaName;
    }

    public DatasourceInfo(Element element) {
        this.name = element.getAttribute("name");
        this.helperClass = element.getAttribute("helper-class");
        this.fieldTypeName = element.getAttribute("field-type-name");

        schemaName = element.getAttribute("schema-name");
        // anything but false is true
        checkOnStart = !"false".equals(element.getAttribute("check-on-start"));
        // anything but true is false
        addMissingOnStart = "true".equals(element.getAttribute("add-missing-on-start"));
        // anything but false is true
        useFks = !"false".equals(element.getAttribute("use-foreign-keys"));
        // anything but false is true
        useFkIndices = !"false".equals(element.getAttribute("use-foreign-key-indices"));
        // anything but true is false
        checkForeignKeysOnStart = "true".equals(element.getAttribute("check-fks-on-start"));
        // anything but true is false
        checkFkIndicesOnStart = "true".equals(element.getAttribute("check-fk-indices-on-start"));
        // anything but false is true
        usePkConstraintNames = !"false".equals(element.getAttribute("use-pk-constraint-names"));
        // parsing of this string is delayed so auto detection can be done once the database is configured
        constraintNameClipLengthStr = element.getAttribute("constraint-name-clip-length");
        fkStyle = element.getAttribute("fk-style");
        // anything but true is false
        useFkInitiallyDeferred = "true".equals(element.getAttribute("use-fk-initially-deferred"));
        // anything but false is true
        useIndices = !"false".equals(element.getAttribute("use-indices"));
        useFunctionBasedIndices = !"false".equals(element.getAttribute("use-function-based-indices"));
        joinStyle = element.getAttribute("join-style");
        if (fkStyle == null || fkStyle.length() == 0) {
            fkStyle = "name_constraint";
        }
        if (joinStyle == null || joinStyle.length() == 0) {
            joinStyle = "ansi";
        }

        Element jndiDatasourceElement = UtilXml.firstChildElement(element, "jndi-jdbc");
        if (jndiDatasourceElement != null) {
            String jndiName = jndiDatasourceElement.getAttribute("jndi-name");
            String jndiServerName = jndiDatasourceElement.getAttribute("jndi-server-name");
            jndiDatasource = new JndiDatasourceInfo(jndiName, jndiServerName);
        }
        Element jdbcDatasourceElement = UtilXml.firstChildElement(element, "inline-jdbc");
        if (jdbcDatasourceElement != null) {
            String uri = jdbcDatasourceElement.getAttribute("jdbc-uri");
            String driverClassName = jdbcDatasourceElement.getAttribute("jdbc-driver");
            String username = jdbcDatasourceElement.getAttribute("jdbc-username");
            String password = jdbcDatasourceElement.getAttribute("jdbc-password");

            ExternalDataHandler externalDataHandler = new ExternalDataHandler();
            Optional<String> externalUsername = externalDataHandler.getExternalData(jdbcDatasourceElement, ExternalData.USERNAME);
            Optional<String> externalPassword = externalDataHandler.getExternalData(jdbcDatasourceElement, ExternalData.PASSWORD);

            password = externalPassword.orElse(password);
            username = externalUsername.orElse(username);

            String transIso = jdbcDatasourceElement.getAttribute("isolation-level");

            // These defaults are copied out of entity-config.dtd
            int maxSize = getIntValueFromElement(jdbcDatasourceElement, "pool-maxsize", DEFAULT_POOL_MAX_SIZE);
            int maxIdle = getIntValueFromElement(jdbcDatasourceElement, "pool-maxidle", maxSize);
            int minSize = getIntValueFromElement(jdbcDatasourceElement, "pool-minsize", ConnectionPoolInfo.DEFAULT_POOL_MIN_SIZE);
            long maxWait = getLongValueFromElement(jdbcDatasourceElement, "pool-maxwait", ConnectionPoolInfo.DEFAULT_POOL_MAX_WAIT);
            long sleepTime = getLongValueFromElement(jdbcDatasourceElement, "pool-sleeptime", ConnectionPoolInfo.DEFAULT_POOL_SLEEP_TIME);
            long lifeTime = getLongValueFromElement(jdbcDatasourceElement, "pool-lifetime", ConnectionPoolInfo.DEFAULT_POOL_LIFE_TIME);
            long deadLockMaxWait = getLongValueFromElement(jdbcDatasourceElement, "pool-deadlock-maxwait", ConnectionPoolInfo.DEFAULT_DEADLOCK_MAX_WAIT);
            long deadLockRetryWait = getLongValueFromElement(jdbcDatasourceElement, "pool-deadlock-retrywait", ConnectionPoolInfo.DEFAULT_DEADLOCK_RETRY_WAIT);
            String validationQuery = jdbcDatasourceElement.getAttribute("pool-validationQuery");
            Long minEvictableTimeMillis = getLongValueFromElement(jdbcDatasourceElement, "pool-minEvictableIdleTimeMillis", null);
            Long timeBetweenEvictionRunsMillis = getLongValueFromElement(jdbcDatasourceElement, "pool-timeBetweenEvictionRunsMillis", null);
            Properties connectionProperties = parsePropertyString(jdbcDatasourceElement.getAttribute("jdbc-connectionProperties"));
            ConnectionPoolInfo connectionPoolInfo = ConnectionPoolInfo.builder()
                    .setPoolMaxSize(maxSize)
                    .setPoolMaxIdle(maxIdle)
                    .setPoolMinSize(minSize)
                    .setPoolMaxWait(maxWait)
                    .setPoolSleepTime(sleepTime)
                    .setPoolLifeTime(lifeTime)
                    .setDeadLockMaxWait(deadLockMaxWait)
                    .setDeadLockRetryWait(deadLockRetryWait)
                    .setValidationQuery(validationQuery)
                    .setMinEvictableTimeMillis(minEvictableTimeMillis)
                    .setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis)
                    .build();
            jdbcDatasource = new JdbcDatasourceInfo(uri, driverClassName, username, password, transIso, connectionProperties, connectionPoolInfo);
        }
        tyrexDataSourceElement = UtilXml.firstChildElement(element, "tyrex-dataSource");
    }

    private String trimAndIgnoreBlank(String s) {
        if (s != null) {
            s = s.trim();
            if (s.length() > 0) {
                return s;
            }
        }
        return null;
    }

    private int getIntValueFromElement(Element element, String attributeName, int defaultValue) {
        String value = trimAndIgnoreBlank(element.getAttribute(attributeName));
        if (value == null) {
            Debug.logInfo(attributeName + " not set, defaulting to " + defaultValue);
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            Debug.logError(attributeName + " was not a number, but was \"" + value + "\", defaulting to " + defaultValue);
            return defaultValue;
        }
    }

    private Long getLongValueFromElement(Element element, String attributeName, Long defaultValue) {
        String value = trimAndIgnoreBlank(element.getAttribute(attributeName));
        if (value == null) {
            Debug.logInfo(attributeName + " not set, defaulting to " + defaultValue);
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException nfe) {
            Debug.logError(attributeName + " was not a number, but was \"" + value + "\", defaulting to " + defaultValue);
            return defaultValue;
        }
    }

    private Properties parsePropertyString(String propertiesString) {
        if (isEmpty(propertiesString)) {
            return null;
        }

        Properties properties = new Properties();
        for (String keyValue : propertiesString.split(";")) {
            String[] values = keyValue.split("=");
            if (values.length != 2) {
                Debug.logError("Bad property format (must be name=value): " + keyValue, DatasourceInfo.class.getName());
            }

            properties.setProperty(values[0], values[1]);
        }

        return properties;
    }

    public String getFieldTypeName() {
        // Check whether the field has already been initialized
        if (AUTO_FIELD_TYPE.equals(fieldTypeName)) {
            fieldTypeName = findFieldTypeFromJDBCConnection();
        }
        return fieldTypeName;
    }

    public String getSchemaName() {
        if (AUTO_SCHEMA_NAME.equals(schemaName)) {
            schemaName = findSchemaNameFromJDBCConnection();
        }
        return schemaName;
    }

    public int getConstraintNameClipLength() {
        if (constraintNameClipLength == null) {
            if (AUTO_CONSTRAINT_NAME_CLIP_LENGTH.equals(constraintNameClipLengthStr)) {
                constraintNameClipLength = findConstraintNameClipLengthFromJDBCConnection();
            } else {
                try {
                    constraintNameClipLength = 30;
                    if ((constraintNameClipLengthStr != null) && (!constraintNameClipLengthStr.equals(""))) {
                        constraintNameClipLength = new Integer(constraintNameClipLengthStr);
                    }
                } catch (Exception e) {
                    Debug.logError("Could not parse constraint-name-clip-length value for datasource with name " + this.name + ", using default value of 30");
                }
            }
        }

        return constraintNameClipLength;
    }

    private String findFieldTypeFromJDBCConnection() {
        try {
            return getDatabaseTypeFromJDBCConnection().getFieldTypeName();
        } catch (Exception e) {
            String error = "Could not get connection to database to determine database type for " + AUTO_FIELD_TYPE;
            Debug.logError(e, error);
            throw new GeneralRuntimeException(error, e);
        }
    }

    private String findSchemaNameFromJDBCConnection() {
        try {
            final Connection connection = ConnectionFactory.getConnection(name);
            try {
                return getDatabaseTypeFromJDBCConnection().getSchemaName(connection);
            } finally {
                silentlyClose(connection);
            }
        } catch (Exception e) {
            String error = "Could not get connection to database to determine database schema-name for " + AUTO_SCHEMA_NAME;
            Debug.logError(e, error);
            throw new GeneralRuntimeException(error, e);
        }
    }

    private int findConstraintNameClipLengthFromJDBCConnection() {
        try {
            return getDatabaseTypeFromJDBCConnection().getConstraintNameClipLength();
        } catch (Exception e) {
            String error = "Could not get connection to database to determine database clip length";
            Debug.logError(e, error);
            return DEFAULT_CONSTRAINT_NAME_CLIP_LENGTH;
        }
    }

    /**
     * Provides a database type.
     *
     * @return the database type.
     */
    public DatabaseType getDatabaseTypeFromJDBCConnection() {
        if (databaseType == null) {
            Connection connection = null;
            try {
                connection = ConnectionFactory.getConnection(name);
                databaseType = getDatabaseTypeFromJDBCConnection(connection);
            } catch (Exception e) {
                String error = "Could not determine database type.";
                Debug.logError(e, error);
                throw new GeneralRuntimeException(error, e);
            } finally {
                silentlyClose(connection);
            }
        }
        return databaseType;
    }

    /**
     * Provides a database type (extracted to enable mocking)
     *
     * @return the database type.
     */
    public DatabaseType getDatabaseTypeFromJDBCConnection(Connection connection) {
        return DatabaseTypeFactory.getTypeForConnection(connection);
    }


    public JndiDatasourceInfo getJndiDatasource() {
        return jndiDatasource;
    }

    public String getName() {
        return name;
    }

    public String getHelperClass() {
        return helperClass;
    }

    public Element getTyrexDataSourceElement() {
        return tyrexDataSourceElement;
    }

    public JdbcDatasourceInfo getJdbcDatasource() {
        return jdbcDatasource;
    }

    public boolean isCheckOnStart() {
        return checkOnStart;
    }

    public boolean isAddMissingOnStart() {
        return addMissingOnStart;
    }

    public boolean isUseFks() {
        return useFks;
    }

    public boolean isUseFkIndices() {
        return useFkIndices;
    }

    public boolean isCheckForeignKeysOnStart() {
        return checkForeignKeysOnStart;
    }

    public boolean isCheckFkIndicesOnStart() {
        return checkFkIndicesOnStart;
    }

    public boolean isUsePkConstraintNames() {
        return usePkConstraintNames;
    }

    public boolean isUseFunctionBasedIndices() {
        return useFunctionBasedIndices;
    }

    public String getFkStyle() {
        return fkStyle;
    }

    public boolean isUseFkInitiallyDeferred() {
        return useFkInitiallyDeferred;
    }

    public boolean isUseIndices() {
        return useIndices;
    }

    public String getJoinStyle() {
        return joinStyle;
    }

    private void silentlyClose(final Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                //ignore
            }
        }
    }
}
