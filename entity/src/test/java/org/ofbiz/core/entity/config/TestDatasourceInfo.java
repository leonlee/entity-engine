package org.ofbiz.core.entity.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 */
public class TestDatasourceInfo {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testFullJdbcConfig() throws Exception {
        DatasourceInfo datasourceInfo = createDatasourceInfo("/TestDatasourceInfo-fullJdbcConfig.xml");
        assertEquals("default", datasourceInfo.getName());
        assertEquals("postgres72", datasourceInfo.getFieldTypeName());
        assertNotNull(datasourceInfo.getJdbcDatasource());
        JdbcDatasourceInfo jdbcInfo = datasourceInfo.getJdbcDatasource();
        assertEquals("jdbc:postgresql://localhost:5432/jira", jdbcInfo.getUri());
        assertEquals("org.postgresql.Driver", jdbcInfo.getDriverClassName());
        assertEquals("jira", jdbcInfo.getUsername());
        assertEquals("password", jdbcInfo.getPassword());

        Properties propertiesInXML = new Properties();
        propertiesInXML.setProperty("portNumber", "5432");
        propertiesInXML.setProperty("defaultAutoCommit", "true");
        assertEquals(propertiesInXML, jdbcInfo.getConnectionProperties());

        assertNotNull(jdbcInfo.getConnectionPoolInfo());
        ConnectionPoolInfo poolInfo = jdbcInfo.getConnectionPoolInfo();
        assertEquals(20, poolInfo.getMaxSize());
        assertEquals(15, poolInfo.getMaxIdle());
        assertEquals(10, poolInfo.getMinSize());
        assertEquals(66, poolInfo.getMaxWait());
        assertEquals(10000, poolInfo.getSleepTime());
        assertEquals(20000, poolInfo.getLifeTime());
        assertEquals(30000, poolInfo.getDeadLockMaxWait());
        assertEquals(40000, poolInfo.getDeadLockRetryWait());
        assertEquals("select 1", poolInfo.getValidationQuery());
        assertEquals((Object) 4000L, poolInfo.getMinEvictableTimeMillis());
        assertEquals((Object) 5000L, poolInfo.getTimeBetweenEvictionRunsMillis());
    }

    @Test
    public void testPartialJdbcConfig() throws Exception {
        DatasourceInfo datasourceInfo = createDatasourceInfo("/TestDatasourceInfo-partialJdbcConfig.xml");
        assertEquals("default", datasourceInfo.getName());
        assertEquals("postgres72", datasourceInfo.getFieldTypeName());
        assertNotNull(datasourceInfo.getJdbcDatasource());
        JdbcDatasourceInfo jdbcInfo = datasourceInfo.getJdbcDatasource();
        assertEquals("jdbc:postgresql://localhost:5432/jira", jdbcInfo.getUri());
        assertEquals("org.postgresql.Driver", jdbcInfo.getDriverClassName());
        assertEquals("jira", jdbcInfo.getUsername());
        assertEquals("password", jdbcInfo.getPassword());
        assertNotNull(jdbcInfo.getConnectionPoolInfo());
        ConnectionPoolInfo poolInfo = jdbcInfo.getConnectionPoolInfo();
        assertEquals(DatasourceInfo.DEFAULT_POOL_MAX_SIZE, poolInfo.getMaxSize());
        assertEquals(poolInfo.getMaxSize(), poolInfo.getMaxIdle());
        assertEquals(ConnectionPoolInfo.DEFAULT_POOL_MIN_SIZE, poolInfo.getMinSize());
        assertEquals(ConnectionPoolInfo.DEFAULT_POOL_MAX_WAIT, poolInfo.getMaxWait());
        assertEquals(ConnectionPoolInfo.DEFAULT_POOL_SLEEP_TIME, poolInfo.getSleepTime());
        assertEquals(ConnectionPoolInfo.DEFAULT_POOL_LIFE_TIME, poolInfo.getLifeTime());
        assertEquals(ConnectionPoolInfo.DEFAULT_DEADLOCK_MAX_WAIT, poolInfo.getDeadLockMaxWait());
        assertEquals(ConnectionPoolInfo.DEFAULT_DEADLOCK_RETRY_WAIT, poolInfo.getDeadLockRetryWait());
    }

    @Test
    public void testJndiConfig() throws Exception {
        DatasourceInfo datasourceInfo = createDatasourceInfo("/TestDatasourceInfo-jndiConfig.xml");
        assertEquals("default", datasourceInfo.getName());
        assertEquals("postgres72", datasourceInfo.getFieldTypeName());
        assertNotNull(datasourceInfo.getJndiDatasource());
        JndiDatasourceInfo jndiInfo = datasourceInfo.getJndiDatasource();
        assertEquals("default", jndiInfo.getJndiServerName());
        assertEquals("java:comp/env/jdbc/JiraDS", jndiInfo.getJndiName());
    }

    @Test
    public void testConnectionPoolInfoToBuilder() throws Exception {
        DatasourceInfo datasourceInfo = createDatasourceInfo("/TestDatasourceInfo-fullJdbcConfig.xml");
        ConnectionPoolInfo.Builder poolInfo = datasourceInfo.getJdbcDatasource().getConnectionPoolInfo().toBuilder();
        assertEquals((Object) 20, poolInfo.getPoolMaxSize());
        assertEquals((Object) 15, poolInfo.getPoolMaxIdle());
        assertEquals((Object) 10, poolInfo.getPoolMinSize());
        assertEquals((Object) 66L, poolInfo.getPoolMaxWait());
        assertEquals((Object) 10000L, poolInfo.getPoolSleepTime());
        assertEquals((Object) 20000L, poolInfo.getPoolLifeTime());
        assertEquals((Object) 30000L, poolInfo.getDeadLockMaxWait());
        assertEquals((Object) 40000L, poolInfo.getDeadLockRetryWait());
        assertEquals("select 1", poolInfo.getValidationQuery());
        assertEquals((Object) 4000L, poolInfo.getMinEvictableTimeMillis());
        assertEquals((Object) 5000L, poolInfo.getTimeBetweenEvictionRunsMillis());
    }

    @Test
    public void testExternalDataProviders() throws Exception {
        DatasourceInfo datasourceInfo = createDatasourceInfo("/TestDatasourceInfo-ExternalUsernameAndPassword.xml");
        JdbcDatasourceInfo jdbcDatasourceInfo = datasourceInfo.getJdbcDatasource();

        assertEquals("UsernameFromExternalSource", jdbcDatasourceInfo.getUsername());
        assertEquals("PasswordFromExternalSource", jdbcDatasourceInfo.getPassword());
    }

    @Test
    public void testExternalPasswordClassDoNotExistSoWeDefaultToNormalPass() throws Exception {
        DatasourceInfo datasourceInfo = createDatasourceInfo("/TestDatasourceInfo-NotExistingClassForExternalPassword.xml");
        JdbcDatasourceInfo jdbcDatasourceInfo = datasourceInfo.getJdbcDatasource();

        assertEquals("password", jdbcDatasourceInfo.getPassword());
    }

    @Test
    public void testExternalPasswordClassThrowsAnErrorSoWePassThisError() throws Exception {
        exception.expect(RuntimeException.class);
        exception.expectMessage("This is exception from external password provider");

        DatasourceInfo datasourceInfo = createDatasourceInfo("/TestDatasourceInfo-ExternalPasswordThrowsAnError.xml");
        datasourceInfo.getJdbcDatasource();
    }


    private DatasourceInfo createDatasourceInfo(String filename) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(getClass().getResourceAsStream(filename));
        return new DatasourceInfo(doc.getDocumentElement());
    }
}
