package rocks.inspectit.server.processor.impl;

import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.persistence.EntityManager;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import rocks.inspectit.server.diagnosis.results.IDiagnosisResults;
import rocks.inspectit.server.diagnosis.service.DiagnosisService;
import rocks.inspectit.server.diagnosis.service.aggregation.AggregatedDiagnosisData;
import rocks.inspectit.server.diagnosis.service.aggregation.DiagnosisDataAggregator;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.TimerData;
import rocks.inspectit.shared.all.testbase.TestBase;
import rocks.inspectit.shared.cs.communication.data.diagnosis.CauseStructure;
import rocks.inspectit.shared.cs.communication.data.diagnosis.CauseStructure.CauseType;
import rocks.inspectit.shared.cs.communication.data.diagnosis.CauseStructure.SourceType;
import rocks.inspectit.shared.cs.communication.data.diagnosis.ProblemOccurrence;
import rocks.inspectit.shared.cs.communication.data.diagnosis.RootCause;


/**
 * @author Christian Voegele
 *
 */
@SuppressWarnings("PMD")
public class DiagnosisCmrProcessorTest extends TestBase {

	@InjectMocks
	DiagnosisCmrProcessor cmrProcessor = new DiagnosisCmrProcessor(1000);

	public static class Process extends DiagnosisCmrProcessorTest {

		@Mock
		DiagnosisService diagnosisService;

		@Mock
		EntityManager entityManager;

		private static final long METHOD_IDENT = 108L;
		private static final Timestamp DEF_DATE = new Timestamp(new Date().getTime());
		boolean canBeProcessed = false;
		InvocationSequenceData invocationSequenceRoot;
		InvocationSequenceData firstChildSequence;
		InvocationSequenceData secondChildSequence;
		InvocationSequenceData thirdChildSequence;

		@BeforeMethod
		public void init() {
			invocationSequenceRoot = new InvocationSequenceData();
			invocationSequenceRoot.setId(1);
			invocationSequenceRoot.setDuration(5000d);
			firstChildSequence = new InvocationSequenceData(DEF_DATE, 10, 10, METHOD_IDENT);
			firstChildSequence.setDuration(200d);
			firstChildSequence.setId(2);
			secondChildSequence = new InvocationSequenceData(DEF_DATE, 20, 20, METHOD_IDENT);
			secondChildSequence.setDuration(4000d);
			secondChildSequence.setId(3);
			thirdChildSequence = new InvocationSequenceData(DEF_DATE, 30, 30, METHOD_IDENT);
			thirdChildSequence.setDuration(500d);
			thirdChildSequence.setId(4);
			invocationSequenceRoot.getNestedSequences().add(firstChildSequence);
			invocationSequenceRoot.getNestedSequences().add(secondChildSequence);
			invocationSequenceRoot.getNestedSequences().add(thirdChildSequence);
		}

		@Test
		public void processData() {
			cmrProcessor.processData(invocationSequenceRoot, entityManager);
			verifyZeroInteractions(entityManager);

			canBeProcessed = cmrProcessor.canBeProcessed(invocationSequenceRoot);
			assertEquals(canBeProcessed, true);

			canBeProcessed = cmrProcessor.canBeProcessed(thirdChildSequence);
			assertEquals(canBeProcessed, false);
		}

	}

	public static class OnNewDiagnosisResult extends DiagnosisCmrProcessorTest {

		@Mock
		DiagnosisService diagnosisService;

		@Mock
		EntityManager entityManager;

		@Mock
		IDiagnosisResults<ProblemOccurrence> diagnosisResults;

		private static final long METHOD_IDENT = 108L;
		private static final Timestamp DEF_DATE = new Timestamp(new Date().getTime());
		boolean canBeProcessed = false;
		InvocationSequenceData invocationSequenceRoot;
		InvocationSequenceData firstChildSequence;
		InvocationSequenceData secondChildSequence;
		InvocationSequenceData thirdChildSequence;

		@BeforeMethod
		public void init() {
			invocationSequenceRoot = new InvocationSequenceData();
			invocationSequenceRoot.setId(1);
			invocationSequenceRoot.setDuration(5000d);

			firstChildSequence = new InvocationSequenceData(DEF_DATE, 10, 10, METHOD_IDENT);
			firstChildSequence.setDuration(200d);
			firstChildSequence.setId(2);

			secondChildSequence = new InvocationSequenceData(DEF_DATE, 20, 20, METHOD_IDENT);
			secondChildSequence.setDuration(4000d);
			secondChildSequence.setId(3);
			long timestampValue = new Date().getTime();
			long platformIdent = new Random().nextLong();
			final long count = 2;
			final double min = 1;
			final double max = 2;
			final double duration = 3;
			TimerData timerData = new TimerData();
			timerData.setTimeStamp(new Timestamp(timestampValue));
			timerData.setPlatformIdent(platformIdent);
			timerData.setCount(count);
			timerData.setExclusiveCount(count);
			timerData.setDuration(duration);
			timerData.setCpuDuration(duration);
			timerData.setExclusiveDuration(duration);
			timerData.calculateMin(min);
			timerData.calculateCpuMin(min);
			timerData.calculateExclusiveMin(min);
			timerData.calculateMax(max);
			timerData.calculateCpuMax(max);
			timerData.calculateExclusiveMax(max);
			timerData.setMethodIdent(50L);
			secondChildSequence.setTimerData(timerData);

			thirdChildSequence = new InvocationSequenceData(DEF_DATE, 30, 30, METHOD_IDENT);
			thirdChildSequence.setDuration(500d);
			thirdChildSequence.setId(4);

			invocationSequenceRoot.getNestedSequences().add(firstChildSequence);
			invocationSequenceRoot.getNestedSequences().add(secondChildSequence);
			invocationSequenceRoot.getNestedSequences().add(thirdChildSequence);
		}

		@Test
		public void onNewDiagnosisResult() {
			CauseStructure causeStructure = new CauseStructure(CauseType.SINGLE, SourceType.TIMERDATA);
			DiagnosisDataAggregator aggregator = new DiagnosisDataAggregator();
			AggregatedDiagnosisData aggregatedInvocationSequenceData = aggregator.getAggregatedDiagnosisData(secondChildSequence);
			aggregator.aggregate(aggregatedInvocationSequenceData, secondChildSequence);
			RootCause rootCause = new RootCause(aggregatedInvocationSequenceData.getMethodIdent(), aggregatedInvocationSequenceData.getAggregatedDiagnosisTimerData());

			ProblemOccurrence problemOccurrence = new ProblemOccurrence(secondChildSequence, secondChildSequence,
					secondChildSequence, rootCause, causeStructure.getCauseType(), causeStructure.getSourceType());

			Set<ProblemOccurrence> problemOccurenceSet = new HashSet<ProblemOccurrence>();
			problemOccurenceSet.add(problemOccurrence);
			when(diagnosisResults.getDiagnosisResults()).thenReturn(problemOccurenceSet);

			cmrProcessor.onNewDiagnosisResult(problemOccurrence);

			Collection<ProblemOccurrence> newProblemOccurenceSet = diagnosisResults.getDiagnosisResults();
			assertEquals(newProblemOccurenceSet.contains(problemOccurrence), true);
		}

	}

}
