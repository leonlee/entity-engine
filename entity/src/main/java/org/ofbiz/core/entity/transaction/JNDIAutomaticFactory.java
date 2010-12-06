package org.ofbiz.core.entity.transaction;

import com.atlassian.util.concurrent.CopyOnWriteMap;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.ConnectionFactory;
import org.ofbiz.core.entity.TransactionUtil;
import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.config.JndiDatasourceInfo;
import org.ofbiz.core.entity.util.ClassLoaderUtils;
import org.ofbiz.core.entity.config.EntityConfigUtil;
import org.ofbiz.core.util.GeneralException;
import org.ofbiz.core.util.Debug;
import org.ofbiz.core.util.JNDIContextFactory;
import org.ofbiz.core.config.GenericConfigException;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.XADataSource;
import javax.sql.DataSource;
import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

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
     * @param propertyKey
     * @param defaultValue
     * @return
     */
    private static String getNonNullProperty(String propertyKey, String defaultValue) {
        String fieldValue = CONFIGURATION.getProperty(propertyKey);
        if(fieldValue != null) {
            return fieldValue;
        } else {
            Debug.logError(propertyKey + " not set in the ofbiz-database.properties file. Using default value: " + defaultValue);
            return defaultValue;
        }
    }

    static String[] parseList(String str) {
        List list = new ArrayList();

        int currentPosition = 0;

        for (int nextDelimiter = str.indexOf(LIST_DELIMITER, currentPosition);
             nextDelimiter >= currentPosition;
             nextDelimiter = str.indexOf(LIST_DELIMITER, currentPosition)) {

            list.add(str.substring(currentPosition, nextDelimiter));
            currentPosition = nextDelimiter + LIST_DELIMITER.length();
        }

        list.add(str.substring(currentPosition));

       return (String[]) list.toArray(new String[] {});
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
                            String [] guessList = {jndiName};

                            if(AUTO_CONFIGURE_TRANS_MGR.equals(jndiName)){
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
                            String [] guessList = {jndiName};

                            if(AUTO_CONFIGURE_TRANS_MGR.equals(jndiName)){
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

    private Object retrieveJNDIObject (String jndiServerName, String[] jndiNameGuesses) throws GeneralException {

        for(int i = 0; i < jndiNameGuesses.length; i++ ) {
            String jndiNameGuess = jndiNameGuesses[i];
            try {
                InitialContext ic = JNDIContextFactory.getInitialContext(jndiServerName);

                if (ic != null) {
                    Object jndiObject = ic.lookup(jndiNameGuess);

                    if(jndiObject != null) {
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
        StringBuffer buff = new StringBuffer();

        buff.append("{");
        if(arr != null) {
            for (int i = 0; i < arr.length - 1; i ++) {
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

                    for (int i = 0; i < JNDI_PREFIX_GUESSES.length; i++) {
                        String jndiPrefixGuess = JNDI_PREFIX_GUESSES[i];
                        con = getJndiConnection(jndiPrefixGuess + jndiSuffix, conDetails.getServerName());

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
                con = getJndiConnection(conDetails.getConnectionName(), conDetails.getServerName());
            }

            if (con != null) return con;
        } else {
            Debug.logError("JNDI loaded is the configured transaction manager but no jndi-jdbc element was specified in the " + helperName + " datasource. Please check your configuration; will try other sources");
        }

        if (datasourceInfo.getJdbcDatasource() != null) {
            return ConnectionFactory.tryGenericConnectionSources(helperName, datasourceInfo.getJdbcDatasource());
        } else {
            //no real need to print an error here
            return null;
        }
    }

    public static Connection getJndiConnection(String jndiName, String jndiServerName) throws SQLException, GenericEntityException {
        // if (Debug.verboseOn()) Debug.logVerbose("Trying JNDI name " + jndiName, module);
        Object ds;

        ds = dsCache.get(jndiName);
        if (ds != null) {
            if (ds instanceof XADataSource) {
                XADataSource xads = (XADataSource) ds;

                return TransactionUtil.enlistConnection(xads.getXAConnection());
            } else {
                DataSource nds = (DataSource) ds;

                return nds.getConnection();
            }
        }

        synchronized (ConnectionFactory.class) {
            // try again inside the synch just in case someone when through while we were waiting
            ds = dsCache.get(jndiName);
            if (ds != null) {
                if (ds instanceof XADataSource) {
                    XADataSource xads = (XADataSource) ds;

                    return TransactionUtil.enlistConnection(xads.getXAConnection());
                } else {
                    DataSource nds = (DataSource) ds;

                    return nds.getConnection();
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
                    Connection con;

                    if (ds instanceof XADataSource) {
                        if (Debug.infoOn()) Debug.logInfo("Got XADataSource for name " + jndiName, module);
                        XADataSource xads = (XADataSource) ds;
                        XAConnection xac = xads.getXAConnection();

                        con = TransactionUtil.enlistConnection(xac);
                    } else {
                        if (Debug.infoOn()) Debug.logInfo("Got DataSource for name " + jndiName, module);
                        DataSource nds = (DataSource) ds;

                        con = nds.getConnection();
                    }

                    /* NOTE: This code causes problems because settting the transaction isolation level after a transaction has started is a no-no
                     * The question is: how should we do this?
                     String isolationLevel = jndiJdbcElement.getAttribute("isolation-level");
                     if (con != null && isolationLevel != null && isolationLevel.length() > 0) {
                     if ("Serializable".equals(isolationLevel)) {
                     con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                     } else if ("RepeatableRead".equals(isolationLevel)) {
                     con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                     } else if ("ReadUncommitted".equals(isolationLevel)) {
                     con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                     } else if ("ReadCommitted".equals(isolationLevel)) {
                     con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                     } else if ("None".equals(isolationLevel)) {
                     con.setTransactionIsolation(Connection.TRANSACTION_NONE);
                     }
                     }
                     */

                    // if (con != null) if (Debug.infoOn()) Debug.logInfo("[ConnectionFactory.getConnection] Got JNDI connection with catalog: " + con.getCatalog());
                    return con;
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
}
