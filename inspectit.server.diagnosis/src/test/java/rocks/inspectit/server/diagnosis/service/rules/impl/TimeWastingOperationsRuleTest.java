package rocks.inspectit.server.diagnosis.service.rules.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import rocks.inspectit.shared.all.communication.data.AggregatedInvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.SqlStatementData;
import rocks.inspectit.shared.all.communication.data.TimerData;
import rocks.inspectit.shared.all.testbase.TestBase;

/**
 * @author Isabel Vico Peinado
 */
@SuppressWarnings("PMD")
public class TimeWastingOperationsRuleTest extends TestBase {

	@InjectMocks
	TimeWastingOperationsRule timeWastingOperationsRule;

	@Mock
	InvocationSequenceData globalContext;

	public static class Action extends TimeWastingOperationsRuleTest {

		private static final double BASELINE = 1000d;
		private static final Random RANDOM = new Random();
		private static final Double HIGH_DURATION = RANDOM.nextDouble() + 1000;
		private static final Timestamp DEF_DATE = new Timestamp(new Date().getTime());
		private static final TimerData TIMER_DATA = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
		private static final long PLATFORM_IDENT = RANDOM.nextLong();
		private static final long SENSOR_TYPE_IDENT = RANDOM.nextLong();

		@BeforeMethod
		private void init() {
			try {
				Field field = TimeWastingOperationsRule.class.getDeclaredField("baseline");
				field.setAccessible(true);
				field.set(timeWastingOperationsRule, BASELINE);
			} catch (NoSuchFieldException | IllegalArgumentException | SecurityException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Checks that the sequenceData have all the mandatory attributes.
		 *
		 * @param aggregatedInvocationSequenceData
		 *            Sequence data to check if has all the mandatory data.
		 */
		private void isAValidRule(AggregatedInvocationSequenceData aggregatedInvocationSequenceData) {
			for (InvocationSequenceData aggregatedSequence : aggregatedInvocationSequenceData.getRawInvocationsSequenceElements()) {
				assertThat("The aggregated sequence cannot be null", aggregatedSequence, notNullValue());
				assertThat("Duration of the aggregated sequence cannot be null", aggregatedSequence.getDuration(), notNullValue());
				assertThat("Start time of the aggregated cannot be null", aggregatedSequence.getStart(), notNullValue());
				assertThat("End time of the aggregated cannot be null", aggregatedSequence.getEnd(), notNullValue());
				assertThat("Child count of the aggregated sequence cannot be null", aggregatedSequence.getChildCount(), notNullValue());
				assertThat("ApplicationId of the aggregated sequence cannot be null", aggregatedSequence.getApplicationId(), notNullValue());
				assertThat("Business transaction id of the aggregated sequence cannot be null", aggregatedSequence.getBusinessTransactionId(), notNullValue());
			}
		}

		/**
		 * Tests that the action method of the rule is not returning a null group of rules.
		 */
		@Test
		public void timerDataMustReturnANotNullGroupOfRules() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<>();
			InvocationSequenceData firstSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 1L);
			TimerData firstSeqTimerData = TIMER_DATA;
			firstSeqTimerData.calculateExclusiveMin(RANDOM.nextDouble());
			firstSequence.setTimerData(firstSeqTimerData);
			InvocationSequenceData secondSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 2L);
			nestedSequences.add(firstSequence);
			nestedSequences.add(secondSequence);
			when(globalContext.getDuration()).thenReturn(HIGH_DURATION);
			when(globalContext.getNestedSequences()).thenReturn(nestedSequences);

			List<AggregatedInvocationSequenceData> timeWastingOperationsResults = timeWastingOperationsRule.action();

			assertThat("The returned list of rules must not be null", timeWastingOperationsResults, notNullValue());
		}

		/**
		 * Tests that the action method of the rule is not returning an empty group of rules.
		 */
		@Test
		public void timerDataMustReturnANotEmptyGroupOfRulesWhenTheDurationIsTooLong() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<>();
			InvocationSequenceData firstSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 1L);
			TimerData firstSeqTimerData = TIMER_DATA;
			firstSeqTimerData.calculateExclusiveMin(RANDOM.nextDouble());
			firstSequence.setTimerData(firstSeqTimerData);
			InvocationSequenceData secondSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 2L);
			nestedSequences.add(firstSequence);
			nestedSequences.add(secondSequence);
			when(globalContext.getDuration()).thenReturn(HIGH_DURATION);
			when(globalContext.getNestedSequences()).thenReturn(nestedSequences);

			List<AggregatedInvocationSequenceData> timeWastingOperationsResults = timeWastingOperationsRule.action();

			assertThat("Action method must return a list of rules not empty", timeWastingOperationsResults, not(hasSize(0)));
		}

		/**
		 * Tests that the action method of the rule is returning an empty group of rules since the
		 * duration is too short.
		 */
		@Test
		public void timerDataMustReturnAEmptyGroupOfRulesWhenTheDurationIsZero() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<>();
			InvocationSequenceData firstSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 1L);
			TimerData firstSeqTimerData = TIMER_DATA;
			firstSeqTimerData.calculateExclusiveMin(RANDOM.nextDouble());
			firstSequence.setTimerData(firstSeqTimerData);
			InvocationSequenceData secondSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 2L);
			nestedSequences.add(firstSequence);
			nestedSequences.add(secondSequence);
			when(globalContext.getDuration()).thenReturn(new Double(0));
			when(globalContext.getNestedSequences()).thenReturn(nestedSequences);

			List<AggregatedInvocationSequenceData> timeWastingOperationsResults = timeWastingOperationsRule.action();

			assertThat("Action method must return an array of rules not empty", timeWastingOperationsResults, hasSize(0));
		}

		/**
		 * Tests that the action method of the rule is not returning the correct number of elements.
		 */
		@Test
		public void timerDataMustReturnAGroupOfRulesWithOneElement() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<>();
			InvocationSequenceData firstSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 1L);
			TimerData firstSeqTimerData = TIMER_DATA;
			firstSeqTimerData.calculateExclusiveMin(RANDOM.nextDouble());
			firstSequence.setTimerData(firstSeqTimerData);
			InvocationSequenceData secondSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 2L);
			nestedSequences.add(firstSequence);
			nestedSequences.add(secondSequence);
			when(globalContext.getDuration()).thenReturn(HIGH_DURATION);
			when(globalContext.getNestedSequences()).thenReturn(nestedSequences);

			List<AggregatedInvocationSequenceData> timeWastingOperationsResults = timeWastingOperationsRule.action();

			assertThat("Action method must return a list of one rule", timeWastingOperationsResults, hasSize(1));
		}

		/**
		 * Tests that the action method of the rule is not returning the correct number of elements.
		 */
		@Test
		public void timerDataMustReturnAGroupOfRulesWithTwoElements() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<>();
			InvocationSequenceData firstSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 1L);
			TimerData firstSeqTimerData = TIMER_DATA;
			firstSeqTimerData.calculateExclusiveMin(RANDOM.nextDouble());
			firstSequence.setTimerData(firstSeqTimerData);
			InvocationSequenceData secondSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 2L);
			InvocationSequenceData thirdSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 3L);
			TimerData thirdTimerData = TIMER_DATA;
			thirdTimerData.calculateExclusiveMin(RANDOM.nextDouble());
			thirdSequence.setTimerData(thirdTimerData);
			nestedSequences.add(firstSequence);
			nestedSequences.add(secondSequence);
			nestedSequences.add(thirdSequence);
			when(globalContext.getDuration()).thenReturn(HIGH_DURATION);
			when(globalContext.getNestedSequences()).thenReturn(nestedSequences);

			List<AggregatedInvocationSequenceData> timeWastingOperationsResults = timeWastingOperationsRule.action();

			assertThat("Action method must return a list with two rules", timeWastingOperationsResults, hasSize(2));
		}

		/**
		 * Tests that the action method of the rule is not returning a valid group of rules.
		 */
		@Test
		public void timerDataMustReturnAValidGroupOfRules() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<>();
			InvocationSequenceData firstSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 1L);
			TimerData firstSeqTimerData = TIMER_DATA;
			firstSeqTimerData.calculateExclusiveMin(RANDOM.nextDouble());
			firstSequence.setTimerData(firstSeqTimerData);
			nestedSequences.add(firstSequence);
			InvocationSequenceData secondSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 2L);
			nestedSequences.add(secondSequence);
			when(globalContext.getDuration()).thenReturn(HIGH_DURATION);
			when(globalContext.getNestedSequences()).thenReturn(nestedSequences);

			List<AggregatedInvocationSequenceData> timeWastingOperations = timeWastingOperationsRule.action();

			assertThat("Action method must return a list of rules not empty", timeWastingOperations, not(hasSize(0)));
			for (AggregatedInvocationSequenceData aggregatedInvocationSequenceData : timeWastingOperations) {
				isAValidRule(aggregatedInvocationSequenceData);
			}
		}

		/**
		 * Tests that checks that the results are the expected.
		 */
		@Test
		public void timerDataMustReturnTheExpectedRules() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<>();
			InvocationSequenceData firstSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 1L);
			TimerData firstSeqTimerData = TIMER_DATA;
			firstSeqTimerData.calculateExclusiveMin(RANDOM.nextDouble());
			firstSequence.setTimerData(firstSeqTimerData);
			InvocationSequenceData secondSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 2L);
			InvocationSequenceData thirdSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 3L);
			TimerData thirdSeqTimerData = TIMER_DATA;
			thirdSeqTimerData.calculateExclusiveMin(RANDOM.nextDouble());
			thirdSequence.setTimerData(thirdSeqTimerData);
			nestedSequences.add(firstSequence);
			nestedSequences.add(secondSequence);
			nestedSequences.add(thirdSequence);
			when(globalContext.getDuration()).thenReturn(HIGH_DURATION);
			when(globalContext.getNestedSequences()).thenReturn(nestedSequences);

			List<AggregatedInvocationSequenceData> timeWastingOperationsResults = timeWastingOperationsRule.action();

			assertThat("Identifier is not the expected one, the first result must have 1 as method identifier", timeWastingOperationsResults.get(0).getMethodIdent(), is(1L));
			assertThat("Identifier is not the expected one, the first result must have 3 as method identifier", timeWastingOperationsResults.get(1).getMethodIdent(), is(3L));
		}

		/**
		 * Tests that the action method of the rule is not returning a null group of rules.
		 */
		@Test
		public void sqlStatementDataMustReturnANotNullGroupOfRules() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<>();
			InvocationSequenceData firstsequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 1L);
			SqlStatementData firstSqlStatementData = new SqlStatementData();
			firstSqlStatementData.calculateExclusiveMin(RANDOM.nextDouble());
			firstsequence.setSqlStatementData(firstSqlStatementData);
			InvocationSequenceData secondSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 2L);
			nestedSequences.add(firstsequence);
			nestedSequences.add(secondSequenceData);
			when(globalContext.getDuration()).thenReturn(HIGH_DURATION);
			when(globalContext.getNestedSequences()).thenReturn(nestedSequences);

			List<AggregatedInvocationSequenceData> timeWastingOperationsResults = timeWastingOperationsRule.action();

			assertThat("The returned list of rules must not be null", timeWastingOperationsResults, notNullValue());
		}

		/**
		 * Tests that the action method of the rule is not returning an empty group of rules.
		 */
		@Test
		public void sqlStatementDataMustReturnANotEmptyGroupOfRulesWhenTheDurationIsTooLong() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<>();
			InvocationSequenceData firstsequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 1L);
			SqlStatementData firstSqlStatementData = new SqlStatementData();
			firstSqlStatementData.calculateExclusiveMin(RANDOM.nextDouble());
			firstsequence.setSqlStatementData(firstSqlStatementData);
			InvocationSequenceData secondSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 2L);
			nestedSequences.add(firstsequence);
			nestedSequences.add(secondSequenceData);
			when(globalContext.getDuration()).thenReturn(HIGH_DURATION);
			when(globalContext.getNestedSequences()).thenReturn(nestedSequences);

			List<AggregatedInvocationSequenceData> timeWastingOperationsResults = timeWastingOperationsRule.action();

			assertThat("Action method must return a list of rules not empty", timeWastingOperationsResults, not(hasSize(0)));
		}

		/**
		 * Tests that when the duration is zero the result list must be empty.
		 */
		@Test
		public void sqlStatementDataMustReturnAEmptyGroupOfRulesWhenTheDurationIsZero() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<>();
			InvocationSequenceData firstsequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 1L);
			SqlStatementData firstSqlStatementData = new SqlStatementData();
			firstSqlStatementData.calculateExclusiveMin(RANDOM.nextDouble());
			firstsequence.setSqlStatementData(firstSqlStatementData);
			nestedSequences.add(firstsequence);
			InvocationSequenceData secondSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 2L);
			nestedSequences.add(secondSequenceData);
			when(globalContext.getDuration()).thenReturn(new Double(0));
			when(globalContext.getNestedSequences()).thenReturn(nestedSequences);

			List<AggregatedInvocationSequenceData> timeWastingOperationsResults = timeWastingOperationsRule.action();

			assertThat("Action method must return an array of rules not empty", timeWastingOperationsResults, hasSize(0));
		}

		/**
		 * Tests that the action method of the rule is not returning the correct number of elements.
		 */
		@Test
		public void sqlStatementDataMustReturnAGroupOfRulesWithOneElement() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<>();
			InvocationSequenceData firstsequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 1L);
			SqlStatementData firstSqlStatementData = new SqlStatementData();
			firstSqlStatementData.calculateExclusiveMin(RANDOM.nextDouble());
			firstsequence.setSqlStatementData(firstSqlStatementData);
			InvocationSequenceData secondSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 2L);
			nestedSequences.add(firstsequence);
			nestedSequences.add(secondSequenceData);
			when(globalContext.getDuration()).thenReturn(HIGH_DURATION);
			when(globalContext.getNestedSequences()).thenReturn(nestedSequences);

			List<AggregatedInvocationSequenceData> timeWastingOperationsResults = timeWastingOperationsRule.action();

			assertThat("Action method must return a list of one rule", timeWastingOperationsResults, hasSize(1));
		}

		/**
		 * Tests that the action method of the rule is not returning the correct number of elements.
		 */
		@Test
		public void sqlStatementDataMustReturnAGroupOfRulesWithTwoElements() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<>();
			InvocationSequenceData firstsequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 1L);
			SqlStatementData firstSqlStatementData = new SqlStatementData();
			firstSqlStatementData.calculateExclusiveMin(RANDOM.nextDouble());
			firstsequence.setSqlStatementData(firstSqlStatementData);
			InvocationSequenceData secondSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 2L);
			InvocationSequenceData thirdSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 3L);
			SqlStatementData thirdSqlStatementData = new SqlStatementData();
			thirdSqlStatementData.calculateExclusiveMin(RANDOM.nextDouble());
			secondSequenceData.setSqlStatementData(thirdSqlStatementData);
			nestedSequences.add(firstsequence);
			nestedSequences.add(secondSequenceData);
			nestedSequences.add(thirdSequenceData);
			when(globalContext.getDuration()).thenReturn(HIGH_DURATION);
			when(globalContext.getNestedSequences()).thenReturn(nestedSequences);

			List<AggregatedInvocationSequenceData> timeWastingOperationsResults = timeWastingOperationsRule.action();

			assertThat("Action method must return a list with two rules", timeWastingOperationsResults, hasSize(2));
		}

		/**
		 * Tests that the action method of the rule is not returning a valid group of rules.
		 */
		@Test
		public void sqlStatementDataMustReturnAValidGroupOfRules() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<>();
			InvocationSequenceData firstsequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 1L);
			SqlStatementData firstSqlStatementData = new SqlStatementData();
			firstSqlStatementData.calculateExclusiveMin(RANDOM.nextDouble());
			firstsequence.setSqlStatementData(firstSqlStatementData);
			InvocationSequenceData secondSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 2L);
			nestedSequences.add(firstsequence);
			nestedSequences.add(secondSequenceData);
			when(globalContext.getDuration()).thenReturn(HIGH_DURATION);
			when(globalContext.getNestedSequences()).thenReturn(nestedSequences);

			List<AggregatedInvocationSequenceData> timeWastingOperationsResults = timeWastingOperationsRule.action();

			assertThat("Action method must return a list of rules not empty", timeWastingOperationsResults, not(hasSize(0)));
			for (AggregatedInvocationSequenceData aggregatedInvocationSequenceData : timeWastingOperationsResults) {
				isAValidRule(aggregatedInvocationSequenceData);
			}
		}

		/**
		 * Tests that checks that the results are the expected.
		 */
		@Test
		public void sqlStatementDataMustReturnTheExpectedRules() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<>();
			InvocationSequenceData firstSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 1L);
			SqlStatementData firstSqlStatementData = new SqlStatementData();
			firstSqlStatementData.calculateExclusiveMin(RANDOM.nextDouble());
			firstSequence.setSqlStatementData(firstSqlStatementData);
			InvocationSequenceData secondSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 2L);
			InvocationSequenceData thirdSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 3L);
			SqlStatementData thirdSqlStatementData = new SqlStatementData();
			thirdSqlStatementData.calculateExclusiveMin(RANDOM.nextDouble());
			thirdSequenceData.setSqlStatementData(thirdSqlStatementData);
			nestedSequences.add(firstSequence);
			nestedSequences.add(secondSequenceData);
			nestedSequences.add(thirdSequenceData);
			when(globalContext.getDuration()).thenReturn(HIGH_DURATION);
			when(globalContext.getNestedSequences()).thenReturn(nestedSequences);

			List<AggregatedInvocationSequenceData> timeWastingOperationsResults = timeWastingOperationsRule.action();

			assertThat("Identifier is not the expected one, the first result must have 1 as method identifier", timeWastingOperationsResults.get(0).getMethodIdent(), is(1L));
			assertThat("Identifier is not the expected one, the first result must have 3 as method identifier", timeWastingOperationsResults.get(1).getMethodIdent(), is(3L));
		}
	}

}
