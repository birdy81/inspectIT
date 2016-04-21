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

import rocks.inspectit.server.diagnosis.service.aggregation.AggregatedDiagnosisData;
import rocks.inspectit.server.diagnosis.service.data.CauseCluster;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.TimerData;
import rocks.inspectit.shared.all.testbase.TestBase;
import rocks.inspectit.shared.cs.communication.data.diagnosis.AggregatedDiagnosisTimerData;

/**
 *
 * @author Isabel Vico Peinado
 *
 */
public class ProblemContextRuleTest extends TestBase {

	@InjectMocks
	ProblemContextRule problemContextRule;

	@Mock
	InvocationSequenceData globalContext;

	@Mock
	AggregatedDiagnosisData timeWastingOperation;


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

			globalContext = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
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

			TimerData timeWastingOperationTimerData = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timeWastingOperationTimerData.calculateExclusiveMin(1);
			timeWastingOperationTimerData.setExclusiveDuration(2000);
			timeWastingOperationTimerData.setDuration(2000);
			AggregatedDiagnosisTimerData aggregatedDiagnosisTimerData = new AggregatedDiagnosisTimerData(timeWastingOperationTimerData);
			when(timeWastingOperation.getAggregatedDiagnosisTimerData()).thenReturn(aggregatedDiagnosisTimerData);

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
			InvocationSequenceData parentSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 9L);
			TimerData timerDataSeachDB = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timerDataSeachDB.setDuration(2000);
			timerDataSeachDB.setExclusiveDuration(2000d);
			parentSequence.setTimerData(timerDataSeachDB);
			parentSequence.setDuration(2000d);

			InvocationSequenceData firstSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 10L);
			TimerData timerDataFirstSequence = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timerDataFirstSequence.setDuration(100d);
			timerDataFirstSequence.setExclusiveDuration(100d);
			timerDataFirstSequence.calculateExclusiveMin(1d);
			firstSequence.setTimerData(timerDataFirstSequence);
			firstSequence.setDuration(100d);
			InvocationSequenceData significantCluster = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 10L);
			TimerData timerDataSignificantCluster = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timerDataSignificantCluster.setDuration(1700d);
			timerDataSignificantCluster.setExclusiveDuration(1700d);
			timerDataSignificantCluster.calculateExclusiveMin(1d);
			significantCluster.setTimerData(timerDataSignificantCluster);
			significantCluster.setDuration(1700d);

			InvocationSequenceData secondSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 10L);
			TimerData timerDataSecondSequence = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timerDataSecondSequence.setDuration(200d);
			timerDataSecondSequence.setExclusiveDuration(200d);
			timerDataSecondSequence.calculateExclusiveMin(1d);
			secondSequence.setTimerData(timerDataSecondSequence);
			secondSequence.setDuration(200d);

			parentSequence.getNestedSequences().add(firstSequence);
			parentSequence.getNestedSequences().add(significantCluster);
			parentSequence.getNestedSequences().add(secondSequence);
			firstSequence.setParentSequence(parentSequence);
			significantCluster.setParentSequence(parentSequence);
			secondSequence.setParentSequence(parentSequence);
			when(timeWastingOperation.getRawInvocationsSequenceElements()).thenReturn(parentSequence.getNestedSequences());
			TimerData timeWastingOperationTimerData = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timeWastingOperationTimerData.calculateExclusiveMin(1);
			timeWastingOperationTimerData.setExclusiveDuration(2000);
			timeWastingOperationTimerData.setDuration(2200);
			AggregatedDiagnosisTimerData aggregatedDiagnosisTimerData = new AggregatedDiagnosisTimerData(timeWastingOperationTimerData);
			when(timeWastingOperation.getAggregatedDiagnosisTimerData()).thenReturn(aggregatedDiagnosisTimerData);

			CauseCluster problemContext = problemContextRule.action();

			assertThat("The returned problemContext must be the most significant cluster context", problemContext.getCommonContext(), is(significantCluster));
		}

		@Test
		public void problemContextMustBeTheProperInvocationWithClustering() {
			InvocationSequenceData rootInvocation = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 3L);
			TimerData timerDataGlobalContext = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timerDataGlobalContext.setDuration(5200d);
			timerDataGlobalContext.setExclusiveDuration(5200d);
			rootInvocation.setTimerData(timerDataGlobalContext);
			rootInvocation.setDuration(5200d);
			InvocationSequenceData firstSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 5L);
			TimerData timerDataFirstSequence = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timerDataFirstSequence.setDuration(5200d);
			timerDataFirstSequence.setExclusiveDuration(5200d);
			firstSequence.setTimerData(timerDataFirstSequence);
			firstSequence.setDuration(5200d);
			InvocationSequenceData expectedProblemContext = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 9L);
			TimerData timerDataExpectedProblemContext = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timerDataExpectedProblemContext.setDuration(5000d);
			timerDataExpectedProblemContext.setExclusiveDuration(5000d);
			expectedProblemContext.setTimerData(timerDataExpectedProblemContext);
			expectedProblemContext.setDuration(5000d);
			InvocationSequenceData firstChildSeq = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 10L);
			TimerData timerDataFirstChildSeq = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timerDataFirstChildSeq.setDuration(2400d);
			timerDataFirstChildSeq.setExclusiveDuration(2400d);
			timerDataFirstChildSeq.calculateExclusiveMin(1d);
			firstChildSeq.setTimerData(timerDataFirstChildSeq);
			firstChildSeq.setDuration(2400d);
			InvocationSequenceData secondChildSeq = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 10L);
			TimerData timerDataSecondChildSeq = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timerDataSecondChildSeq.setDuration(1400d);
			timerDataSecondChildSeq.setExclusiveDuration(1400d);
			timerDataSecondChildSeq.calculateExclusiveMin(1d);
			secondChildSeq.setTimerData(timerDataSecondChildSeq);
			secondChildSeq.setDuration(1400d);
			InvocationSequenceData thirdChildSeq = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, 10L);
			TimerData timerDataThirdChildSeq = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timerDataThirdChildSeq.setDuration(800d);
			timerDataThirdChildSeq.setExclusiveDuration(800d);
			timerDataThirdChildSeq.calculateExclusiveMin(1d);
			thirdChildSeq.setTimerData(timerDataThirdChildSeq);
			thirdChildSeq.setDuration(800d);
			rootInvocation.getNestedSequences().add(firstSequence);
			firstSequence.setParentSequence(rootInvocation);
			firstSequence.getNestedSequences().add(expectedProblemContext);
			expectedProblemContext.setParentSequence(firstSequence);
			expectedProblemContext.getNestedSequences().add(firstChildSeq);
			expectedProblemContext.getNestedSequences().add(secondChildSeq);
			expectedProblemContext.getNestedSequences().add(thirdChildSeq);
			firstChildSeq.setParentSequence(expectedProblemContext);
			secondChildSeq.setParentSequence(expectedProblemContext);
			thirdChildSeq.setParentSequence(expectedProblemContext);
			TimerData timeWastingOperationTimerData = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			timeWastingOperationTimerData.calculateExclusiveMin(1);
			timeWastingOperationTimerData.setExclusiveDuration(4600);
			timeWastingOperationTimerData.setDuration(4600);
			AggregatedDiagnosisTimerData aggregatedDiagnosisTimerData = new AggregatedDiagnosisTimerData(timeWastingOperationTimerData);
			when(timeWastingOperation.getAggregatedDiagnosisTimerData()).thenReturn(aggregatedDiagnosisTimerData);
			when(timeWastingOperation.getRawInvocationsSequenceElements()).thenReturn(expectedProblemContext.getNestedSequences());
			when(globalContext.getNestedSequences()).thenReturn(rootInvocation.getNestedSequences());

			CauseCluster problemContext = problemContextRule.action();

			assertThat("The returned problemContext must be the most significant cluster context", problemContext.getCommonContext(), is(expectedProblemContext));
		}
	}
}
