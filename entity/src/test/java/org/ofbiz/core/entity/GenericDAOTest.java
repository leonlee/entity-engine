package org.ofbiz.core.entity;

import com.google.common.collect.ImmutableList;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class GenericDAOTest {

    @Test
    public void testCorrectlyDetectsExpressionToBeRewritten() throws Exception {
        assertTrue(GenericDAO.conditionContainsInClauseWithListOfSizeGreaterThanMaxSize(new EntityExpr("test", EntityOperator.IN, ImmutableList.of(1, 2)), 1));
        assertFalse(GenericDAO.conditionContainsInClauseWithListOfSizeGreaterThanMaxSize(new EntityExpr("test", EntityOperator.IN, ImmutableList.of(1, 2)), 5));
        assertFalse(GenericDAO.conditionContainsInClauseWithListOfSizeGreaterThanMaxSize(new EntityExpr("test", EntityOperator.AND, ImmutableList.of(1, 2)), 1));
    }

    @Test
    public void testCorrectlyRewritesQuery() throws Exception {
        EntityCondition result = GenericDAO.transformConditionSplittingInClauseListsToChunksNoLongerThanMaxSize(new EntityExpr("test", EntityOperator.IN, ImmutableList.of(1, 2, 3, 4, 5)), 2);

        assertThat(result, instanceOf(EntityExprList.class));
        assertThat(((EntityExprList) result).getExprListSize(), equalTo(3));
        assertThat(((EntityExprList) result).getOperator(), equalTo(EntityOperator.OR));
        final List<EntityExpr> exprList = ImmutableList.copyOf(((EntityExprList) result).getExprIterator());
        assertThat(exprList, contains(entityExpr("test", EntityOperator.IN, ImmutableList.of(1, 2)), entityExpr("test", EntityOperator.IN, ImmutableList.of(3, 4)), entityExpr("test", EntityOperator.IN, ImmutableList.of(5))));
    }

    @Test
    public void testCorrectlyRewritesQueryWithoutModification() throws Exception {
        EntityCondition result = GenericDAO.transformConditionSplittingInClauseListsToChunksNoLongerThanMaxSize(new EntityExpr("test", EntityOperator.IN, ImmutableList.of(1, 2, 3, 4, 5)), 10);

        assertThat(result, instanceOf(EntityExpr.class));
        assertThat((EntityExpr) result, entityExpr("test", EntityOperator.IN, ImmutableList.of(1, 2, 3, 4, 5)));
    }

    private Matcher<EntityExpr> entityExpr(final String lhs, final EntityOperator operator, final ImmutableList<Integer> rhs) {
        return new BaseMatcher<EntityExpr>() {
            public boolean matches(Object o) {
                return o instanceof EntityExpr && ((EntityExpr) o).getLhs().equals(lhs) && ((EntityExpr) o).getOperator().equals(operator) && Matchers.contains(rhs.toArray()).matches(((EntityExpr) o).getRhs());
            }

            public void describeTo(Description description) {
                description.appendText(new EntityExpr(lhs, operator, rhs).toString());
            }
        };
    }
}
