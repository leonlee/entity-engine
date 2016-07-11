package org.ofbiz.core.entity;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.ofbiz.core.entity.GenericDelegator.getGenericDelegator;

/**
 *
 * @since v1.0.26
 */
@RunWith(MockitoJUnitRunner.class)
public class TestVSequenceUtil {

    // These names are from the test XML files in src/test/resources
    private static final String DELEGATOR_NAME = "default";
    private static final String HELPER_NAME ="defaultDS";

    private static final String SEQUENCE_NAME_FIELD = "seqName";
    private static final String SEQUENCE_ID_FIELD = "seqId";

    private static final String SEQUENCE_ENTITY = "SequenceValueItem";

    @Mock
    private ModelEntity mockModeLEntity;
    @Mock
    private ModelField mockNameField;
    @Mock
    private ModelField mockIdField;

    private VSequenceUtil vSequenceUtil;
    private GenericDelegator genericDelegator;

    @Before
    public void setup() throws Exception{
        GenericDelegator.removeGenericDelegator(DELEGATOR_NAME);
        GenericDelegator.unlock();
        genericDelegator = getGenericDelegator(DELEGATOR_NAME);
        resetDatabase();
        when(mockModeLEntity.getField(SEQUENCE_NAME_FIELD)).thenReturn(mockNameField);
        when(mockModeLEntity.getField(SEQUENCE_ID_FIELD)).thenReturn(mockIdField);
        when(mockModeLEntity.getTableName(HELPER_NAME)).thenReturn("SEQUENCE_VALUE_ITEM");
        when(mockNameField.getColName()).thenReturn("SEQ_NAME");
        when(mockIdField.getColName()).thenReturn("SEQ_ID");


        vSequenceUtil = new VSequenceUtil(HELPER_NAME, mockModeLEntity, SEQUENCE_NAME_FIELD, SEQUENCE_ID_FIELD);
    }

    private void resetDatabase() throws Exception {
        genericDelegator.removeByCondition(SEQUENCE_ENTITY, null);
    }

    @Test
    public void testGetNextIdReturns10000ForEmptySequences() {
        assertThat(vSequenceUtil.getNextSeqId("issue"), equalTo(10000L));
    }

    @Test
    public void testGetNextIdsReturnsCorrectAmount() {
        List<Long> ids = vSequenceUtil.getNextSeqIds("issue", 4);
        assertThat(ids, contains(10000L, 10001L, 10002L, 10003L));
    }

    @Test
    public void testGetNextIdIncrements() {
        vSequenceUtil.getNextSeqId("issue");
        assertThat(vSequenceUtil.getNextSeqId("issue"), equalTo(10001L));
    }

    @Test
    public void testMultiThreadedBehaviour()  throws Exception{
        final List<Long> sequenceIds = Lists.newArrayListWithCapacity(50);
        final List<Long> expected = LongStream.range(10000L, 10050L).boxed().collect(Collectors.toList());
        final CountDownLatch doneLatch = new CountDownLatch(50);
        final CountDownLatch startLatch = new CountDownLatch(1);
        expected.stream().forEach( n -> {
            new Thread(() -> {
                try {
                    startLatch.await();
                    sequenceIds.add(vSequenceUtil.getNextSeqId("issue"));
                    doneLatch.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        });
        startLatch.countDown();;
        doneLatch.await();
        assertThat(sequenceIds, containsInAnyOrder(expected.toArray()));
    }
}
