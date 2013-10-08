package org.ofbiz.core.entity;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.ofbiz.core.entity.model.ModelEntity;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Pure unit test of {@link GenericHelperDAO}.
 */
public class TestGenericHelperDAO {

    private static final String HELPER_NAME = "myHelper";

    private GenericHelperDAO dao;
    @Mock private GenericDAO mockGenericDao;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        dao = new GenericHelperDAO(HELPER_NAME, mockGenericDao);
    }

    @Test
    public void transformShouldDelegateToDao() throws Exception {
        // Set up
        final ModelEntity mockModelEntity = mock(ModelEntity.class);
        final EntityCondition mockEntityCondition = mock(EntityCondition.class);
        final List<String> orderBy = Arrays.asList("foo", "bar");
        final Transformation mockTransformation = mock(Transformation.class);

        // Invoke
        dao.transform(mockModelEntity, mockEntityCondition, orderBy, mockTransformation);

        // Check
        verify(mockGenericDao).transform(mockModelEntity, mockEntityCondition, orderBy, mockTransformation);
    }
}
