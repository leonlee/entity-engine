package org.ofbiz.core.entity;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.ofbiz.core.entity.config.DatasourceInfo;
import org.ofbiz.core.entity.jdbc.SQLProcessor;
import org.ofbiz.core.entity.jdbc.dbtype.DatabaseType;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.ofbiz.core.entity.model.ModelFieldTypeReader;
import org.ofbiz.core.entity.model.ModelViewEntity;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ofbiz.core.entity.EntityOperator.AND;
import static org.ofbiz.core.entity.EntityOperator.IN;
import static org.ofbiz.core.entity.EntityOperator.OR;
import static org.ofbiz.core.entity.jdbc.dbtype.DatabaseTypeFactory.MSSQL;
import static org.ofbiz.core.entity.jdbc.dbtype.DatabaseTypeFactory.MYSQL;
import static org.ofbiz.core.entity.jdbc.dbtype.DatabaseTypeFactory.ORACLE_10G;
import static org.ofbiz.core.entity.jdbc.dbtype.DatabaseTypeFactory.ORACLE_8I;
import static org.ofbiz.core.entity.jdbc.dbtype.DatabaseTypeFactory.POSTGRES_7_3;

/**
 * Pure unit test of {@link GenericDAO}.
 */
public class GenericDAOTest {

    private static final String HELPER_NAME = "MyHelper";
    private static final String TABLE_NAME = "Issue";

    @Mock
    private CountHelper mockCountHelper;
    @Mock
    private DatabaseType mockDatabaseType;
    @Mock
    private DatasourceInfo mockDatasourceInfo;
    @Mock
    private LimitHelper mockLimitHelper;
    @Mock
    private ModelEntity mockModelEntity;
    @Mock
    private ModelFieldTypeReader mockModelFieldTypeReader;
    private GenericDAO dao;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockModelEntity.getTableName(mockDatasourceInfo)).thenReturn(TABLE_NAME);
        dao = new GenericDAO(HELPER_NAME, mockModelFieldTypeReader, mockDatasourceInfo, mockLimitHelper, mockCountHelper);
        GenericDAO.InQueryRewritter.resetTemporaryTableCounter();
    }

    @Test
    public void testCorrectlyDetectsExpressionToBeRewritten() throws Exception {
        assertTrue(GenericDAO.conditionContainsInClauseWithListOfSizeGreaterThanMaxSize(
                new EntityExpr("test", IN, ImmutableList.of(1, 2)), 1));
        assertFalse(GenericDAO.conditionContainsInClauseWithListOfSizeGreaterThanMaxSize(
                new EntityExpr("test", IN, ImmutableList.of(1, 2)), 5));
        assertFalse(GenericDAO.conditionContainsInClauseWithListOfSizeGreaterThanMaxSize(
                new EntityExpr("test", AND, ImmutableList.of(1, 2)), 1));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCorrectlyRewritesQuery() throws Exception {
        EntityCondition result = GenericDAO.transformConditionSplittingInClauseListsToChunksNoLongerThanMaxSize(
                new EntityExpr("test", IN, ImmutableList.of(1, 2, 3, 4, 5)), 2);

        assertThat(result, instanceOf(EntityExprList.class));
        assertThat(((EntityExprList) result).getExprListSize(), equalTo(3));
        assertThat(((EntityExprList) result).getOperator(), equalTo(OR));
        final List<EntityExpr> exprList = ImmutableList.copyOf(((EntityExprList) result).getExprIterator());
        assertThat(exprList, contains(
                entityExpr("test", IN, ImmutableList.of(1, 2)),
                entityExpr("test", IN, ImmutableList.of(3, 4)),
                entityExpr("test", IN, ImmutableList.of(5))));
    }

    @Test
    public void testCorrectlyRewritesQueryWithoutModification() throws Exception {
        EntityCondition result = GenericDAO.transformConditionSplittingInClauseListsToChunksNoLongerThanMaxSize(
                new EntityExpr("test", IN, ImmutableList.of(1, 2, 3, 4, 5)), 10);

        assertThat(result, instanceOf(EntityExpr.class));
        assertThat((EntityExpr) result, entityExpr("test", IN, ImmutableList.of(1, 2, 3, 4, 5)));
    }

    private Matcher<EntityExpr> entityExpr(
            final String lhs, final EntityOperator operator, final ImmutableList<Integer> rhs) {
        return new BaseMatcher<EntityExpr>() {
            public boolean matches(Object o) {
                return o instanceof EntityExpr
                        && ((EntityExpr) o).getLhs().equals(lhs)
                        && ((EntityExpr) o).getOperator().equals(operator)
                        && Matchers.contains(rhs.toArray()).matches(((EntityExpr) o).getRhs());
            }

            public void describeTo(Description description) {
                description.appendText(new EntityExpr(lhs, operator, rhs).toString());
            }
        };
    }

    @Test
    public void shouldBeAbleToSelectAllColumnsUsingNullListOfSelectFields() throws Exception {
        // Set up
        final EntityFindOptions mockFindOptions = mock(EntityFindOptions.class);

        // Invoke
        final String sql = dao.getSelectQuery(
                null, mockFindOptions, mockModelEntity, null, null, null, null, null, mockDatabaseType);

        // Check
        assertEquals("SELECT * FROM " + TABLE_NAME, sql);
    }

    @Test
    public void shouldBeAbleToSelectSpecificFields() throws Exception {
        // Set up
        final EntityFindOptions mockFindOptions = mock(EntityFindOptions.class);
        final ModelField mockFirstNameField = mock(ModelField.class);
        final ModelField mockLastNameField = mock(ModelField.class);
        final List<ModelField> selectFields = asList(mockFirstNameField, mockLastNameField);
        when(mockModelEntity.colNameString(selectFields, ", ", "")).thenReturn("FIRST_NAME, LAST_NAME");

        // Invoke
        final String sql = dao.getSelectQuery(
                selectFields, mockFindOptions, mockModelEntity, null, null, null, null, null, mockDatabaseType);

        // Check
        assertEquals("SELECT FIRST_NAME, LAST_NAME FROM " + TABLE_NAME, sql);
    }

    @Test
    public void shouldBeAbleToSelectDistinctValues() throws Exception {
        // Set up
        final EntityFindOptions mockFindOptions = mock(EntityFindOptions.class);
        final ModelField mockFirstNameField = mock(ModelField.class);
        final List<ModelField> selectFields = singletonList(mockFirstNameField);
        when(mockModelEntity.colNameString(selectFields, ", ", "")).thenReturn("FIRST_NAME");
        when(mockFindOptions.getDistinct()).thenReturn(true);

        // Invoke
        final String sql = dao.getSelectQuery(
                selectFields, mockFindOptions, mockModelEntity, null, null, null, null, null, mockDatabaseType);

        // Check
        assertEquals("SELECT DISTINCT FIRST_NAME FROM " + TABLE_NAME, sql);
    }

    @Test
    public void shouldBeAbleToProvideEntityWhereCondition() throws Exception {
        // Set up
        final EntityFindOptions mockFindOptions = mock(EntityFindOptions.class);
        final ModelField mockFirstNameField = mock(ModelField.class);
        final List<ModelField> selectFields = singletonList(mockFirstNameField);
        when(mockModelEntity.colNameString(selectFields, ", ", "")).thenReturn("FIRST_NAME");
        when(mockFindOptions.getDistinct()).thenReturn(true);
        final EntityCondition mockWhereCondition = mock(EntityCondition.class);
        final List<EntityConditionParam> whereConditionParams = new ArrayList<EntityConditionParam>();
        when(mockWhereCondition.makeWhereString(mockModelEntity, whereConditionParams)).thenReturn("LAST_NAME IS NULL");

        // Invoke
        final String sql = dao.getSelectQuery(selectFields, mockFindOptions, mockModelEntity, null, mockWhereCondition,
                null, whereConditionParams, null, mockDatabaseType);

        // Check
        assertEquals("SELECT DISTINCT FIRST_NAME FROM " + TABLE_NAME + " WHERE LAST_NAME IS NULL", sql);
    }

    @Test
    public void shouldBeAbleToGroupByFields() throws Exception {
        // Set up
        final ModelViewEntity mockModelViewEntity = mock(ModelViewEntity.class);
        final EntityFindOptions mockFindOptions = mock(EntityFindOptions.class);
        @SuppressWarnings("unchecked")
        final List<ModelField> mockGroupBysCopy = mock(List.class);
        when(mockModelViewEntity.getGroupBysCopy()).thenReturn(mockGroupBysCopy);
        final String groupByString = "LAST_NAME";
        when(mockModelViewEntity.colNameString(mockGroupBysCopy, ", ", "")).thenReturn(groupByString);
        when(mockDatasourceInfo.getJoinStyle()).thenReturn("theta-oracle");
        final Map<String, ModelViewEntity.ModelMemberEntity> memberModelMemberEntities = emptyMap();
        when(mockModelViewEntity.getMemberModelMemberEntities()).thenReturn(memberModelMemberEntities);

        // Invoke
        final String sql = dao.getSelectQuery(
                null, mockFindOptions, mockModelViewEntity, null, null, null, null, null, mockDatabaseType);

        // Check (invalid SQL, but creating valid SQL requires lots of test setup and doesn't test the DAO)
        assertEquals("SELECT * FROM  GROUP BY LAST_NAME", sql);
    }

    @Test
    public void shouldBeAbleToApplyHavingCondition() throws Exception {
        // Set up
        final EntityFindOptions mockFindOptions = mock(EntityFindOptions.class);
        final EntityCondition mockHavingEntityCondition = mock(EntityCondition.class);
        final List<EntityConditionParam> havingConditionParams = new ArrayList<EntityConditionParam>();
        when(mockHavingEntityCondition.makeWhereString(mockModelEntity, havingConditionParams)).thenReturn("BLAH");

        // Invoke
        final String sql = dao.getSelectQuery(null, mockFindOptions, mockModelEntity, null, null,
                mockHavingEntityCondition, null, havingConditionParams, mockDatabaseType);

        // Check (invalid SQL, but creating valid SQL requires lots of test setup and doesn't test the DAO)
        assertEquals("SELECT * FROM Issue HAVING BLAH", sql);
    }

    @Test
    public void shouldBeAbleToRetrieveAGivenPageOfResults() throws Exception {
        // Set up
        final ModelField mockSelectField = mock(ModelField.class);
        final EntityFindOptions mockFindOptions = mock(EntityFindOptions.class);
        final int maxResults = 40;
        final int offset = 2000;
        when(mockFindOptions.getMaxResults()).thenReturn(maxResults);
        when(mockFindOptions.getOffset()).thenReturn(offset);
        final List<ModelField> selectFields = singletonList(mockSelectField);
        when(mockModelEntity.colNameString(selectFields, ", ", "")).thenReturn("Address");
        final String sqlWithLimit = "some SQL with a limit";
        when(mockLimitHelper.addLimitClause("SELECT Address FROM Issue", selectFields, offset, maxResults))
                .thenReturn(sqlWithLimit);

        // Invoke
        final String sql = dao.getSelectQuery(
                selectFields, mockFindOptions, mockModelEntity, null, null, null, null, null, mockDatabaseType);

        // Check
        assertEquals(sqlWithLimit, sql);
    }

    @Test
    public void storeAllShouldAcceptNullEntityList() throws Exception {
        assertEquals(0, dao.storeAll(null));
    }

    @Test
    public void storeAllShouldAcceptEmptyEntityList() throws Exception {
        assertEquals(0, dao.storeAll(Collections.<GenericEntity>emptyList()));
    }

    @Test
    public void createEntityListIteratorMustCloseSQLProcessorIfGenericEntityExceptionIsThrown() throws Exception {
        SQLProcessor sqlProcessor = mock(SQLProcessor.class);
        doThrow(new GenericEntityException()).when(sqlProcessor).prepareStatement(anyString(), anyBoolean(), anyInt(), anyInt());

        createEntityListIteratorExpectingException(sqlProcessor);

        verify(sqlProcessor).close();
    }

    @Test
    public void createEntityListIteratorMustCloseSQLProcessorIfRuntimeExceptionIsThrown() throws Exception {
        SQLProcessor sqlProcessor = mock(SQLProcessor.class);
        doThrow(new RuntimeException()).when(sqlProcessor).prepareStatement(anyString(), anyBoolean(), anyInt(), anyInt());

        createEntityListIteratorExpectingException(sqlProcessor);

        verify(sqlProcessor).close();
    }

    private void createEntityListIteratorExpectingException(final SQLProcessor sqlProcessor) throws Exception {
        String anySql = "any sql";
        List<ModelField> anySelectFields = Collections.emptyList();
        List<EntityConditionParam> anyWhereConditions = Collections.emptyList();
        List<EntityConditionParam> anyHavingConditions = Collections.emptyList();

        try {
            dao.createEntityListIterator(sqlProcessor, anySql, mock(EntityFindOptions.class), mockModelEntity, anySelectFields, anyWhereConditions, anyHavingConditions, null);
            fail("An exception was expected to be thrown");
        } catch (Exception e) {
            // do nothing, we were expecting the exception
        }
    }

    @Test
    public void testRewriteWithTemporaryTables() {
        List<Integer> ids = Collections.nCopies(2001, 1);
        ModelEntity modelEntity = new ModelEntity();
        ModelField field = new ModelField();
        field.setName("test");
        modelEntity.addField(field);

        final GenericDAO.InQueryRewritter inQueryRewritter = new GenericDAO.InQueryRewritter(MSSQL, new EntityExpr("test", IN, ids), modelEntity);
        Optional<GenericDAO.WhereRewrite> rewrite = inQueryRewritter.rewriteConditionToUseTemporaryTablesForLargeInClauses();

        assertTrue("Rewrite should be required.", rewrite.isPresent());
        Collection<GenericDAO.InReplacement> replacements = rewrite.get().getInReplacements();
        assertThat(replacements, hasSize(1));
        GenericDAO.InReplacement replacement = replacements.iterator().next();
        assertEquals("Wrong replacements.", ids, replacement.getItems());
        assertEquals("Wrong temp table name.", "#temp1", replacement.getTemporaryTableName());

        EntityCondition rewrittenCondition = rewrite.get().getNewCondition();
        assertThat(rewrittenCondition, instanceOf(EntityExpr.class));
        EntityExpr rewrittenExpr = (EntityExpr) rewrittenCondition;
        assertEquals("test", rewrittenExpr.getLhs());
        assertEquals(IN, rewrittenExpr.getOperator());
        assertThat(rewrittenExpr.getRhs(), instanceOf(EntityWhereString.class));
        EntityWhereString rhs = (EntityWhereString) rewrittenExpr.getRhs();
        assertEquals("select item from #temp1", rhs.sqlString);
    }

    @Test
    public void testRewriteWithTemporaryTablesTwoSmallerInFragments() {
        List<Integer> ids = Collections.nCopies(1001, 1);
        ModelEntity modelEntity = new ModelEntity();
        ModelField field1 = new ModelField();
        field1.setName("test1");
        modelEntity.addField(field1);
        ModelField field2 = new ModelField();
        field2.setName("test2");
        modelEntity.addField(field2);

        EntityExpr expr1 = new EntityExpr("test1", IN, ids);
        EntityExpr expr2 = new EntityExpr("test2", IN, ids);
        EntityExprList outerExpr = new EntityExprList(ImmutableList.of(expr1, expr2), EntityOperator.OR);

        final GenericDAO.InQueryRewritter inQueryRewritter = new GenericDAO.InQueryRewritter(MSSQL, outerExpr, modelEntity);
        Optional<GenericDAO.WhereRewrite> rewrite = inQueryRewritter.rewriteConditionToUseTemporaryTablesForLargeInClauses();

        assertTrue("Rewrite should be required.", rewrite.isPresent());
        Collection<GenericDAO.InReplacement> replacements = rewrite.get().getInReplacements();
        assertThat(replacements, hasSize(2));
        GenericDAO.InReplacement replacement = replacements.iterator().next();
        assertEquals("Wrong replacements.", ids, replacement.getItems());
        assertEquals("Wrong temp table name.", "#temp1", replacement.getTemporaryTableName());

        EntityCondition rewrittenCondition = rewrite.get().getNewCondition();
        assertThat(rewrittenCondition, instanceOf(EntityConditionList.class));
        EntityConditionList outerCondition = (EntityConditionList) rewrittenCondition;
        assertEquals(2, outerCondition.getConditionListSize());
        EntityExpr rewritten1 = (EntityExpr) outerCondition.getCondition(0);
        EntityExpr rewritten2 = (EntityExpr) outerCondition.getCondition(1);

        assertEquals("test1", rewritten1.getLhs());
        assertEquals("test2", rewritten2.getLhs());
        assertEquals(IN, rewritten1.getOperator());
        assertEquals(IN, rewritten2.getOperator());
        assertThat(rewritten1.getRhs(), instanceOf(EntityWhereString.class));
        assertThat(rewritten2.getRhs(), instanceOf(EntityWhereString.class));
        EntityWhereString rhs1 = (EntityWhereString) rewritten1.getRhs();
        EntityWhereString rhs2 = (EntityWhereString) rewritten2.getRhs();
        assertEquals("select item from #temp1", rhs1.sqlString);
        assertEquals("select item from #temp2", rhs2.sqlString);
    }

    /**
     * Temp table rewriteIfNeeded should not occur with too few parameters.
     */
    @Test
    public void testNoRewriteWithNotEnoughParameters() {
        List<Integer> ids = Collections.nCopies(2000, 1);
        ModelEntity modelEntity = new ModelEntity();
        ModelField field = new ModelField();
        field.setName("test");
        modelEntity.addField(field);

        final GenericDAO.InQueryRewritter inQueryRewritter = new GenericDAO.InQueryRewritter(MSSQL, new EntityExpr("test", IN, ids), modelEntity);

        Optional<GenericDAO.WhereRewrite> rewrite = inQueryRewritter.rewriteConditionToUseTemporaryTablesForLargeInClauses();

        assertFalse("Rewrite should not be required.", rewrite.isPresent());
    }

    @Test
    public void testShouldRewriteOnlyForMssqlAndPostgres() {
        ImmutableMap<DatabaseType, Boolean> databases = ImmutableMap.<DatabaseType, Boolean>builder()
                .put(MSSQL, true)
                .put(POSTGRES_7_3, true)
                .put(MYSQL, false)
                .put(ORACLE_8I, false)
                .put(ORACLE_10G, false)
                .build();
        final int size = 100000;

        for (Map.Entry<DatabaseType, Boolean> entry : databases.entrySet()) {
            final List<Integer> ids = Collections.nCopies(size, 1);
            final ModelEntity modelEntity = new ModelEntity();
            final ModelField field = new ModelField();
            field.setName("test");
            modelEntity.addField(field);

            final GenericDAO.InQueryRewritter inQueryRewritter = new GenericDAO.InQueryRewritter(entry.getKey(), new EntityExpr("test", IN, ids), modelEntity);
            inQueryRewritter.rewriteIfNeeded();
            assertThat("Should have proper value for " + entry.getKey(), inQueryRewritter.isRewritten(), equalTo(entry.getValue()));
        }
    }

    @Test
    public void testShouldGenerateProperSqlForCreateTable() throws GenericEntityException {
        verifyCreateTableSql(MSSQL, "create table #temp1 (item bigint primary key)");
        verifyCreateTableSql(POSTGRES_7_3, "create temporary table temp2 (item bigint primary key)");
    }

    private void verifyCreateTableSql(DatabaseType databaseType, String expectedSql) throws GenericEntityException {
        final List<Integer> ids = Collections.nCopies(50000, 1);
        final ModelEntity modelEntity = new ModelEntity();
        final ModelField field = new ModelField();
        field.setName("test");
        modelEntity.addField(field);

        final SQLProcessor mockSqlProcessor = mock(SQLProcessor.class);
        when(mockSqlProcessor.getPreparedStatement()).thenReturn(mock(PreparedStatement.class));


        final GenericDAO.InQueryRewritter inQueryRewritter = new GenericDAO.InQueryRewritter(databaseType, new EntityExpr("test", IN, ids), modelEntity);
        inQueryRewritter.rewriteIfNeeded();
        inQueryRewritter.createTemporaryTablesIfNeeded(mockSqlProcessor);

        final ArgumentCaptor<String> executeUpdateParameter = ArgumentCaptor.forClass(String.class);
        verify(mockSqlProcessor).executeUpdate(executeUpdateParameter.capture());

        assertThat(executeUpdateParameter.getValue(), equalTo(expectedSql));
    }

    @Test
    public void testShouldReturnProperCleanUpObjectWhenQueryIsRewritten() throws GenericEntityException {
        final List<Integer> ids = Collections.nCopies(2001, 1);
        final ModelEntity modelEntity = new ModelEntity();
        final ModelField field = new ModelField();
        field.setName("test");
        modelEntity.addField(field);

        final SQLProcessor mockSqlProcessor = mock(SQLProcessor.class);
        when(mockSqlProcessor.getPreparedStatement()).thenReturn(mock(PreparedStatement.class));

        final GenericDAO.InQueryRewritter inQueryRewritter = new GenericDAO.InQueryRewritter(MSSQL, new EntityExpr("test", IN, ids), modelEntity);
        inQueryRewritter.rewriteIfNeeded();
        inQueryRewritter.createTemporaryTablesIfNeeded(mockSqlProcessor);

        final ArgumentCaptor<String> executeUpdateParameter = ArgumentCaptor.forClass(String.class);
        verify(mockSqlProcessor).executeUpdate(executeUpdateParameter.capture());

        assertThat(executeUpdateParameter.getValue(), equalTo("create table #temp1 (item bigint primary key)"));

        GenericDAO.TableCleanUp tableCleanUp = inQueryRewritter.getTableCleanUpHandler();
        assertNotNull(tableCleanUp);

        final SQLProcessor mockSqlProcessorForCleanup = mock(SQLProcessor.class);
        tableCleanUp.cleanUp(mockSqlProcessorForCleanup);
        final ArgumentCaptor<String> executeUpdateParameterCleanUp = ArgumentCaptor.forClass(String.class);
        verify(mockSqlProcessorForCleanup).executeUpdate(executeUpdateParameterCleanUp.capture());

        assertThat(executeUpdateParameterCleanUp.getValue(), equalTo("drop table #temp1"));
    }


}
