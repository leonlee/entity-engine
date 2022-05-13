package org.ofbiz.core.entity.transaction;

import com.atlassian.util.concurrent.CopyOnWriteMap;
import org.ofbiz.core.config.GenericConfigException;
import org.ofbiz.core.entity.ConnectionFactory;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.TransactionUtil;
import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.config.EntityConfigUtil;
import org.ofbiz.core.entity.config.JndiDatasourceInfo;
import org.ofbiz.core.entity.jdbc.interceptors.connection.ConnectionPoolInfoSynthesizer;
import org.ofbiz.core.entity.jdbc.interceptors.connection.ConnectionTracker;
import org.ofbiz.core.entity.util.ClassLoaderUtils;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.GeneralException;
import org.ofbiz.core.util.JNDIContextFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import static org.ofbiz.core.entity.metrics.MetricEmittingConnection.wrapWithMetrics;

/**
 * A TransactionFactory that automatically resolves the transaction factory from JNDI by making
 * some informed guesses.
 * <p>
 * Due to the nature of the original JNDITransactionFactory, a lot of this code is cut and paste from there,
 * and is essentially a copy.
 */
public class JNDIAutomaticFactory implements TransactionFactoryInterface {

    // Debug module name
    public static final String module = JNDIAutomaticFactory.class.getName();

    static TransactionManager transactionManager = null;
    static UserTransaction userTransaction = null;

    protected static Map<String, Object> dsCache = CopyOnWriteMap.newHashMap();
    protected static Map<String, ConnectionTracker> trackerCache = CopyOnWriteMap.newHashMap();

    protected static final Properties CONFIGURATION;

    static {
        CONFIGURATION = new Properties();

        try {
            CONFIGURATION.load(ClassLoaderUtils.getResourceAsStream("ofbiz-database.properties", JNDIFactory.class));
        } catch (Exception e) {
            Debug.logError("Unable to find ofbiz-database.properties file. Using default values for ofbiz configuration.");
        }
    }

    /**
     * Value found in configuration file in the jndi-name field if we are to
     * guess the the jndi-name of the (user-)transaction manager
     */
    protected static final String AUTO_CONFIGURE_TRANS_MGR;

    /**
     * A list of guesses for the jndi-name of the transaction manager. Used
     * if the user asks us to automagically configure this for them.
     */
    protected static final String[] TRANSMGR_NAME_GUESS;

    /**
     * A list of guesses for the jndi-name of the user transaction manager. Used
     * if the user asks us to automagically configure this for them.
     */
    protected static final String[] USR_TRANSMGR_NAME_GUESS;

    /**
     * Value found in configuration file as a prefix in the JNDI-NAME field if we are to
     * guess the prefix of the JNDI-NAME
     */
    protected static final String AUTO_CONFIGURE_JNDI_PREFIX;

    /**
     * A list of guesses that are to be used as prefixes for the JNDI-NAME field if the
     * user asked us to auto-magically configure it for them.
     */
    protected static final String[] JNDI_PREFIX_GUESSES;

    static {
        AUTO_CONFIGURE_TRANS_MGR = getNonNullProperty("transactionManager.autoConfigure", "${auto-jndi-name}");
        TRANSMGR_NAME_GUESS = parseList(getNonNullProperty("transactionManager.guess", "java:comp/UserTransaction,java:comp/env/UserTransaction"));
        USR_TRANSMGR_NAME_GUESS = parseList(getNonNullProperty("usrTransactionManager.guess", "java:comp/UserTransaction,java:comp/env/UserTransaction"));
        AUTO_CONFIGURE_JNDI_PREFIX = getNonNullProperty("jndiPrefix.autoConfigure", "${auto-server-prefix}");
        JNDI_PREFIX_GUESSES = parseList(getNonNullProperty("jndiPrefix.guess", "jdbc/,java:comp/env/jdbc/,"));
    }

    protected static JNDIConnectionDetails conDetails = new JNDIConnectionDetails();

    protected static final String LIST_DELIMITER = ",";

    /**
     * A method for getting properties from the configuration file. Uses the default value passed in
     * if the key as read from the property file was null.
     *
     * @param propertyKey
     * @param defaultValue
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

    static String[] parseList(String str) {
        List<String> list = new ArrayList<String>();

        int currentPosition = 0;

        for (int nextDelimiter = str.indexOf(LIST_DELIMITER, currentPosition);
             nextDelimiter >= currentPosition;
             nextDelimiter = str.indexOf(LIST_DELIMITER, currentPosition)) {

            list.add(str.substring(currentPosition, nextDelimiter));
            currentPosition = nextDelimiter + LIST_DELIMITER.length();
        }

        list.add(str.substring(currentPosition));

        return list.toArray(new String[list.size()]);
    }


    public TransactionManager getTransactionManager() {
        if (transactionManager == null) {
            synchronized (JNDIFactory.class) {
                // try again inside the synch just in case someone when through while we were waiting
                if (transactionManager == null) {
                    try {
                        String jndiName = EntityConfigUtil.getInstance().getTxFactoryTxMgrJndiName();
                        String jndiServerName = EntityConfigUtil.getInstance().getTxFactoryTxMgrJndiServerName();

                        if (jndiName != null && jndiName.length() > 0) {
                            // if (Debug.verboseOn()) Debug.logVerbose("[JNDIFactory.getTransactionManager] Trying JNDI name " + jndiName, module);

                            // Use the given JNDI name unless the user has asked for us to auto-configure this parameter
                            String[] guessList = {jndiName};

                            if (AUTO_CONFIGURE_TRANS_MGR.equals(jndiName)) {
                                guessList = TRANSMGR_NAME_GUESS;
                            }

                            transactionManager = (TransactionManager) retrieveJNDIObject(jndiServerName, guessList);
                        }
                    } catch (GeneralException e) {
                        Debug.logError(e);
                        transactionManager = null;
                    }
                }
            }
        }
        return transactionManager;
    }

    public UserTransaction getUserTransaction() {
        if (userTransaction == null) {
            synchronized (JNDIFactory.class) {
                // try again inside the synch just in case someone when through while we were waiting
                if (userTransaction == null) {
                    try {
                        String jndiName = EntityConfigUtil.getInstance().getTxFactoryUserTxJndiName();
                        String jndiServerName = EntityConfigUtil.getInstance().getTxFactoryUserTxJndiServerName();

                        if (jndiName != null && jndiName.length() > 0) {
                            // if (Debug.verboseOn()) Debug.logVerbose("[JNDIFactory.getTransactionManager] Trying JNDI name " + jndiName, module);

                            // Use the given JNDI name unless the user has asked for us to auto-configure this parameter
                            String[] guessList = {jndiName};

                            if (AUTO_CONFIGURE_TRANS_MGR.equals(jndiName)) {
                                guessList = USR_TRANSMGR_NAME_GUESS;
                            }

                            userTransaction = (UserTransaction) retrieveJNDIObject(jndiServerName, guessList);
                        }
                    } catch (GeneralException e) {
                        Debug.logError(e);
                        transactionManager = null;
                    }
                }
            }
        }
        return userTransaction;
    }

    private Object retrieveJNDIObject(String jndiServerName, String[] jndiNameGuesses) throws GeneralException {

        for (String jndiNameGuess : jndiNameGuesses) {
            try {
                InitialContext ic = JNDIContextFactory.getInitialContext(jndiServerName);

                if (ic != null) {
                    Object jndiObject = ic.lookup(jndiNameGuess);

                    if (jndiObject != null) {
                        Debug.logInfo("JNDI Object found using the look-up name " + jndiNameGuess, module);
                        return jndiObject;
                    }
                }
            } catch (NamingException ne) {
                Debug.logInfo("JNDI Object not found using the look-up name " + jndiNameGuess + " in JNDI.", module);
            }
        }

        Debug.logWarning("JNDI Object could not be found in " + arrayToString(jndiNameGuesses), module);
        return null;
    }

    private String arrayToString(String[] arr) {
        StringBuilder buff = new StringBuilder();

        buff.append("{");
        if (arr != null) {
            for (int i = 0; i < arr.length - 1; i++) {
                buff.append(arr[i] + ",");
            }
            buff.append(arr[arr.length - 1]);
        }
        buff.append("}");


        return buff.toString();
    }

    public String getTxMgrName() {
        return "jndi";
    }

    public Connection getConnection(String helperName) throws SQLException, GenericEntityException {
        DatasourceInfo datasourceInfo = EntityConfigUtil.getInstance().getDatasourceInfo(helperName);
        Connection con = null;

        if (datasourceInfo.getJndiDatasource() != null) {
            if (!conDetails.detailsSet()) {
                JndiDatasourceInfo jndiDatasource = datasourceInfo.getJndiDatasource();

                String jndiName = jndiDatasource.getJndiName();

                // Check whether prefix was set to the automatic value
                boolean guessJndiName = jndiName.startsWith(AUTO_CONFIGURE_JNDI_PREFIX);

                conDetails.setServerName(jndiDatasource.getJndiServerName());

                if (guessJndiName) {
                    // User has asked us to guess what the prefix of the connection name should be
                    // Loop through a set of guesses and see which one successfully creates a connection.

                    String jndiSuffix = jndiName.substring(AUTO_CONFIGURE_JNDI_PREFIX.length());

                    for (String jndiPrefixGuess : JNDI_PREFIX_GUESSES) {
                        con = getJndiConnection(helperName, jndiPrefixGuess + jndiSuffix, conDetails.getServerName());

                        if (con != null) {
                            // A connection name prefix has been guessed that works.
                            // Remember it to use it when making subsequent connection attempts
                            conDetails.setConnectionName(jndiPrefixGuess + jndiSuffix);
                            Debug.logInfo("Found connection using the JNDI name '" + jndiPrefixGuess + jndiSuffix + "'", module);
                            break;
                        } else {
                            Debug.logInfo("Failed to find connection using the JNDI name '" + jndiPrefixGuess + jndiSuffix + "'", module);
                        }
                    }
                } else {
                    // User hasn't asked us to guess the connection name prefix
                    // Just use the user supplied value
                    conDetails.setConnectionName(jndiName);
                }
            }

            if (con == null) {
                // Connection wasn't set above.
                // This means we already have the confirmed connection details
                con = getJndiConnection(helperName, conDetails.getConnectionName(), conDetails.getServerName());
            }

        } else {
            Debug.logError("JNDI loaded is the configured transaction manager but no jndi-jdbc element was specified in the " + helperName + " datasource. Please check your configuration; will try other sources");
        }

        if (con == null && datasourceInfo.getJdbcDatasource() != null) {
            con = ConnectionFactory.tryGenericConnectionSources(helperName, datasourceInfo.getJdbcDatasource());
        }

        return wrapWithMetrics(con);
    }

    private static Connection getJndiConnection(final String helperName, String jndiName, String jndiServerName) throws SQLException, GenericEntityException {
        // if (Debug.verboseOn()) Debug.logVerbose("Trying JNDI name " + jndiName, module);
        Object ds;

        ds = dsCache.get(jndiName);
        if (ds != null) {
            if (ds instanceof XADataSource) {
                XADataSource xads = (XADataSource) ds;

                return trackConnection(helperName, xads);
            } else {
                DataSource nds = (DataSource) ds;

                return trackConnection(helperName, nds);
            }
        }

        synchronized (ConnectionFactory.class) {
            // try again inside the synch just in case someone when through while we were waiting
            ds = dsCache.get(jndiName);
            if (ds != null) {
                if (ds instanceof XADataSource) {
                    XADataSource xads = (XADataSource) ds;
                    return trackConnection(helperName, xads);
                } else {
                    DataSource nds = (DataSource) ds;

                    return trackConnection(helperName, nds);
                }
            }

            try {
                if (Debug.infoOn()) Debug.logInfo("Doing JNDI lookup for name " + jndiName, module);
                InitialContext ic = JNDIContextFactory.getInitialContext(jndiServerName);

                if (ic != null) {
                    ds = ic.lookup(jndiName);
                } else {
                    Debug.logWarning("Initial Context returned was NULL for server name " + jndiServerName, module);
                }

                if (ds != null) {
                    if (Debug.verboseOn()) Debug.logVerbose("Got a Datasource object.", module);
                    dsCache.put(jndiName, ds);

                    if (ds instanceof XADataSource) {
                        if (Debug.infoOn()) Debug.logInfo("Got XADataSource for name " + jndiName, module);
                        XADataSource xads = (XADataSource) ds;

                        trackerCache.put(helperName, new ConnectionTracker());
                        return trackConnection(helperName, xads);
                    } else {
                        if (Debug.infoOn()) Debug.logInfo("Got DataSource for name " + jndiName, module);
                        DataSource nds = (DataSource) ds;

                        trackerCache.put(helperName, new ConnectionTracker(ConnectionPoolInfoSynthesizer.synthesizeConnectionPoolInfo(nds)));
                        return trackConnection(helperName, nds);
                    }
                } else {
                    Debug.logError("Datasource returned was NULL.", module);
                }
            } catch (NamingException ne) {
//                Debug.logInfo("[ConnectionFactory.getConnection] Failed to find DataSource named " + jndiName + " in JNDI server with name " + jndiServerName + ". Trying normal database.", module);
            } catch (GenericConfigException gce) {
                throw new GenericEntityException("Problems with the JNDI configuration.", gce.getNested());
            }
        }
        return null;
    }

    private static Connection trackConnection(final String helperName, final XADataSource xads) {
        ConnectionTracker connectionTracker = trackerCache.get(helperName);
        return connectionTracker.trackConnection(helperName, new Callable<Connection>() {
            public Connection call() throws Exception {
                return TransactionUtil.enlistConnection(xads.getXAConnection());
            }
        });
    }

    private static Connection trackConnection(final String helperName, final DataSource nds) {
        ConnectionTracker connectionTracker = trackerCache.get(helperName);
        return connectionTracker.trackConnection(helperName, new Callable<Connection>() {
            public Connection call() throws Exception {
                return nds.getConnection();
            }
        });
    }


    public void removeDatasource(final String helperName) {
        DatasourceInfo datasourceInfo = EntityConfigUtil.getInstance().getDatasourceInfo(helperName);
        if (datasourceInfo.getJndiDatasource() == null && datasourceInfo.getJdbcDatasource() != null) {
            // If a JDBC connection was configured, then there may be one here
            ConnectionFactory.removeDatasource(helperName);
        }
        trackerCache.remove(helperName);
    }
}
