package org.ofbiz.core.entity.transaction;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Properties;

public class AbstractConnectionFactory {

    private static final Logger log = Logger.getLogger(AbstractConnectionFactory.class);

    static void logType4DriverWarning() {
        log.warn("*********************************************** IMPORTANT  ***********************************************");
        log.warn("                                                                                                          ");
        log.warn("  We found that you may experience problems with database connectivity because your database driver       ");
        log.warn("  is not fully JDBC 4 compatible. As a workaround of this problem a validation query was added to your    ");
        log.warn("  runtime configuration. Please add a line with validation query:                                       \n");
        log.warn("                          <validation-query>select 1</validation-query>                                 \n");
        log.warn("  to your JIRA_HOME/dbconfig.xml or update your database driver to version which fully supports JDBC 4.   ");
        log.warn("  More information about this problem can be found here: https://jira.atlassian.com/browse/JRA-59768      ");
        log.warn("                                                                                                          ");
        log.warn("**********************************************************************************************************");
    }

    @VisibleForTesting
    static boolean checkIfProblemMayBeCausedByIsValidMethod(final String validationQuery,
                                                            final AbstractMethodError error) {
        if (validationQuery == null || validationQuery.isEmpty()) {
            final List<StackTraceElement> stackTraceElements = Lists.newArrayList(error.getStackTrace());
            return stackTraceElements.stream().anyMatch(
                    stackTraceElement -> stackTraceElement.getMethodName().contains("isValid"));
        }

        return false;
    }

    static Properties loadPropertiesFile(final String filename) {
        final Properties properties = new Properties();
        final InputStream inputStream = AbstractConnectionFactory.class.getResourceAsStream("/" + filename);
        if (inputStream != null) {
            try {
                properties.load(inputStream);
            } catch (IOException e) {
                log.error("Error loading " + properties, e);
            }
        }

        return properties;
    }

    static void unregisterDatasourceFromJmx(final String jmxBeanName) {
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            mbs.unregisterMBean(ObjectName.getInstance(jmxBeanName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void unregisterMBean(String name) {
        try {
            final ObjectName objectName = ObjectName.getInstance(name);
            final MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
            if (platformMBeanServer.isRegistered(objectName)) {
                platformMBeanServer.unregisterMBean(objectName);
            }
        } catch (Exception e) {
            log.error("Exception un-registering MBean data source " + name, e);
        }
    }
}
