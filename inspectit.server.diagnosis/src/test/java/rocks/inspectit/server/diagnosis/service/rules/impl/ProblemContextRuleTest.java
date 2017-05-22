package rocks.inspectit.server.diagnosis.service.rules.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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

public class ProblemContextRuleTest extends TestBase {

	@InjectMocks
	ProblemContextRule problemContextRule;

	@Mock
	InvocationSequenceData globalContext;

	@Mock
	AggregatedInvocationSequenceData timeWastingOperation;


	public static class Action extends ProblemContextRuleTest {
		private static final long METHOD_IDENT = 108L;
		private static final Random RANDOM = new Random();
		private static final Timestamp DEF_DATE = new Timestamp(new Date().getTime());
		private static final long PLATFORM_IDENT = RANDOM.nextLong();
		private static final long SENSOR_TYPE_IDENT = RANDOM.nextLong();
		private static final Timestamp CURRENT_TIME = new Timestamp(System.currentTimeMillis());

		private InvocationSequenceData parentSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
		private InvocationSequenceData childSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);

		@Test
		public void problemContextMustBeTheSameInvocationIfItIsTheOnlyOneAndIsTheInvoker() {
			List<InvocationSequenceData> rawInvocations = new ArrayList<InvocationSequenceData>();
			parentSequence.getNestedSequences().add(childSequence);
			rawInvocations.add(parentSequence);
			when(timeWastingOperation.getRawInvocationsSequenceElements()).thenReturn(rawInvocations);

			CauseCluster problemContext = problemContextRule.action();

			assertThat("The returned problemContext must be the invoker", problemContext.getCommonContext(), is(parentSequence));
		}

		@Test
		public void problemContextMustBeTheProperInvocationIfThereOneAndIsTheInvokerWithAParentSequence() {
			List<InvocationSequenceData> rawInvocations = new ArrayList<InvocationSequenceData>();
			childSequence.setParentSequence(parentSequence);
			rawInvocations.add(childSequence);
			when(timeWastingOperation.getRawInvocationsSequenceElements()).thenReturn(rawInvocations);

			CauseCluster problemContext = problemContextRule.action();

			assertThat("The returned problemContext must be the invoker", problemContext.getCommonContext(), is(childSequence.getParentSequence()));
		}

		@Test
		public void problemContextMustBeTheMostSignificantClusterContext() {
			List<InvocationSequenceData> rawInvocations = new ArrayList<InvocationSequenceData>();
			TimerData timerData = new TimerData(CURRENT_TIME, 10L, 20L, 30L);
			timerData.calculateExclusiveMin(RANDOM.nextDouble());
			timerData.setExclusiveDuration(2000d);
			globalContext.setTimerData(timerData);
			InvocationSequenceData firstSeqWithParent = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			firstSeqWithParent.setTimerData(timerData);
			firstSeqWithParent.setParentSequence(globalContext);
			globalContext.getNestedSequences().add(firstSeqWithParent);
			InvocationSequenceData significantContext = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			timerData.setExclusiveDuration(8000d);
			significantContext.setTimerData(timerData);
			significantContext.setParentSequence(globalContext);
			InvocationSequenceData significantContextChildWithParent = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			timerData.setExclusiveDuration(4000d);
			significantContextChildWithParent.setTimerData(timerData);
			significantContextChildWithParent.setParentSequence(significantContext);
			significantContext.getNestedSequences().add(significantContextChildWithParent);
			InvocationSequenceData secondSignificantContextChildWithParent = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			secondSignificantContextChildWithParent.setTimerData(timerData);
			secondSignificantContextChildWithParent.setParentSequence(significantContext);
			significantContext.getNestedSequences().add(secondSignificantContextChildWithParent);
			InvocationSequenceData secondRawInvocation = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			timerData.setExclusiveDuration(2000d);
			secondRawInvocation.setTimerData(timerData);
			secondRawInvocation.setParentSequence(globalContext);
			InvocationSequenceData secondSeqWithParent = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			secondSeqWithParent.setParentSequence(secondRawInvocation);
			secondRawInvocation.getNestedSequences().add(secondSeqWithParent);
			rawInvocations.add(firstSeqWithParent);
			rawInvocations.add(significantContext);
			rawInvocations.add(secondRawInvocation);
			List<InvocationSequenceData> rawInvocationsSignificant = new ArrayList<InvocationSequenceData>();
			rawInvocationsSignificant.add(significantContextChildWithParent);
			rawInvocationsSignificant.add(secondSignificantContextChildWithParent);
			when(timeWastingOperation.getRawInvocationsSequenceElements()).thenReturn(rawInvocationsSignificant);
			when(globalContext.getNestedSequences()).thenReturn(rawInvocations);

			CauseCluster problemContext = problemContextRule.action();

			assertThat("The returned problemContext must be the most significant cluster context", problemContext.getCommonContext(), is(significantContext));
		}

		@Test
		public void problemContextMustBeGlobalContext() {
			List<InvocationSequenceData> rawInvocations = new ArrayList<InvocationSequenceData>();
			TimerData timerData = new TimerData(CURRENT_TIME, 10L, 20L, 30L);
			timerData.calculateExclusiveMin(RANDOM.nextDouble());
			timerData.setExclusiveDuration(RANDOM.nextDouble());
			when(globalContext.getTimerData()).thenReturn(timerData);
			rawInvocations.add(globalContext);
			when(timeWastingOperation.getRawInvocationsSequenceElements()).thenReturn(rawInvocations);

			CauseCluster problemContext = problemContextRule.action();

			assertThat("The returned problemContext must be the most significant cluster context", problemContext.getCommonContext(), is(globalContext));
		}

		@Test
		public void problemContextMustBeTheMostSignificantClusterContextWithoutClustering() {
			double highDuration = RANDOM.nextDouble() + 1000;
			List<InvocationSequenceData> rawInvocations = new ArrayList<InvocationSequenceData>();
			TimerData timerData = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timerData.calculateExclusiveMin(RANDOM.nextDouble());
			timerData.setExclusiveDuration(RANDOM.nextDouble());
			InvocationSequenceData firstRawInvocation = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			firstRawInvocation.setTimerData(timerData);
			InvocationSequenceData firstSeqWithParent = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			firstSeqWithParent.setTimerData(timerData);
			firstSeqWithParent.setParentSequence(new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT));
			firstRawInvocation.getNestedSequences().add(firstSeqWithParent);
			InvocationSequenceData significantContext = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			TimerData significantContextTimerData = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			significantContextTimerData.calculateExclusiveMin(RANDOM.nextDouble());
			significantContextTimerData.setExclusiveDuration(highDuration);
			significantContext.setTimerData(significantContextTimerData);
			InvocationSequenceData significantContextChildWithParent = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			TimerData significantseqWithParentTimerData = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			significantseqWithParentTimerData.calculateExclusiveMin(RANDOM.nextDouble());
			significantseqWithParentTimerData.setExclusiveDuration(highDuration);
			significantContextChildWithParent.setTimerData(significantseqWithParentTimerData);
			significantContextChildWithParent.setParentSequence(new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT));
			significantContext.getNestedSequences().add(significantContextChildWithParent);
			InvocationSequenceData secondRawInvocation = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			secondRawInvocation.setTimerData(timerData);
			InvocationSequenceData secondSeqWithParent = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			secondSeqWithParent.setParentSequence(new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT));
			secondRawInvocation.getNestedSequences().add(secondSeqWithParent);
			rawInvocations.add(firstRawInvocation);
			rawInvocations.add(significantContext);
			rawInvocations.add(secondRawInvocation);
			when(timeWastingOperation.getRawInvocationsSequenceElements()).thenReturn(rawInvocations);

			CauseCluster problemContext = problemContextRule.action();

			assertThat("The returned problemContext must be the most significant cluster context", problemContext.getCommonContext(), is(significantContext));
		}
	}
}
