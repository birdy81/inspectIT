package rocks.inspectit.server.diagnosis.service.rules.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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

		private InvocationSequenceData parentSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
		private InvocationSequenceData childSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);

		@Test
		public void problemContextMustBeTheSameInvocationIfItIsTheOnlyOneAndIsTheInvoker() {
			List<InvocationSequenceData> rawInvocations = new ArrayList<InvocationSequenceData>();
			parentSequence.getNestedSequences().add(childSequence);
			rawInvocations.add(parentSequence);
			when(timeWastingOperation.getRawInvocationsSequenceElements()).thenReturn(rawInvocations);

			InvocationSequenceData problemContext = problemContextRule.action();

			assertThat("The returned problemContext must be the invoker", problemContext, is(equalTo(parentSequence)));
		}

		@Test
		public void problemContextMustBeTheProperInvocationIfThereOneAndIsTheInvokerWithAParentSequence() {
			List<InvocationSequenceData> rawInvocations = new ArrayList<InvocationSequenceData>();
			childSequence.setParentSequence(parentSequence);
			rawInvocations.add(childSequence);
			when(timeWastingOperation.getRawInvocationsSequenceElements()).thenReturn(rawInvocations);

			InvocationSequenceData problemContext = problemContextRule.action();

			assertThat("The returned problemContext must be the invoker", problemContext, is(equalTo(childSequence.getParentSequence())));
		}

		@Test
		public void problemContextMustBeTheMostSignificantClusterContext() {
			List<InvocationSequenceData> rawInvocations = new ArrayList<InvocationSequenceData>();

			// InvocationSequenceData rootInvocation = new InvocationSequenceData(DEF_DATE,
			// PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);

			TimerData timerData = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timerData.calculateExclusiveMin(RANDOM.nextDouble());
			timerData.setExclusiveDuration(2000.0);
			globalContext.setTimerData(timerData);
			// globalContext.setParentSequence(rootInvocation);

			InvocationSequenceData firstSeqWithParent = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			TimerData timerData2 = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timerData2.calculateExclusiveMin(RANDOM.nextDouble());
			timerData2.setExclusiveDuration(2000.0);
			firstSeqWithParent.setTimerData(timerData2);
			firstSeqWithParent.setParentSequence(globalContext);
			globalContext.getNestedSequences().add(firstSeqWithParent);

			InvocationSequenceData significantContext = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			TimerData significantContextTimerData = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			significantContextTimerData.calculateExclusiveMin(RANDOM.nextDouble());
			significantContextTimerData.setExclusiveDuration(8000.0);
			significantContext.setTimerData(significantContextTimerData);
			significantContext.setParentSequence(globalContext);

			InvocationSequenceData significantContextChildWithParent = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			TimerData significantseqWithParentTimerData = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			significantseqWithParentTimerData.calculateExclusiveMin(RANDOM.nextDouble());
			significantseqWithParentTimerData.setExclusiveDuration(4000.0);
			significantContextChildWithParent.setTimerData(significantseqWithParentTimerData);
			significantContextChildWithParent.setParentSequence(significantContext);
			significantContext.getNestedSequences().add(significantContextChildWithParent);

			InvocationSequenceData significantContextChildWithParent2 = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			TimerData significantseqWithParentTimerData2 = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			significantseqWithParentTimerData2.calculateExclusiveMin(RANDOM.nextDouble());
			significantseqWithParentTimerData2.setExclusiveDuration(4000.0);
			significantContextChildWithParent2.setTimerData(significantseqWithParentTimerData2);
			significantContextChildWithParent2.setParentSequence(significantContext);
			significantContext.getNestedSequences().add(significantContextChildWithParent2);

			InvocationSequenceData secondRawInvocation = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			TimerData timerData3 = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timerData3.calculateExclusiveMin(RANDOM.nextDouble());
			timerData3.setExclusiveDuration(2000.0);
			secondRawInvocation.setTimerData(timerData3);
			secondRawInvocation.setParentSequence(globalContext);

			InvocationSequenceData secondSeqWithParent = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			secondSeqWithParent.setParentSequence(secondRawInvocation);
			secondRawInvocation.getNestedSequences().add(secondSeqWithParent);

			rawInvocations.add(firstSeqWithParent);
			rawInvocations.add(significantContext);
			rawInvocations.add(secondRawInvocation);

			List<InvocationSequenceData> rawInvocationsSignificant = new ArrayList<InvocationSequenceData>();
			rawInvocationsSignificant.add(significantContextChildWithParent);
			rawInvocationsSignificant.add(significantContextChildWithParent2);

			when(timeWastingOperation.getRawInvocationsSequenceElements()).thenReturn(rawInvocationsSignificant);
			when(globalContext.getNestedSequences()).thenReturn(rawInvocations);
			InvocationSequenceData problemContext = problemContextRule.action();

			assertThat("The returned problemContext must be the most significant cluster context", problemContext, is(equalTo(significantContext)));
		}

		@Test
		public void problemContextMustBeGlobalContext() {

			List<InvocationSequenceData> rawInvocations = new ArrayList<InvocationSequenceData>();
			TimerData timerData = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			// timerData.calculateExclusiveMin(RANDOM.nextDouble());
			timerData.setExclusiveDuration(RANDOM.nextDouble());
			when(globalContext.getTimerData()).thenReturn(timerData);
			rawInvocations.add(globalContext);

			when(timeWastingOperation.getRawInvocationsSequenceElements()).thenReturn(rawInvocations);

			InvocationSequenceData problemContext = problemContextRule.action();

			assertThat("The returned problemContext must be the most significant cluster context", problemContext, is(equalTo(globalContext)));
		}

		@Test
		public void problemContextMustBeTheMostSignificantClusterContextWithOneInvocationSequence() {
			List<InvocationSequenceData> rawInvocations = new ArrayList<InvocationSequenceData>();

			TimerData timerData = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timerData.calculateExclusiveMin(RANDOM.nextDouble());
			timerData.setExclusiveDuration(2000.0);
			globalContext.setTimerData(timerData);

			rawInvocations.add(globalContext);

			List<InvocationSequenceData> rawInvocationsSignificant = new ArrayList<InvocationSequenceData>();
			rawInvocationsSignificant.add(globalContext);

			when(timeWastingOperation.getRawInvocationsSequenceElements()).thenReturn(rawInvocationsSignificant);
			when(globalContext.getNestedSequences()).thenReturn(rawInvocations);

			InvocationSequenceData problemContext = problemContextRule.action();

			assertThat("The returned problemContext must be the most significant cluster context with one invocation sequence", problemContext, is(equalTo(globalContext)));
		}

	}

}
