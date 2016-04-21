package rocks.inspectit.server.diagnosis.service.rules.impl;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.Test;

import rocks.inspectit.server.diagnosis.service.aggregation.AggregatedDiagnosisData;
import rocks.inspectit.server.diagnosis.service.data.CauseCluster;
import rocks.inspectit.shared.all.communication.data.HttpTimerData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.SqlStatementData;
import rocks.inspectit.shared.all.communication.data.TimerData;
import rocks.inspectit.shared.all.testbase.TestBase;
import rocks.inspectit.shared.all.util.Pair;
import rocks.inspectit.shared.cs.communication.data.diagnosis.AggregatedDiagnosisTimerData;
import rocks.inspectit.shared.cs.communication.data.diagnosis.CauseStructure;
import rocks.inspectit.shared.cs.communication.data.diagnosis.CauseStructure.CauseType;

/**
 *
 * @author Isabel Vico Peinado
 *
 */
public class CauseStructureRuleTest extends TestBase {

	@InjectMocks
	CauseStructureRule causeStructureRule;

	@Mock
	CauseCluster problemContext;

	@Mock
	InvocationSequenceData commonContext;

	@Mock
	AggregatedDiagnosisData rootCause;

	public static class Action extends CauseStructureRuleTest {

		private static final Random RANDOM = new Random();
		private static final Timestamp DEF_DATE = new Timestamp(new Date().getTime());
		private static final long METHOD_IDENT_EQUAL = new Long(108);
		private static final long METHOD_IDENT_DIFF = RANDOM.nextLong();
		private static final Timestamp CURRENT_TIME = new Timestamp(System.currentTimeMillis());
		private static final TimerData TIMER_DATA = new TimerData(CURRENT_TIME, 10L, 20L, 30L);
		private static final AggregatedDiagnosisTimerData AGGREGATED_TIMER_DATA = new AggregatedDiagnosisTimerData(TIMER_DATA);
		private static final SqlStatementData TIMER_DATA_SQL = new SqlStatementData(CURRENT_TIME, 10L, 20L, 30L);
		private static final AggregatedDiagnosisTimerData AGGREGATED_TIMER_DATA_SQL = new AggregatedDiagnosisTimerData(TIMER_DATA_SQL);
		private static final HttpTimerData TIMER_DATA_HTTP = new HttpTimerData(CURRENT_TIME, 10L, 20L, 30L);
		private static final AggregatedDiagnosisTimerData AGGREGATED_TIMER_DATA_HTTP = new AggregatedDiagnosisTimerData(TIMER_DATA_HTTP);
		private static final long PLATFORM_IDENT = RANDOM.nextLong();
		private static final long SENSOR_TYPE_IDENT = RANDOM.nextLong();

		@Test(expectedExceptions = IllegalArgumentException.class)
		public void expectedExceptionsIfTheCauseHasNoElements() {
			when(rootCause.getMethodIdent()).thenReturn(METHOD_IDENT_EQUAL);
			when(rootCause.size()).thenReturn(0);

			CauseStructure causeStructure = causeStructureRule.action();

			assert (causeStructure.getCauseType() == CauseType.SINGLE);
		}

		@Test
		public void timerDataMustReturnAnInstanceOfSingleCauseTypeIfTheCauseHasJustOneElement() {
			when(rootCause.size()).thenReturn(1);

			CauseStructure causeStructure = causeStructureRule.action();

			assertThat("The returned cause type must be single", causeStructure.getCauseType(), is(CauseType.SINGLE));
		}

		@Test
		public void timerDataMustReturnAnInstanceOfRecursiveCauseTypeIfTheCauseHasMoreThanOneSequenceWithTheSameMethodIdent() {
			InvocationSequenceData detectedProblemContext = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			detectedProblemContext.setTimerData(TIMER_DATA);
			InvocationSequenceData firstMethod = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			firstMethod.setTimerData(TIMER_DATA);
			InvocationSequenceData secondMethod = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			secondMethod.setTimerData(TIMER_DATA);
			detectedProblemContext.getNestedSequences().add(firstMethod);
			firstMethod.setParentSequence(detectedProblemContext);
			firstMethod.getNestedSequences().add(secondMethod);
			secondMethod.setParentSequence(firstMethod);
			when(rootCause.getMethodIdent()).thenReturn(detectedProblemContext.getNestedSequences().get(0).getMethodIdent());
			when(rootCause.size()).thenReturn(2);
			when(rootCause.getAggregatedDiagnosisTimerData()).thenReturn(AGGREGATED_TIMER_DATA);
			when(rootCause.getAggregationKey()).thenReturn(detectedProblemContext.getNestedSequences().get(0).getMethodIdent());

			when(problemContext.getCommonContext()).thenReturn(commonContext);
			when(commonContext.getParentSequence()).thenReturn(null);
			when(commonContext.getNestedSequences()).thenReturn(Collections.singletonList(detectedProblemContext));
			when(commonContext.getMethodIdent()).thenReturn(firstMethod.getMethodIdent());
			when(commonContext.getTimerData()).thenReturn(firstMethod.getTimerData());
			when(problemContext.getCauseInvocations()).thenReturn(detectedProblemContext.getNestedSequences());
			when(commonContext.getDuration()).thenReturn(firstMethod.getDuration());

			CauseStructure causeStructure = causeStructureRule.action();

			assertThat("The returned cause type must be recursive", causeStructure.getCauseType(), is(CauseType.RECURSIVE));
		}

		@Test
		public void timerDataMustReturnAnInstanceOfIterativeCauseTypeIfTheCauseHasNotMoreThanOneSequenceWithTheSameMethodIdent() {
			InvocationSequenceData childSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			childSequence.setTimerData(TIMER_DATA);
			InvocationSequenceData parentSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			parentSequence.setTimerData(TIMER_DATA);
			InvocationSequenceData grandParentSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			grandParentSequence.setTimerData(TIMER_DATA);
			parentSequence.setParentSequence(grandParentSequence);
			List<InvocationSequenceData> rawInvocations = new ArrayList<>();
			rawInvocations.add(new InvocationSequenceData());
			rawInvocations.add(childSequence);
			when(problemContext.getCommonContext()).thenReturn(commonContext);
			when(commonContext.getParentSequence()).thenReturn(parentSequence);
			when(commonContext.getTimerData()).thenReturn(TIMER_DATA);
			when(rootCause.getRawInvocationsSequenceElements()).thenReturn(rawInvocations);
			when(rootCause.getMethodIdent()).thenReturn(METHOD_IDENT_EQUAL);
			when(rootCause.getAggregatedDiagnosisTimerData()).thenReturn(AGGREGATED_TIMER_DATA);

			CauseStructure causeStructure = causeStructureRule.action();

			assertThat("The returned cause type must be iterative", causeStructure.getCauseType(), is(CauseType.ITERATIVE));
		}

		@Test
		public void sqlStatementDataMustReturnAnInstanceOfIterativeDataBaseCauseTypeIfTheCauseHasNotMoreThanOneSequenceWithTheSameMethodIdent() {
			InvocationSequenceData childSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			TIMER_DATA_SQL.setCount(1);
			TIMER_DATA_SQL.setSql("somethingsomething");
			childSequence.setTimerData(TIMER_DATA_SQL);
			InvocationSequenceData parentSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			parentSequence.setTimerData(TIMER_DATA_SQL);
			InvocationSequenceData grandParentSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			grandParentSequence.setTimerData(TIMER_DATA_SQL);
			parentSequence.setParentSequence(grandParentSequence);
			List<InvocationSequenceData> rawInvocations = new ArrayList<>();
			rawInvocations.add(new InvocationSequenceData());
			rawInvocations.add(childSequence);
			when(problemContext.getCommonContext()).thenReturn(commonContext);
			when(commonContext.getParentSequence()).thenReturn(parentSequence);
			when(commonContext.getTimerData()).thenReturn(TIMER_DATA_SQL);
			when(commonContext.getSqlStatementData()).thenReturn(TIMER_DATA_SQL);
			when(rootCause.getRawInvocationsSequenceElements()).thenReturn(rawInvocations);
			when(rootCause.getMethodIdent()).thenReturn(METHOD_IDENT_EQUAL);
			when(rootCause.getAggregatedDiagnosisTimerData()).thenReturn(AGGREGATED_TIMER_DATA_SQL);

			CauseStructure causeStructure = causeStructureRule.action();

			assertThat("The returned cause type must be iterative", causeStructure.getCauseType(), is(CauseType.ITERATIVE));
		}

		@Test
		public void sqlStatementDataMustReturnAnInstanceOfSingleDataBaseCauseTypeIfTheCauseHasJustOneElement() {
			when(rootCause.size()).thenReturn(1);

			CauseStructure causeStructure = causeStructureRule.action();

			assertThat("The returned cause type must be single", causeStructure.getCauseType(), is(CauseType.SINGLE));
		}

		@Test
		public void sqlStatementDataMustReturnAnInstanceOfRecursiveDataBaseCauseTypeIfTheCauseHasMoreThanOneSequenceWithTheSameMethodIdent() {
			InvocationSequenceData detectedProblemContext = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			TIMER_DATA_SQL.setCount(1);
			TIMER_DATA_SQL.setSql("somethingsomething");
			detectedProblemContext.setSqlStatementData(TIMER_DATA_SQL);
			InvocationSequenceData firstMethod = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			firstMethod.setSqlStatementData(TIMER_DATA_SQL);
			InvocationSequenceData secondMethod = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			secondMethod.setSqlStatementData(TIMER_DATA_SQL);
			detectedProblemContext.getNestedSequences().add(firstMethod);
			firstMethod.setParentSequence(detectedProblemContext);
			firstMethod.getNestedSequences().add(secondMethod);
			secondMethod.setParentSequence(firstMethod);
			when(rootCause.getMethodIdent()).thenReturn(detectedProblemContext.getNestedSequences().get(0).getMethodIdent());
			when(rootCause.size()).thenReturn(2);
			Pair<Long, String> pair = new Pair<Long, String>(detectedProblemContext.getNestedSequences().get(0).getMethodIdent(), "somethingsomething");
			when(rootCause.getAggregationKey()).thenReturn(pair);

			when(problemContext.getCommonContext()).thenReturn(commonContext);
			when(commonContext.getParentSequence()).thenReturn(null);
			when(commonContext.getNestedSequences()).thenReturn(Collections.singletonList(detectedProblemContext));
			when(commonContext.getMethodIdent()).thenReturn(firstMethod.getMethodIdent());
			when(commonContext.getTimerData()).thenReturn(firstMethod.getTimerData());
			when(commonContext.getDuration()).thenReturn(firstMethod.getDuration());
			when(commonContext.getSqlStatementData()).thenReturn(TIMER_DATA_SQL);
			when(problemContext.getCauseInvocations()).thenReturn(detectedProblemContext.getNestedSequences());

			CauseStructure causeStructure = causeStructureRule.action();

			assertThat("The returned cause type must be recursive", causeStructure.getCauseType(), is(CauseType.RECURSIVE));
		}

		@Test
		public void httpTimerDataMustReturnAnInstanceOfSingleCauseTypeIfTheCauseHasJustOneElement() {
			when(rootCause.size()).thenReturn(1);

			CauseStructure causeStructure = causeStructureRule.action();

			assertThat("The returned cause type must be single", causeStructure.getCauseType(), is(CauseType.SINGLE));
		}

		@Test
		public void httpTimerDataMustReturnAnInstanceOfRecursiveCauseTypeIfTheCauseHasMoreThanOneSequenceWithTheSameMethodIdent() {
			InvocationSequenceData detectedProblemContext = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			detectedProblemContext.setTimerData(TIMER_DATA_HTTP);
			InvocationSequenceData firstMethod = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			firstMethod.setTimerData(TIMER_DATA_HTTP);
			InvocationSequenceData secondMethod = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			secondMethod.setTimerData(TIMER_DATA_HTTP);
			detectedProblemContext.getNestedSequences().add(firstMethod);
			firstMethod.setParentSequence(detectedProblemContext);
			firstMethod.getNestedSequences().add(secondMethod);
			secondMethod.setParentSequence(firstMethod);
			when(rootCause.getMethodIdent()).thenReturn(detectedProblemContext.getNestedSequences().get(0).getMethodIdent());
			when(rootCause.size()).thenReturn(2);
			Pair<Long, String> pair = new Pair<Long, String>(detectedProblemContext.getNestedSequences().get(0).getMethodIdent(), "n.a.");
			when(rootCause.getAggregationKey()).thenReturn(pair);
			when(problemContext.getCommonContext()).thenReturn(commonContext);
			when(commonContext.getParentSequence()).thenReturn(null);
			when(commonContext.getNestedSequences()).thenReturn(Collections.singletonList(detectedProblemContext));
			when(commonContext.getMethodIdent()).thenReturn(firstMethod.getMethodIdent());
			when(commonContext.getTimerData()).thenReturn(firstMethod.getTimerData());
			when(problemContext.getCauseInvocations()).thenReturn(detectedProblemContext.getNestedSequences());
			when(commonContext.getDuration()).thenReturn(firstMethod.getDuration());

			CauseStructure causeStructure = causeStructureRule.action();

			assertThat("The returned cause type must be recursive", causeStructure.getCauseType(), is(CauseType.RECURSIVE));
		}

		@Test
		public void httpTimerDataMustReturnAnInstanceOfIterativeCauseTypeIfTheCauseHasNotMoreThanOneSequenceWithTheSameMethodIdent() {
			InvocationSequenceData childSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			childSequence.setTimerData(TIMER_DATA_HTTP);
			InvocationSequenceData parentSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			parentSequence.setTimerData(TIMER_DATA_HTTP);
			InvocationSequenceData grandParentSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			grandParentSequence.setTimerData(TIMER_DATA_HTTP);
			parentSequence.setParentSequence(grandParentSequence);
			List<InvocationSequenceData> rawInvocations = new ArrayList<>();
			rawInvocations.add(new InvocationSequenceData());
			rawInvocations.add(childSequence);
			when(problemContext.getCommonContext()).thenReturn(commonContext);
			when(commonContext.getParentSequence()).thenReturn(parentSequence);
			when(commonContext.getTimerData()).thenReturn(TIMER_DATA_HTTP);
			when(rootCause.getRawInvocationsSequenceElements()).thenReturn(rawInvocations);
			when(rootCause.getMethodIdent()).thenReturn(METHOD_IDENT_EQUAL);
			when(rootCause.getAggregatedDiagnosisTimerData()).thenReturn(AGGREGATED_TIMER_DATA_HTTP);

			CauseStructure causeStructure = causeStructureRule.action();

			assertThat("The returned cause type must be iterative", causeStructure.getCauseType(), is(CauseType.ITERATIVE));
		}

		@Test
		public void differentTimerDataMustReturnAnInstanceOfIterativeCauseType() {
			InvocationSequenceData childSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			InvocationSequenceData parentSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			InvocationSequenceData grandParentSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			parentSequence.setParentSequence(grandParentSequence);
			List<InvocationSequenceData> rawInvocations = new ArrayList<>();
			rawInvocations.add(new InvocationSequenceData());
			rawInvocations.add(childSequence);
			when(problemContext.getCommonContext()).thenReturn(commonContext);
			when(commonContext.getParentSequence()).thenReturn(parentSequence);
			when(rootCause.getRawInvocationsSequenceElements()).thenReturn(rawInvocations);
			when(rootCause.getMethodIdent()).thenReturn(METHOD_IDENT_EQUAL);
			when(rootCause.getAggregationKey()).thenReturn(METHOD_IDENT_EQUAL);

			CauseStructure causeStructure = causeStructureRule.action();

			assertThat("The returned cause type must be iterative", causeStructure.getCauseType(), is(CauseType.ITERATIVE));
		}
	}
}
