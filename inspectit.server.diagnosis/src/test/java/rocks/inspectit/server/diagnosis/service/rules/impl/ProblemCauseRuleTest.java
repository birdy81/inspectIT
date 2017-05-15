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
import rocks.inspectit.shared.all.communication.data.diagnosis.results.CauseCluster;
import rocks.inspectit.shared.all.testbase.TestBase;

public class ProblemCauseRuleTest extends TestBase {

	@InjectMocks
	ProblemCauseRule problemCauseRule;

	@Mock
	CauseCluster problemContext;

	@Mock
	InvocationSequenceData commonContext;

	public static class Action extends ProblemCauseRuleTest {

		private static final Random RANDOM = new Random();
		private static final Timestamp DEF_DATE = new Timestamp(new Date().getTime());
		private static final long METHOD_IDENT_EQUAL = 108L;
		private static final long PLATFORM_IDENT = RANDOM.nextLong();
		private static final long SENSOR_TYPE_IDENT = RANDOM.nextLong();
		private static final double HIGH_DURATION = RANDOM.nextDouble() + 1000;

		@Test
		public void rootCauseMustBeNotNullWhenMethodIdentIsEqualAndTheInvocationHasTimerData() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<InvocationSequenceData>();
			InvocationSequenceData firstSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			TimerData timerData = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timerData.calculateExclusiveMin(RANDOM.nextDouble());
			timerData.setExclusiveDuration(RANDOM.nextDouble());
			firstSequenceData.setTimerData(timerData);
			InvocationSequenceData secondSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			TimerData secondTimerData = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			secondTimerData.calculateExclusiveMin(RANDOM.nextDouble());
			secondTimerData.setExclusiveDuration(RANDOM.nextDouble());
			secondSequenceData.setTimerData(secondTimerData);
			nestedSequences.add(firstSequenceData);
			nestedSequences.add(secondSequenceData);
			when(problemContext.getCommonContext()).thenReturn(commonContext);
			when(commonContext.getMethodIdent()).thenReturn(1L);
			when(commonContext.getDuration()).thenReturn(HIGH_DURATION);
			when(problemContext.getCauseInvocations()).thenReturn(nestedSequences);

			AggregatedInvocationSequenceData rootCause = problemCauseRule.action();

			assertThat("The returned root cause rule must not be null", rootCause, notNullValue());
		}

		@Test
		public void rootCauseMustBeNullWhenMethodIdentIsEqualAndInvocationHasNotTimerData() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<InvocationSequenceData>();
			nestedSequences.add(new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL));
			nestedSequences.add(new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL));
			when(problemContext.getCommonContext()).thenReturn(commonContext);
			when(problemContext.getCommonContext().getMethodIdent()).thenReturn(1L);
			when(problemContext.getCommonContext().getDuration()).thenReturn(0.0);
			when(problemContext.getCauseInvocations()).thenReturn(nestedSequences);

			AggregatedInvocationSequenceData rootCause = problemCauseRule.action();

			assertThat("The returned root cause rule must be null", rootCause, nullValue());
		}

		@Test
		public void rootCauseMustHaveTwoElementsInRawInvocationSequence() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<InvocationSequenceData>();
			InvocationSequenceData firstSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			TimerData firstTimerData = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			firstTimerData.setExclusiveDuration(2000);
			firstTimerData.setDuration(2000);
			firstSequenceData.setTimerData(firstTimerData);
			InvocationSequenceData secondSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			TimerData secondTimerData = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			secondTimerData.setExclusiveDuration(1000);
			secondTimerData.setDuration(1000);
			secondSequenceData.setTimerData(secondTimerData);
			InvocationSequenceData thirdSequenceData = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			TimerData thirdTimerData = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			thirdTimerData.setExclusiveDuration(100);
			thirdTimerData.setDuration(100);
			thirdSequenceData.setTimerData(thirdTimerData);

			nestedSequences.add(firstSequenceData);
			nestedSequences.add(secondSequenceData);
			nestedSequences.add(thirdSequenceData);

			when(problemContext.getCommonContext()).thenReturn(commonContext);
			when(commonContext.getMethodIdent()).thenReturn(METHOD_IDENT_EQUAL);
			when(commonContext.getDuration()).thenReturn(3100.0);
			when(problemContext.getCauseInvocations()).thenReturn(nestedSequences);

			AggregatedInvocationSequenceData rootCause = problemCauseRule.action();

			assertThat("Raw invocation sequence must have two elements", rootCause.getRawInvocationsSequenceElements(), hasSize(3));
		}
	}

}
