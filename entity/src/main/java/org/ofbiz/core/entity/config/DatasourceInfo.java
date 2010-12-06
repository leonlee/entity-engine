package org.ofbiz.core.entity.config;

import org.ofbiz.core.entity.ConnectionFactory;
import org.ofbiz.core.entity.GenericHelperDAO;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseType;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseTypeFactory;
import org.ofbiz.core.entity.util.ClassLoaderUtils;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.GeneralRuntimeException;
import org.ofbiz.core.util.UtilXml;
import org.w3c.dom.Element;

import java.sql.Connection;
import java.util.Properties;

/**
 * Info about a datasource
*/
public class DatasourceInfo
{
    private final String name;
    private final String helperClass;
    private String fieldTypeName;

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
    private String joinStyle = null;

    protected static final Properties CONFIGURATION;


    static
    {
        CONFIGURATION = new Properties();
        try
        {
            CONFIGURATION.load(ClassLoaderUtils.getResourceAsStream("ofbiz-database.properties", EntityConfigUtil.class));
        }
        catch (Exception e)
        {
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

    static
    {
        AUTO_FIELD_TYPE = getNonNullProperty("fieldType.autoConfigue", "${auto-field-type-name}");

        AUTO_SCHEMA_NAME = getNonNullProperty("schemaName.autoConfigure", "${auto-schema-name}");

        AUTO_CONSTRAINT_NAME_CLIP_LENGTH = getNonNullProperty("constraintNameClipLength.autoConfigure", "${auto-constraint-name-clip-length}");
    }

    public static final int DEFAULT_CONSTRAINT_NAME_CLIP_LENGTH = 20;

    /**
     * A method for getting properties from the configuration file. Uses the default value passed in if the key as
     * read from the property file was null.
     */
    private static String getNonNullProperty(String propertyKey, String defaultValue)
    {
        String fieldValue = CONFIGURATION.getProperty(propertyKey);
        if (fieldValue != null)
        {
            return fieldValue;
        }
        else
        {
            Debug.logError(propertyKey + " not set in the ofbiz-database.properties file. Using default value: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Constructor for JIRAs use.  The defaults are set such that these properties are the only ones that need
     * setting.
     *
     * @param name The name of the datasource
     * @param fieldTypeName The type of the datasource
     * @param schemaName The schema to use, may be null
     * @param jdbcDatasource The JDBC datasource
     */
    public DatasourceInfo(String name, String fieldTypeName, String schemaName, JdbcDatasourceInfo jdbcDatasource)
    {
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
     * @param name The name of the datasource
     * @param fieldTypeName The type of the datasource
     * @param schemaName The schema to use, may be null
     * @param jndiDatasource The JNDI datasource
     */
    public DatasourceInfo(String name, String fieldTypeName, String schemaName, JndiDatasourceInfo jndiDatasource)
    {
        // Default constructor, for dynamic instantiation
        this.name = name;
        this.fieldTypeName = fieldTypeName;
        this.helperClass = GenericHelperDAO.class.getName();
        this.jndiDatasource = jndiDatasource;
        this.schemaName = schemaName;
    }

    public DatasourceInfo(Element element)
    {
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
        joinStyle = element.getAttribute("join-style");
        if (fkStyle == null || fkStyle.length() == 0)
        {
            fkStyle = "name_constraint";
        }
        if (joinStyle == null || joinStyle.length() == 0)
        {
            joinStyle = "ansi";
        }

        Element jndiDatasourceElement = UtilXml.firstChildElement(element, "jndi-jdbc");
        if (jndiDatasourceElement != null)
        {
            String jndiName = jndiDatasourceElement.getAttribute("jndi-name");
            String jndiServerName = jndiDatasourceElement.getAttribute("jndi-server-name");
            jndiDatasource = new JndiDatasourceInfo(jndiName, jndiServerName);
        }
        Element jdbcDatasourceElement = UtilXml.firstChildElement(element, "inline-jdbc");
        if (jdbcDatasourceElement != null)
        {
            String uri = jdbcDatasourceElement.getAttribute("jdbc-uri");
            String driverClassName = jdbcDatasourceElement.getAttribute("jdbc-driver");
            String username = jdbcDatasourceElement.getAttribute("jdbc-username");
            String password = jdbcDatasourceElement.getAttribute("jdbc-password");
            String transIso = jdbcDatasourceElement.getAttribute("isolation-level");

            // These defaults are copied out of entity-config.dtd
            int maxSize = getIntValueFromElement(jdbcDatasourceElement, "pool-maxsize", 50);
            int minSize = getIntValueFromElement(jdbcDatasourceElement, "pool-minsize", 2);
            long sleepTime = getLongValueFromElement(jdbcDatasourceElement, "pool-sleeptime", 300000);
            long lifeTime = getLongValueFromElement(jdbcDatasourceElement, "pool-lifetime", 600000);
            long deadLockMaxWait = getLongValueFromElement(jdbcDatasourceElement, "pool-deadlock-maxwait", 300000);
            long deadLockRetryWait = getLongValueFromElement(jdbcDatasourceElement, "pool-deadlock-retrywait", 10000);
            ConnectionPoolInfo connectionPoolInfo = new ConnectionPoolInfo(maxSize, minSize, sleepTime, lifeTime,
                    deadLockMaxWait, deadLockRetryWait);

            jdbcDatasource = new JdbcDatasourceInfo(uri, driverClassName, username, password, transIso, connectionPoolInfo);
        }
        tyrexDataSourceElement = UtilXml.firstChildElement(element, "tyrex-dataSource");
    }

    private int getIntValueFromElement(Element element, String attributeName, int defaultValue)
    {
        String value = element.getAttribute(attributeName);
        if (value == null)
        {
            Debug.logInfo(attributeName + " not set, defaulting to " + defaultValue);
            return defaultValue;
        }
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException nfe)
        {
            Debug.logError(attributeName + " was not a number, but was \"" + value + "\", defaulting to " + defaultValue);
            return defaultValue;
        }
    }

    private long getLongValueFromElement(Element element, String attributeName, long defaultValue)
    {
        String value = element.getAttribute(attributeName);
        if (value == null)
        {
            Debug.logInfo(attributeName + " not set, defaulting to " + defaultValue);
            return defaultValue;
        }
        try
        {
            return Long.parseLong(value);
        }
        catch (NumberFormatException nfe)
        {
            Debug.logError(attributeName + " was not a number, but was \"" + value + "\", defaulting to " + defaultValue);
            return defaultValue;
        }
    }

    public String getFieldTypeName()
    {
        // Check whether the field has already been initialized
        if (AUTO_FIELD_TYPE.equals(fieldTypeName))
        {
            fieldTypeName = findFieldTypeFromJDBCConnection();
        }
        return fieldTypeName;
    }

    public String getSchemaName()
    {
        if (AUTO_SCHEMA_NAME.equals(schemaName))
        {
            schemaName = findSchemaNameFromJDBCConnection();
        }
        return schemaName;
    }

    public int getConstraintNameClipLength()
    {
        if(constraintNameClipLength == null) {
            if(AUTO_CONSTRAINT_NAME_CLIP_LENGTH.equals(constraintNameClipLengthStr)) {
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

    private String findFieldTypeFromJDBCConnection()
    {
        final Connection connection;
        try
        {
            connection = ConnectionFactory.getConnection(name);
            final DatabaseType typeForConnection = DatabaseTypeFactory.getTypeForConnection(connection);
            if (typeForConnection == null)
            {
                Debug.logError("Could not determine database type from ");
            }
            return typeForConnection.getFieldTypeName();
        }
        catch (Exception e)
        {
            String error = "Could not get connection to database to determine database type for " + AUTO_FIELD_TYPE;
            Debug.logError(e, error);
            throw new GeneralRuntimeException(error, e);
        }
    }

    private String findSchemaNameFromJDBCConnection()
    {
        final Connection connection;
        try
        {
            connection = ConnectionFactory.getConnection(name);
            final DatabaseType typeForConnection = DatabaseTypeFactory.getTypeForConnection(connection);
            if (typeForConnection == null)
            {
                Debug.logError("Could not determine database type from ");
            }
            return typeForConnection.getSchemaName(connection);
        }
        catch (Exception e)
        {
            String error = "Could not get connection to database to determine database schema-name for " + AUTO_SCHEMA_NAME;
            Debug.logError(e, error);
            throw new GeneralRuntimeException(error, e);
        }
    }

    private int findConstraintNameClipLengthFromJDBCConnection()
    {
        final Connection connection;
        try
        {
            connection = ConnectionFactory.getConnection(name);
            final DatabaseType typeForConnection = DatabaseTypeFactory.getTypeForConnection(connection);
            if (typeForConnection == null)
            {
                Debug.logError("Could not determine database type from ");
            }
            return typeForConnection.getConstraintNameClipLength();
        }
        catch (Exception e)
        {
            String error = "Could not get connection to database to determine database clip length";
            Debug.logError(e, error);
            return DEFAULT_CONSTRAINT_NAME_CLIP_LENGTH;
        }
    }

    public JndiDatasourceInfo getJndiDatasource()
    {
        return jndiDatasource;
    }

    public String getName()
    {
        return name;
    }

    public String getHelperClass()
    {
        return helperClass;
    }

    public Element getTyrexDataSourceElement()
    {
        return tyrexDataSourceElement;
    }

    public JdbcDatasourceInfo getJdbcDatasource()
    {
        return jdbcDatasource;
    }

    public boolean isCheckOnStart()
    {
        return checkOnStart;
    }

    public boolean isAddMissingOnStart()
    {
        return addMissingOnStart;
    }

    public boolean isUseFks()
    {
        return useFks;
    }

    public boolean isUseFkIndices()
    {
        return useFkIndices;
    }

    public boolean isCheckForeignKeysOnStart()
    {
        return checkForeignKeysOnStart;
    }

    public boolean isCheckFkIndicesOnStart()
    {
        return checkFkIndicesOnStart;
    }

    public boolean isUsePkConstraintNames()
    {
        return usePkConstraintNames;
    }

    public String getFkStyle()
    {
        return fkStyle;
    }

    public boolean isUseFkInitiallyDeferred()
    {
        return useFkInitiallyDeferred;
    }

    public boolean isUseIndices()
    {
        return useIndices;
    }

    public String getJoinStyle()
    {
        return joinStyle;
    }
}
