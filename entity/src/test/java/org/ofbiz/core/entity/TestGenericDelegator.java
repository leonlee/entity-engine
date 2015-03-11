package org.ofbiz.core.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.ofbiz.core.entity.model.ModelEntity;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.xml.parsers.ParserConfigurationException;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ofbiz.core.entity.EntityOperator.EQUALS;
import static org.ofbiz.core.entity.EntityOperator.LIKE;
import static org.ofbiz.core.entity.GenericDelegator.getGenericDelegator;

/**
 * Integration test of GenericDelegator using an in-memory database and real collaborators.
 */
public class TestGenericDelegator
{
    // These names are from the test XML files in src/test/resources
    private static final String DELEGATOR_NAME = "default";
    private static final String ENTITY_GROUP_NAME = "default";
    private static final String ID_FIELD = "id";
    private static final String ISSUE_COUNT_FIELD = "counter";
    private static final String ISSUE_ENTITY = "Issue";
    private static final String ISSUE_KEY_FIELD = "key";
    private static final String PROJECT_ENTITY = "Project";
    private static final String PROJECT_KEY_FIELD = "key";
    private static final EntityExpr PROJECT_KEY_LIKE_B_PERCENT = new EntityExpr(PROJECT_KEY_FIELD, LIKE, "B%");
    private static final String SEQUENCE_ENTITY = "SequenceValueItem";

    // Be sure to list all entities in the "default" group here
    private static final String[] ENTITIES = { ISSUE_ENTITY, SEQUENCE_ENTITY, PROJECT_ENTITY };
    private static final int PROJECT_ID_1 = 23;

    private GenericDelegator genericDelegator;

    @Before
    public void setUp() throws Exception {
        GenericDelegator.removeGenericDelegator(DELEGATOR_NAME);
        GenericDelegator.unlock();
        genericDelegator = getGenericDelegator(DELEGATOR_NAME);
        resetDatabase();
    }

    private void resetDatabase() throws Exception {
        genericDelegator.removeByCondition(PROJECT_ENTITY, null);
        genericDelegator.removeByCondition(ISSUE_ENTITY, null);
    }

    @Test
    public void callingFactoryMethodWithIsLockedSetToFalseShouldReturnGenericDelegator() {
        // Check
        assertNotNull(genericDelegator);
        assertEquals(GenericDelegator.class, genericDelegator.getClass());
    }

    @Test
    public void shouldBeAbleToRemoveGenericDelegator() {
        // Set up
        GenericDelegator initialGD = getGenericDelegator(DELEGATOR_NAME);
        assertEquals(GenericDelegator.class, initialGD.getClass());

        // Invoke
        GenericDelegator.removeGenericDelegator(DELEGATOR_NAME);

        // Check that a different instance has been returned, not the cached one
        GenericDelegator finalGD = getGenericDelegator(DELEGATOR_NAME);
        assertNotSame(initialGD, finalGD);
    }

    @Test
    public void initialEntityCountShouldBeZero() throws Exception {
        // Invoke
        final long projectCount = genericDelegator.countAll(PROJECT_ENTITY);

        // Check
        assertEquals(0, projectCount);
    }

    @Test
    public void entityCountShouldBeOneAfterInsertingAnEntity() throws Exception {
        // Set up
        final String projectKey = "FOO";
        final int issueCount = 230;
        final long projectId = genericDelegator.getNextSeqId(PROJECT_ENTITY);
        final Map<String, Object> fields = getProjectFields(projectId, projectKey, issueCount);
        final GenericValue project = genericDelegator.create(PROJECT_ENTITY, fields);

        // Invoke
        final long projectCount = genericDelegator.countAll(PROJECT_ENTITY);

        // Check
        assertEquals(1, projectCount);
        assertProject(projectId, projectKey, issueCount, project);
    }

    private Map<String, Object> getProjectFields(final long projectId, final String projectKey, final long issueCount)
    {
        return ImmutableMap.<String, Object>of(
                ID_FIELD, projectId,
                PROJECT_KEY_FIELD, projectKey,
                ISSUE_COUNT_FIELD, issueCount
        );
    }

    @Test
    public void shouldBeAbleToGetEntityGroupName() {
        assertEquals(ENTITY_GROUP_NAME, genericDelegator.getEntityGroupName(PROJECT_ENTITY));
    }

    @Test
    public void shouldBeAbleToGetModelEntitiesByGroup() {
        // Invoke
        final List<ModelEntity> entities = genericDelegator.getModelEntitiesByGroup(ENTITY_GROUP_NAME);

        // Check
        assertThat(entities, containsInAnyOrder(modelEntities(ENTITIES)));
    }

    @Test
    public void shouldBeAbleToGetModelEntityMapByGroup() {
        // Invoke
        final Map<String, ModelEntity> entities = genericDelegator.getModelEntityMapByGroup(ENTITY_GROUP_NAME);

        // Check
        for (final String entityName : ENTITIES)
        {
            assertThat(entities, hasEntry(is(entityName), modelEntity(entityName)));
        }
        assertEquals(3, entities.size());
    }

    @Test
    public void getModelEntityShouldReturnNullForUnknownEntity() {
        assertNull(genericDelegator.getModelEntity("dfhdfsfgfdgjhsg"));
    }

    @Test
    public void getModelEntitiesByGroupShouldReturnEmptyListForUnknownGroup() {
        assertEquals(Collections.<ModelEntity>emptyList(), genericDelegator.getModelEntitiesByGroup("fhgfdjhg"));
    }

    @Test
    public void getModelEntityMapByGroupShouldReturnEmptyListForUnknownGroup() {
        assertEquals(Collections.<String, ModelEntity>emptyMap(), genericDelegator.getModelEntityMapByGroup("fhgfdjhg"));
    }

    private void assertModelEntity(final String expectedName, final ModelEntity actualEntity) {
        assertEquals(expectedName, actualEntity.getEntityName());
    }

    @Test
    public void getEntityHelperNameShouldReturnNullForNullModelEntity() {
        assertNull(genericDelegator.getEntityHelperName((ModelEntity)null));
    }

    @Test
    public void shouldBeAbleToGetEntityHelperForKnownModelEntity() throws Exception {
        // Set up
        final ModelEntity mockModelEntity = mock(ModelEntity.class);
        when(mockModelEntity.getEntityName()).thenReturn(ENTITIES[0]);

        // Invoke
        final GenericHelper entityHelper = genericDelegator.getEntityHelper(mockModelEntity);

        // Check
        assertEquals("defaultDS", entityHelper.getHelperName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBeAbleToMakeEntityWithInvalidName() {
        genericDelegator.makeValue("gfjjhkdf", Collections.<String, Object>emptyMap());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBeAbleToMakePrimaryKeyWithInvalidEntityName() {
        genericDelegator.makePK("gfjjhkdf", Collections.<String, Object>emptyMap());
    }

    @Test
    public void shouldBeAbleToMakeEntityWithValidName() throws Exception {
        // Set up
        final long projectId = 666;
        final String projectKey = "BAR";
        final int issueCount = 27;
        final Map<String, Object> fields = getProjectFields(projectId, projectKey, issueCount);

        // Invoke
        final GenericValue genericValue = genericDelegator.makeValue(PROJECT_ENTITY, fields);

        // Check
        assertProject(projectId, projectKey, issueCount, genericValue);
        assertEquals(0, genericDelegator.countAll(PROJECT_ENTITY));
    }

    @Test
    public void shouldBeAbleToMakePrimaryKeyWithValidName() throws Exception {
        // Set up
        final long projectId = 42;
        final String projectKey = "BAR";
        final int issueCount = 2000;
        final Map<String, Object> fields = getProjectFields(projectId, projectKey, issueCount);

        // Invoke
        final GenericPK primaryKey = genericDelegator.makePK(PROJECT_ENTITY, fields);

        // Check
        assertProject(projectId, projectKey, issueCount, primaryKey);
        assertEquals(0, genericDelegator.countAll(PROJECT_ENTITY));
    }

    private void assertProject(final long expectedId, final String expectedKey, final long expectedIssueCount,
            final GenericEntity actualProject)
    {
        assertNotNull(actualProject);
        assertEquals(PROJECT_ENTITY, actualProject.getEntityName());
        assertEquals(expectedId, actualProject.getLong(ID_FIELD).longValue());
        assertEquals(expectedKey, actualProject.getString(PROJECT_KEY_FIELD));
        assertEquals(expectedIssueCount, actualProject.getLong(ISSUE_COUNT_FIELD).longValue());
        assertSame(genericDelegator, actualProject.getDelegator());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBeAbleToCreateWithNullPrimaryKey() throws Exception {
        genericDelegator.create((GenericPK)null, false);
    }

    @Test
    public void creatingFromPrimaryKeyShouldInsertIntoTheDatabase() throws Exception {
        // Set up
        final long issueId = 1234;
        final Map<String, ?> issueFields = ImmutableMap.of(ID_FIELD, issueId);
        final GenericPK primaryKey = genericDelegator.makePK(ISSUE_ENTITY, issueFields);

        // Invoke
        final GenericValue createdValue = genericDelegator.create(primaryKey);

        // Check
        assertNotNull(createdValue);
        assertEquals(issueId, createdValue.getLong(ID_FIELD).longValue());
        assertSame(genericDelegator, createdValue.getDelegator());
        assertNull(createdValue.getString(ISSUE_KEY_FIELD));
    }

    @Test
    public void creatingWithNullEntityNameShouldReturnNull() throws Exception {
        assertNull(genericDelegator.create(null, singletonMap("foo", 27)));
    }

    @Test
    public void creatingWithNullFieldMapShouldReturnNull() throws Exception {
        assertNull(genericDelegator.create(ISSUE_ENTITY, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void findByPrimaryKeyShouldRejectInvalidKey() throws Exception {
        // Set up
        final GenericPK genericPK = genericDelegator.makePK(ISSUE_ENTITY, Collections.<String, Object>emptyMap());

        // Invoke
        genericDelegator.findByPrimaryKey(genericPK);
    }

    @Test(expected = IllegalArgumentException.class)
    public void findPartialByPrimaryKeyShouldRejectInvalidKey() throws Exception {
        // Set up
        final GenericPK genericPK = genericDelegator.makePK(ISSUE_ENTITY, Collections.<String, Object>emptyMap());

        // Invoke
        genericDelegator.findByPrimaryKeyPartial(genericPK, singleton(ISSUE_KEY_FIELD));
    }

    @Test
    public void newlyCreatedEntityShouldBeFindableByPrimaryKey() throws Exception {
        // Set up
        final long issueId = 345;
        final GenericPK issuePK = genericDelegator.makePK(ISSUE_ENTITY, singletonMap(ID_FIELD, issueId));
        genericDelegator.create(issuePK);

        // Invoke
        final GenericValue issueByPrimaryKey = genericDelegator.findByPrimaryKey(issuePK);

        // Check
        assertEquals(issueId, issueByPrimaryKey.getLong(ID_FIELD).longValue());
    }

    @Test
    public void findingByUnknownPrimaryKeyShouldReturnNull() throws Exception {
        // Set up
        final GenericPK invalidPK = genericDelegator.makePK(ISSUE_ENTITY, singletonMap(ID_FIELD, Long.MAX_VALUE));

        // Invoke and check
        assertNull(genericDelegator.findByPrimaryKey(invalidPK));
    }

    @Test
    public void findingPartialByUnknownPrimaryKeyShouldReturnNull() throws Exception {
        // Set up
        final GenericPK invalidPK = genericDelegator.makePK(ISSUE_ENTITY, singletonMap(ID_FIELD, Long.MAX_VALUE));

        // Invoke and check
        assertNull(genericDelegator.findByPrimaryKeyPartial(invalidPK, singleton(ISSUE_KEY_FIELD)));
    }

    @Test
    public void entityShouldBeFindableByPrimaryKeyFields() throws Exception {
        // Set up
        final long issueId = 345;
        final GenericPK issuePK = genericDelegator.makePK(ISSUE_ENTITY, singletonMap(ID_FIELD, issueId));
        genericDelegator.create(issuePK);

        // Invoke
        final GenericValue issueByPrimaryKey =
                genericDelegator.findByPrimaryKey(ISSUE_ENTITY, singletonMap(ID_FIELD, issueId));

        // Check
        assertEquals(issueId, issueByPrimaryKey.getLong(ID_FIELD).longValue());
    }

    @Test
    public void findByPrimaryKeyPartialShouldOnlyReturnRequestedFields() throws Exception {
        final Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ID_FIELD, 27L);
        fields.put(PROJECT_KEY_FIELD, "FOO");
        final long issueCount = 500;
        fields.put(ISSUE_COUNT_FIELD, issueCount);
        final GenericPK insertedPK = genericDelegator.create(PROJECT_ENTITY, fields).getPrimaryKey();

        // Invoke
        final GenericValue partialProject =
                genericDelegator.findByPrimaryKeyPartial(insertedPK, singleton(ISSUE_COUNT_FIELD));

        // Check
        assertNotNull(partialProject);
        assertEquals(issueCount, partialProject.getLong(ISSUE_COUNT_FIELD).longValue());
        assertFalse("Did not ask for the project key field", partialProject.containsKey(PROJECT_KEY_FIELD));
    }

    @Test
    public void findAllByPrimaryKeysShouldReturnNullForNullArgument() throws Exception {
        assertNull(genericDelegator.findAllByPrimaryKeys(null));
    }

    @Test
    public void findAllByPrimaryKeysShouldBeAbleToFindMultipleEntityTypes() throws Exception {
        // Set up
        final GenericValue project = genericDelegator.create(PROJECT_ENTITY, getProjectFields(123, "PROJ", 456));
        final GenericValue issue = genericDelegator.create(ISSUE_ENTITY, getIssueFields(789, "PROJ-345"));

        // Invoke
        final List<GenericValue> entities =
                genericDelegator.findAllByPrimaryKeys(asList(project.getPrimaryKey(), issue.getPrimaryKey()));

        // Check
        assertEquals(2, entities.size());
        assertEquals(project, entities.get(0));
        assertEquals(issue, entities.get(1));
    }

    private Map<String,?> getIssueFields(final long id, final String key) {
        return ImmutableMap.of(ID_FIELD, id, ISSUE_KEY_FIELD, key);
    }

    @Test
    public void clearAllCacheLinesByDummyPKShouldAcceptNullArgument() {
        genericDelegator.clearAllCacheLinesByDummyPK(null);
    }

    @Test
    public void gettingFromPrimaryKeyCacheWithNullPrimaryKeyShouldReturnNull() {
        assertNull(genericDelegator.getFromPrimaryKeyCache(null));
    }

    @Test
    public void gettingFromAllCacheWithNullEntityNameShouldReturnNull() {
        assertNull(genericDelegator.getFromAllCache(null));
    }

    @Test
    public void gettingFromAndCacheWithNullEntityNameShouldReturnNull() {
        assertNull(genericDelegator.getFromAndCache((String)null, singletonMap(ID_FIELD, 789L)));
    }

    @Test
    public void gettingFromAndCacheWithNullModelEntityShouldReturnNull() {
        assertNull(genericDelegator.getFromAndCache((ModelEntity) null, singletonMap(ID_FIELD, 789L)));
    }

    @Test
    public void gettingFromAndCacheWithNullFieldMapShouldReturnNull() {
        assertNull(genericDelegator.getFromAndCache(ISSUE_ENTITY, null));
    }

    @Test
    public void makeValuesShouldReturnNullForNullXmlDocument() {
        assertNull(genericDelegator.makeValues(null));
    }

    @Test
    public void readXmlDocumentShouldReturnNullForNullUrl() throws Exception {
        assertNull(genericDelegator.readXmlDocument(null));
    }

    @Test
    public void shouldReadEntitiesFromValidXmlFile() throws Exception {
        // Invoke
        final List<GenericValue> entities = loadTestEntitiesFromXml("test-entities.xml");

        // Check
        assertEquals(4, entities.size());
        assertProject(23, "BAZ", 567, entities.get(0));
        assertProject(24, "BAR", 568, entities.get(1));
        assertProject(25, "FOO", 600, entities.get(2));
        assertIssue(25, "BAR-123", entities.get(3));
        // Check the database was not updated
        assertEquals(0, genericDelegator.countAll(PROJECT_ENTITY));
        assertEquals(0, genericDelegator.countAll(ISSUE_ENTITY));
    }

    private List<GenericValue> loadTestEntitiesFromXml(final String xmlFilename)
            throws SAXException, ParserConfigurationException, IOException
    {
        final Class<?> loadingClass = getClass();
        final URL xmlUrl = loadingClass.getResource(xmlFilename);
        assertNotNull(
                "Couldn't find " + xmlFilename + " in the package " + loadingClass.getPackage().getName(), xmlUrl);
        return genericDelegator.readXmlDocument(xmlUrl);
    }

    private void assertIssue(final long expectedId, final String expectedKey, final GenericValue actualIssue) {
        assertEquals(ISSUE_ENTITY, actualIssue.getEntityName());
        assertEquals(expectedId, actualIssue.getLong(ID_FIELD).longValue());
        assertEquals(expectedKey, actualIssue.getString(ISSUE_KEY_FIELD));
    }

    @Test
    public void shouldRejectXmlFileWithWrongRootElement() throws Exception {
        // Set up
        final URL xmlFileUrl = getClass().getResource("bad-entities.xml");

        // Invoke
        try {
            genericDelegator.readXmlDocument(xmlFileUrl);
            fail("Expected a " + IllegalArgumentException.class);
        }
        catch (final IllegalArgumentException e)
        {
            assertEquals("Root node was not <entity-engine-xml>", e.getMessage());
        }
    }

    @Test
    public void makeValuesShouldReturnEmptyListWhenFileContainsNoEntities() throws Exception {
        // Set up
        final URL xmlFileUrl = getClass().getResource("no-entities.xml");

        // Invoke
        final List<GenericValue> entities = genericDelegator.readXmlDocument(xmlFileUrl);

        // Check
        assertEquals(Collections.<GenericValue>emptyList(), entities);
    }

    @Test
    public void makeValueShouldReturnNullGivenNullElement() {
        assertNull(genericDelegator.makeValue(null));
    }

    @Test
    public void shouldBeAbleToFindUsingLikeOperator() throws Exception {
        // Set up
        genericDelegator.storeAll(loadTestEntitiesFromXml("test-entities.xml"));
        
        // Invoke
        final List<GenericValue> matchingProjects = genericDelegator.findByCondition(
                PROJECT_ENTITY, PROJECT_KEY_LIKE_B_PERCENT, null, singletonList(ID_FIELD));

        // Check
        assertEquals(2, matchingProjects.size());
        assertProject(23, "BAZ", 567, matchingProjects.get(0));
        assertProject(24, "BAR", 568, matchingProjects.get(1));
    }

    @Test
    public void shouldBeAbleToFindUsingNullSelectAndOrderByColumns() throws Exception {
        // Set up
        genericDelegator.storeAll(loadTestEntitiesFromXml("test-entities.xml"));
        final EntityExpr keyEqualsFoo = new EntityExpr(PROJECT_KEY_FIELD, EQUALS, "FOO");

        // Invoke
        final List<GenericValue> matchingProjects =
                genericDelegator.findByCondition(PROJECT_ENTITY, keyEqualsFoo, null, null);

        // Check
        assertEquals(1, matchingProjects.size());
        assertProject(25, "FOO", 600, matchingProjects.get(0));
    }

    @Test
    public void transformShouldUpdateTheDatabaseAndReturnTheModifiedEntities() throws Exception {
        // Set up
        genericDelegator.storeAll(loadTestEntitiesFromXml("test-entities.xml"));
        final Transformation transformation = new IncrementIssueCount();

        // Invoke
        final List<GenericValue> transformedProjects = genericDelegator.transform(
                PROJECT_ENTITY, PROJECT_KEY_LIKE_B_PERCENT, singletonList("key ASC"), ISSUE_COUNT_FIELD, transformation);

        // Check
        assertEquals(2, transformedProjects.size());
        for (final GenericValue transformedProject : transformedProjects) {
            // Ensure the returned values are what's persisted in the database
            assertEquals(transformedProject, genericDelegator.findByPrimaryKey(transformedProject.getPrimaryKey()));
        }
        assertProject(24, "BAR", 569, transformedProjects.get(0));
        assertProject(PROJECT_ID_1, "BAZ", 568, transformedProjects.get(1));
    }

    @Test
    public void transformationShouldBeReappliedIfLockColumnHasChangedBetweenSelectAndUpdate() throws Exception {
        // Set up
        genericDelegator.storeAll(loadTestEntitiesFromXml("test-entities.xml"));
        final int projectId = PROJECT_ID_1;
        final int threads = 50;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(threads);
        final Executor executor = Executors.newFixedThreadPool(threads);
        final Collection<IssueCountIncrementer> incrementers = new ArrayList<IssueCountIncrementer>();
        for (int i = 0; i < threads; i++) {
            final IssueCountIncrementer incrementer =
                    new IssueCountIncrementer(startLatch, endLatch, genericDelegator, projectId);
            executor.execute(incrementer);
            incrementers.add(incrementer);
        }

        // Go!
        startLatch.countDown();
        endLatch.await();

        // Check
        final Set<Long> newIssueCounts = new HashSet<Long>();
        for (final IssueCountIncrementer incrementer : incrementers) {
            newIssueCounts.add(incrementer.getNewIssueCount());
        }
        assertEquals("Actual new issue counts = " + newIssueCounts, threads, newIssueCounts.size());
        final long initialIssueCount = 567; // from test-entities.xml
        for (int i = 0; i < threads; i++) {
            final long expectedIssueCount = initialIssueCount + i + 1;
            assertTrue("Missing issue count " + expectedIssueCount, newIssueCounts.contains(expectedIssueCount));
        }
    }

    @Test
    public void transformingNonExistentEntityShouldReturnEmptyList() throws Exception {
        // Set up
        genericDelegator.storeAll(loadTestEntitiesFromXml("test-entities.xml"));
        final EntityExpr invalidIdCondition = new EntityExpr(ID_FIELD, EQUALS, Long.MAX_VALUE);

        // Invoke
        final List<GenericValue> transformedEntities = genericDelegator.transform(
                PROJECT_ENTITY, invalidIdCondition, null, ISSUE_COUNT_FIELD, new Transformation()
        {
            @Override
            public void transform(final GenericValue entity)
            {
                // Doesn't matter what we do here
            }
        });

        // Check
        assertEquals(Collections.<GenericValue>emptyList(), transformedEntities);
    }

    private static List<Matcher<? super ModelEntity>> modelEntities(final String... expectedNames)
    {
        final ImmutableList.Builder<Matcher<? super ModelEntity>> list = ImmutableList.builder();
        for (String expectedName : expectedNames)
        {
            list.add(modelEntity(expectedName));
        }
        return list.build();
    }

    private static Matcher<ModelEntity> modelEntity(final String expectedName)
    {
        return new TypeSafeMatcher<ModelEntity>()
        {
            @Override
            protected boolean matchesSafely(ModelEntity modelEntity)
            {
                return expectedName.equals(modelEntity.getEntityName());
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText("ModelEntity").appendValue(expectedName);
            }

            @Override
            protected void describeMismatchSafely(ModelEntity item, Description mismatchDescription)
            {
                mismatchDescription.appendText("ModelEntity").appendValue(item.getEntityName());
            }
        };
    }

    /**
     * A {@link Runnable} that increments the issue count for a given project.
     */
    private static class IssueCountIncrementer implements Runnable {

        private final int projectId;
        private final CountDownLatch endLatch;
        private final CountDownLatch startLatch;
        private final GenericDelegator delegator;
        private final List<GenericValue> transformedEntities;
        private GenericEntityException ex;

        private IssueCountIncrementer(final CountDownLatch startLatch, final CountDownLatch endLatch,
                final GenericDelegator delegator, final int projectId)
        {
            this.delegator = delegator;
            this.endLatch = endLatch;
            this.projectId = projectId;
            this.startLatch = startLatch;
            this.transformedEntities = new ArrayList<GenericValue>();
        }

        @Override
        public void run() {
            final EntityCondition selectCondition = new EntityExpr(ID_FIELD, EQUALS, projectId);
            final Transformation transformation = new IncrementIssueCount();
            waitForTheGoSignal();
            incrementIssueCount(selectCondition, transformation);
            signalCompletion();
        }

        private void waitForTheGoSignal() {
            try {
                startLatch.await();
            }
            catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        private void incrementIssueCount(final EntityCondition selectCondition, final Transformation transformation) {
            try {
                transformedEntities.addAll(
                        delegator.transform(PROJECT_ENTITY, selectCondition, null, ISSUE_COUNT_FIELD, transformation));
            }
            catch (final GenericEntityException ex) {
                this.ex = ex;
            }
        }

        private void signalCompletion() {
            endLatch.countDown();
        }

        private long getNewIssueCount() throws GenericEntityException
        {
            final Thread currentThread = currentThread();
            if (ex != null) {
                throw new IllegalStateException("Error in thread " + currentThread, ex);
            }
            assertTrue(currentThread + ": actual entities = " + transformedEntities, transformedEntities.size() == 1);
            final GenericValue transformedProject = transformedEntities.get(0);
            return transformedProject.getLong(ISSUE_COUNT_FIELD);
        }
    }

    private static class IncrementIssueCount implements Transformation {

        @Override
        public void transform(final GenericValue project) {
            final long issueCount = project.getLong(ISSUE_COUNT_FIELD);
            project.set(ISSUE_COUNT_FIELD, issueCount + 1);
        }
    }
}
