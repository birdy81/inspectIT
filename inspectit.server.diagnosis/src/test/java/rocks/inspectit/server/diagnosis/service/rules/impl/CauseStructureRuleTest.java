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
import rocks.inspectit.shared.all.communication.data.SqlStatementData;
import rocks.inspectit.shared.all.communication.data.TimerData;
import rocks.inspectit.shared.all.communication.data.diagnosis.results.CauseStructure;
import rocks.inspectit.shared.all.communication.data.diagnosis.results.CauseStructure.CauseType;
import rocks.inspectit.shared.all.testbase.TestBase;

public class CauseStructureRuleTest extends TestBase {

	@InjectMocks
	CauseStructureRule causeStructureRule;

	@Mock
	InvocationSequenceData problemContext;

	@Mock
	AggregatedInvocationSequenceData cause;

	public static class Action extends CauseStructureRuleTest {

		private static final Random RANDOM = new Random();
		private static final Timestamp DEF_DATE = new Timestamp(new Date().getTime());
		private static final long METHOD_IDENT_EQUAL = new Long(108);
		private static final long METHOD_IDENT_DIFF = RANDOM.nextLong();
		private static final TimerData TIMER_DATA = new TimerData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
		private static final long PLATFORM_IDENT = RANDOM.nextLong();
		private static final long SENSOR_TYPE_IDENT = RANDOM.nextLong();

		@Test
		public void timerDataMustReturnAnInstanceOfSingleCauseTypeIfTheCauseHasJustOneElement() {
			List<InvocationSequenceData> rawInvocations = new ArrayList<>();
			rawInvocations.add(new InvocationSequenceData());
			when(cause.getRawInvocationsSequenceElements()).thenReturn(rawInvocations);
			when(cause.getMethodIdent()).thenReturn(METHOD_IDENT_EQUAL);
			when(cause.size()).thenReturn(1);

			CauseStructure causeStructure = causeStructureRule.action();

			assertThat("The returned cause type must be single", causeStructure.getCauseType(), is(CauseType.SINGLE));
		}

		@Test
		public void timerDataMustReturnAnInstanceOfRecursiveCauseTypeIfTheCauseHasMoreThanOneSequenceWithTheSameMethodIdent() {
			List<InvocationSequenceData> rawInvocations = new ArrayList<>();
			rawInvocations.add(new InvocationSequenceData());

			InvocationSequenceData problemContextParent = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			problemContextParent.setTimerData(TIMER_DATA);

			InvocationSequenceData problemContextChild1 = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			problemContextChild1.setTimerData(TIMER_DATA);
			problemContextChild1.setParentSequence(problemContextParent);

			InvocationSequenceData problemContextChild2 = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			problemContextChild2.setTimerData(TIMER_DATA);
			problemContextChild2.setParentSequence(problemContextParent);

			when(cause.getRawInvocationsSequenceElements()).thenReturn(rawInvocations);
			when(cause.getMethodIdent()).thenReturn(METHOD_IDENT_EQUAL);
			when(cause.size()).thenReturn(3);
			when(problemContext.getParentSequence()).thenReturn(problemContextParent);
			when(problemContext.getMethodIdent()).thenReturn(METHOD_IDENT_EQUAL);
			when(problemContext.getTimerData()).thenReturn(TIMER_DATA);

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
			when(problemContext.getParentSequence()).thenReturn(parentSequence);
			when(problemContext.getTimerData()).thenReturn(TIMER_DATA);
			when(cause.getRawInvocationsSequenceElements()).thenReturn(rawInvocations);
			when(cause.getMethodIdent()).thenReturn(METHOD_IDENT_EQUAL);

			CauseStructure causeStructure = causeStructureRule.action();

			assertThat("The returned cause type must be iterative", causeStructure.getCauseType(), is(CauseType.ITERATIVE));
		}

		@Test
		public void timerDataMustReturnAnInstanceOfIterativeCauseTypeIfTheCauseHasNoElements() {
			when(cause.getMethodIdent()).thenReturn(METHOD_IDENT_EQUAL);
			when(cause.size()).thenReturn(0);

			CauseStructure causeStructure = causeStructureRule.action();

			assertThat("The returned cause type must be iterative", causeStructure.getCauseType(), is(CauseType.ITERATIVE));
		}

		@Test
		public void sqlStatementDataMustReturnAnInstanceOfRecursiveCauseTypeIfTheCauseHasMoreThanOneSequenceWithTheSameMethodIdent() {
			List<InvocationSequenceData> rawInvocations = new ArrayList<>();
			rawInvocations.add(new InvocationSequenceData());
			SqlStatementData sqlData = new SqlStatementData(new Timestamp(System.currentTimeMillis()), 10L, 20L, 30L);
			sqlData.setCount(1);
			sqlData.setSql("somethingsomething");
			InvocationSequenceData problemContextParent = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_DIFF);
			problemContextParent.setSqlStatementData(sqlData);
			InvocationSequenceData problemContextGrandParent = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT_EQUAL);
			problemContextGrandParent.setSqlStatementData(sqlData);
			problemContextParent.setParentSequence(problemContextGrandParent);
			when(cause.getRawInvocationsSequenceElements()).thenReturn(rawInvocations);
			when(cause.getMethodIdent()).thenReturn(METHOD_IDENT_EQUAL);
			when(cause.size()).thenReturn(3);
			when(cause.getSqlStatementData()).thenReturn(sqlData);
			when(problemContext.getParentSequence()).thenReturn(problemContextParent);
			when(problemContext.getMethodIdent()).thenReturn(METHOD_IDENT_EQUAL);
			when(problemContext.getSqlStatementData()).thenReturn(sqlData);

			CauseStructure causeStructure = causeStructureRule.action();

			assertThat("The returned cause type must be recursive", causeStructure.getCauseType(), is(CauseType.RECURSIVE));
		}


	}
}
