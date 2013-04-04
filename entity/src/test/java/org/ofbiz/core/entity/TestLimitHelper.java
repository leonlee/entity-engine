package org.ofbiz.core.entity;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ofbiz.core.entity.model.ModelField;

import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @since v1.0.24
 */
public class TestLimitHelper {

    /**
     *  Test the range of supported types, also check that the unsupported dbs are not altered
     *  
     *   Supported types
     *   
     *   <field-type name="hsql" loader="maincp" location="entitydefs/fieldtype-hsql18.xml"/>
     *   <field-type name="mysql" loader="maincp" location="entitydefs/fieldtype-mysql.xml"/>
     *   <field-type name="mssql" loader="maincp" location="entitydefs/fieldtype-mssql.xml"/>
     *   <field-type name="oracle" loader="maincp" location="entitydefs/fieldtype-oracle.xml"/>
     *   <field-type name="oracle10g" loader="maincp" location="entitydefs/fieldtype-oracle10g.xml"/>
     *   <field-type name="postgres" loader="maincp" location="entitydefs/fieldtype-postgres.xml"/>
     *  <field-type name="postgres72" loader="maincp" location="entitydefs/fieldtype-postgres72.xml"/>   
     *   
     *    Unsupported types
     *   <field-type name="cloudscape" loader="maincp" location="entitydefs/fieldtype-cloudscape.xml"/>
     *   <field-type name="firebird" loader="maincp" location="entitydefs/fieldtype-firebird.xml"/>
     *   <field-type name="mckoidb" loader="maincp" location="entitydefs/fieldtype-mckoidb.xml"/> 
     *   <field-type name="sapdb" loader="maincp" location="entitydefs/fieldtype-sapdb.xml"/>
     *   <field-type name="sybase" loader="maincp" location="entitydefs/fieldtype-sybase.xml"/>
     *   <field-type name="db2" loader="maincp" location="entitydefs/fieldtype-db2.xml"/>
     */
    private final String maxResults ="5";
    private final String offset = "1";
    private final String sql = "SELECT jiraissue.ID FROM jiraissue jiraissue INNER JOIN changegroup cg ON jiraissue.ID = cg.issueid WHERE (jiraissue.PROJECT IN (10000) ) ORDER BY cg.CREATED DESC";
    private final Map<String, String> expectedLimitResults = new HashMap<String, String>();
    private final Map<String, String> expectedLimitAndOffsetResults = new HashMap<String, String>();
    private final String top = "SELECT TOP %s jiraissue.ID FROM jiraissue jiraissue INNER JOIN changegroup cg ON jiraissue.ID = cg.issueid WHERE (jiraissue.PROJECT IN (10000) ) ORDER BY cg.CREATED DESC";
    private final String topSql = String.format(top, maxResults);
    private final String limitSql = "SELECT jiraissue.ID FROM jiraissue jiraissue INNER JOIN changegroup cg ON jiraissue.ID = cg.issueid WHERE (jiraissue.PROJECT IN (10000) ) ORDER BY cg.CREATED DESC LIMIT 5";
    private final String subQuery = "SELECT ID FROM (SELECT jiraissue.ID FROM jiraissue jiraissue INNER JOIN changegroup cg ON jiraissue.ID = cg.issueid WHERE (jiraissue.PROJECT IN (10000) ) ORDER BY cg.CREATED DESC) WHERE ROWNUM <= ";
    private final String subQuerySql = subQuery + maxResults;
    private final String limitWithOffsetSql = "SELECT LIMIT 1 5 jiraissue.ID FROM jiraissue jiraissue INNER JOIN changegroup cg ON jiraissue.ID = cg.issueid WHERE (jiraissue.PROJECT IN (10000) ) ORDER BY cg.CREATED DESC";
    private final String limitWithOffsetForPostgresSql = "SELECT jiraissue.ID FROM jiraissue jiraissue INNER JOIN changegroup cg ON jiraissue.ID = cg.issueid WHERE (jiraissue.PROJECT IN (10000) ) ORDER BY cg.CREATED DESC LIMIT 5 OFFSET 1";
    private final String limitWithOffsetForOracleSql = subQuery + maxResults + " MINUS " + subQuery + offset;
    private final String limitWithOffsetForMSSQLSql = topSql + " EXCEPT " + String.format(top, offset);
    private final List<ModelField> emptyFields = new ArrayList<ModelField>();
    private final List<ModelField> idFields = new ArrayList<ModelField>();

    @Before
    public void setupMocks() {
        expectedLimitResults.put("hsql", topSql);
        expectedLimitResults.put("mysql", limitSql);
        expectedLimitResults.put("mssql", topSql);
        expectedLimitResults.put("oracle", subQuerySql);
        expectedLimitResults.put("oracle10g", subQuerySql);
        expectedLimitResults.put("postgres", limitSql);
        expectedLimitResults.put("postgres72", limitSql);
        expectedLimitAndOffsetResults.put("hsql", limitWithOffsetSql);
        expectedLimitAndOffsetResults.put("mysql", limitWithOffsetForPostgresSql);
        expectedLimitAndOffsetResults.put("mssql", limitWithOffsetForMSSQLSql);
        expectedLimitAndOffsetResults.put("oracle", limitWithOffsetForOracleSql);
        expectedLimitAndOffsetResults.put("oracle10g", limitWithOffsetForOracleSql);
        expectedLimitAndOffsetResults.put("postgres", limitWithOffsetForPostgresSql);
        expectedLimitAndOffsetResults.put("postgres72", limitWithOffsetForPostgresSql);
        ModelField idField = mock(ModelField.class);
        when(idField.getColName()).thenReturn("jiraissue.ID");
        idFields.add(idField);
    }

    @Test
    public void testOracleProducesSensibleResultsForProvidedFields() {
        String sql = "SELECT abc.a, A12.b, ABC.c, a12.D FROM jira ORDER BY a";
        LimitHelper helper = new LimitHelper("oracle");
        ModelField field1 = mock(ModelField.class); 
        ModelField field2 = mock(ModelField.class); 
        ModelField field3 = mock(ModelField.class); 
        ModelField field4 = mock(ModelField.class); 
        when(field1.getColName()).thenReturn("abc.a");
        when(field2.getColName()).thenReturn("A12.b");
        when(field3.getColName()).thenReturn("ABC.c");
        when(field4.getColName()).thenReturn("a12.D");
        List<ModelField> modelFields = Arrays.asList(field1, field2, field3, field4);
        Assert.assertEquals("SELECT a,b,c,D FROM (SELECT abc.a, A12.b, ABC.c, a12.D FROM jira ORDER BY a) WHERE ROWNUM <= 5", helper.addLimitClause(sql, modelFields, 5));
    }

    @Test
    public void TestOracleProducesSensibleResultsWithNoFieldsProvided() {
        String sql = "SELECT abc.a, A12.b, ABC.c, a12.D FROM jira ORDER BY a";
        LimitHelper helper = new LimitHelper("oracle");
        Assert.assertEquals("SELECT * FROM (SELECT abc.a, A12.b, ABC.c, a12.D FROM jira ORDER BY a) WHERE ROWNUM <= 5", helper.addLimitClause(sql, emptyFields, 5));
    }

    @Test
    public void testSupportedFieldTypes() {
        for (String fieldType : expectedLimitResults.keySet()) {
            LimitHelper helper = new LimitHelper(fieldType);
            Assert.assertEquals(fieldType+" is supposed to return", expectedLimitResults.get(fieldType), helper.addLimitClause(sql,idFields,5));
        }
    }
    
    @Test
    public void testUnsupportedFieldTypes() {
        LimitHelper helper = new LimitHelper("cloudscape");
        Assert.assertEquals("cloudscape is supposed to return", sql, helper.addLimitClause(sql,idFields,5));
    }

    @Test
    public void testOffsetForSupportedFieldTypes() {
        for (String fieldType : expectedLimitAndOffsetResults.keySet()) {
            LimitHelper helper = new LimitHelper(fieldType);
            Assert.assertEquals(fieldType+" is supposed to return", expectedLimitAndOffsetResults.get(fieldType), helper.addLimitClause(sql,idFields,1,5));
        }
    }

    @Test
    public void testZeroOrNegativeMaxResultsDoesNotAffectQuery() {
        int maxResults = 0;
        for (String fieldType : expectedLimitResults.keySet()) {
            LimitHelper helper = new LimitHelper(fieldType);
            Assert.assertEquals(fieldType+" is supposed to return", sql, helper.addLimitClause(sql,idFields,maxResults--));
        }
    }
}
