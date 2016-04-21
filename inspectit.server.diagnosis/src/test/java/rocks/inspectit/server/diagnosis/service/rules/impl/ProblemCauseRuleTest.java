package rocks.inspectit.server.diagnosis.service.rules.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.Test;

import rocks.inspectit.shared.all.communication.data.AggregatedInvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.TimerData;
import rocks.inspectit.shared.all.testbase.TestBase;

public class ProblemCauseRuleTest extends TestBase {

	@InjectMocks
	ProblemCauseRule problemCauseRule;

	@Mock
	AggregatedInvocationSequenceData timeWastingOperation;

	@Mock
	InvocationSequenceData problemContext;

	public static class Action extends ProblemCauseRuleTest {

		private static final Random RANDOM = new Random();
		private static final Timestamp DEF_DATE = new Timestamp(new Date().getTime());
		private static final long METHOD_IDENT_EQUAL = 108L;
		private static final long METHOD_IDENT_DIFF = 2L;
		private static final TimerData TIMER_DATA = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
		private static final long PLATFORM_IDENT = RANDOM.nextLong();
		private static final long SENSOR_TYPE_IDENT = RANDOM.nextLong();
		private static final double HIGH_DURATION = RANDOM.nextDouble() + 1000;

		@Test
		public void rootCauseMustBeNotNullWhenMethodIdentIsEqualAndTheInvocationHasTimerData() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<InvocationSequenceData>();
			InvocationSequenceData firstSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			TimerData timerData = TIMER_DATA;
			timerData.calculateExclusiveMin(RANDOM.nextDouble());
			timerData.setExclusiveDuration(RANDOM.nextDouble());
			firstSequenceData.setTimerData(timerData);
			InvocationSequenceData secondSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			nestedSequences.add(firstSequenceData);
			nestedSequences.add(secondSequenceData);
			when(problemContext.getMethodIdent()).thenReturn(1L);
			when(problemContext.getDuration()).thenReturn(HIGH_DURATION);
			when(problemContext.getNestedSequences()).thenReturn(nestedSequences);
			when(timeWastingOperation.getMethodIdent()).thenReturn(METHOD_IDENT_EQUAL);

			AggregatedInvocationSequenceData rootCause = problemCauseRule.action();

			assertThat("The returned root cause rule must not be null", rootCause, notNullValue());
		}

		@Test
		public void rootCauseMustBeNullWhenMethodIdentIsEqualAndInvocationHasNotTimerData() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<InvocationSequenceData>();
			nestedSequences.add(new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL));
			nestedSequences.add(new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF));
			when(problemContext.getMethodIdent()).thenReturn(1L);
			when(problemContext.getDuration()).thenReturn(HIGH_DURATION);
			when(problemContext.getNestedSequences()).thenReturn(nestedSequences);
			when(timeWastingOperation.getMethodIdent()).thenReturn(METHOD_IDENT_EQUAL);

			AggregatedInvocationSequenceData rootCause = problemCauseRule.action();

			assertThat("The returned root cause rule must be null", rootCause, nullValue());
		}

		@Test
		public void rootCauseMustBeNullWhenMethodIdentIsNotEqualAndTheInvocationHasTimerData() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<InvocationSequenceData>();
			InvocationSequenceData firstSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			TimerData timerData = TIMER_DATA;
			timerData.calculateExclusiveMin(RANDOM.nextDouble());
			timerData.setExclusiveDuration(RANDOM.nextDouble());
			firstSequenceData.setTimerData(timerData);
			InvocationSequenceData secondSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			nestedSequences.add(firstSequenceData);
			nestedSequences.add(secondSequenceData);
			when(problemContext.getMethodIdent()).thenReturn(1L);
			when(problemContext.getDuration()).thenReturn(HIGH_DURATION);
			when(problemContext.getNestedSequences()).thenReturn(nestedSequences);
			when(timeWastingOperation.getMethodIdent()).thenReturn(RANDOM.nextLong());

			AggregatedInvocationSequenceData rootCause = problemCauseRule.action();

			assertThat("The returned root cause rule must be null", rootCause, nullValue());
		}

		@Test
		public void rootCauseMustBeNullWhenMethodIdentIsNotEqualAndInvocationHasNotTimerData() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<InvocationSequenceData>();
			nestedSequences.add(new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL));
			nestedSequences.add(new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF));
			when(problemContext.getMethodIdent()).thenReturn(1L);
			when(problemContext.getDuration()).thenReturn(HIGH_DURATION);
			when(problemContext.getNestedSequences()).thenReturn(nestedSequences);
			when(timeWastingOperation.getMethodIdent()).thenReturn(RANDOM.nextLong());

			AggregatedInvocationSequenceData rootCause = problemCauseRule.action();

			assertThat("The returned root cause rule must be null", rootCause, nullValue());
		}

		@Test
		public void rootCauseMustHaveOneElementInRawInvocationSequence() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<InvocationSequenceData>();
			InvocationSequenceData firstSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			TimerData timerData = TIMER_DATA;
			timerData.calculateExclusiveMin(RANDOM.nextDouble());
			timerData.setExclusiveDuration(RANDOM.nextDouble());
			firstSequenceData.setTimerData(timerData);
			InvocationSequenceData secondSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			nestedSequences.add(firstSequenceData);
			nestedSequences.add(secondSequenceData);
			when(problemContext.getMethodIdent()).thenReturn(1L);
			when(problemContext.getDuration()).thenReturn(HIGH_DURATION);
			when(problemContext.getNestedSequences()).thenReturn(nestedSequences);
			when(timeWastingOperation.getMethodIdent()).thenReturn(METHOD_IDENT_EQUAL);

			AggregatedInvocationSequenceData rootCause = problemCauseRule.action();

			assertThat("Raw invocation sequence must have one element", rootCause.getRawInvocationsSequenceElements(), hasSize(1));
		}

		@Test
		public void rootCauseMustHaveTwoElementsInRawInvocationSequence() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<InvocationSequenceData>();
			InvocationSequenceData firstSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			TimerData firstTimerData = TIMER_DATA;
			firstTimerData.calculateExclusiveMin(RANDOM.nextDouble());
			firstTimerData.setExclusiveDuration(RANDOM.nextDouble());
			firstSequenceData.setTimerData(firstTimerData);
			InvocationSequenceData secondSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			InvocationSequenceData thirdSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			TimerData secondTimerData = TIMER_DATA;
			secondTimerData.calculateExclusiveMin(RANDOM.nextDouble());
			secondTimerData.setExclusiveDuration(RANDOM.nextDouble());
			thirdSequenceData.setTimerData(secondTimerData);
			nestedSequences.add(firstSequenceData);
			nestedSequences.add(secondSequenceData);
			nestedSequences.add(thirdSequenceData);
			when(problemContext.getMethodIdent()).thenReturn(1L);
			when(problemContext.getDuration()).thenReturn(HIGH_DURATION);
			when(timeWastingOperation.getMethodIdent()).thenReturn(METHOD_IDENT_EQUAL);
			when(problemContext.getNestedSequences()).thenReturn(nestedSequences);

			AggregatedInvocationSequenceData rootCause = problemCauseRule.action();

			assertThat("Raw invocation sequence must have two elements", rootCause.getRawInvocationsSequenceElements(), hasSize(2));
		}
	}

}
