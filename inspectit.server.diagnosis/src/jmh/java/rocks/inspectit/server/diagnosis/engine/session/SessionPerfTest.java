package rocks.inspectit.server.diagnosis.engine.session;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import rocks.inspectit.server.diagnosis.engine.rule.exception.RuleDefinitionException;
import rocks.inspectit.server.diagnosis.engine.rule.factory.Rules;
import rocks.inspectit.server.diagnosis.engine.session.DefaultSessionResult;
import rocks.inspectit.server.diagnosis.engine.session.DefaultSessionResultCollector;
import rocks.inspectit.server.diagnosis.engine.session.Session;
import rocks.inspectit.server.diagnosis.service.rules.RuleConstants;
import rocks.inspectit.server.diagnosis.service.rules.impl.CauseStructureRule;
import rocks.inspectit.server.diagnosis.service.rules.impl.GlobalContextRule;
import rocks.inspectit.server.diagnosis.service.rules.impl.ProblemCauseRule;
import rocks.inspectit.server.diagnosis.service.rules.impl.ProblemContextRule;
import rocks.inspectit.server.diagnosis.service.rules.impl.TimeWastingOperationsRule;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.TimerData;

/**
 * Tests the throughput of the DiagnosisService.
 * 
 * @author Tobias Angerstein
 *
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 10)
@Measurement(iterations = 5)
@Fork(2)
@State(Scope.Thread)
public class SessionPerfTest {

	/**
	 * Default CPU duration which is used in {@link TimerData}.
	 */
	private static final int CPU_DURATION = 20;

	/**
	 * Exclusive duration of parent.
	 */
	private static final int EXCLUSIVE_DURATION_OF_PARENT = 50;

	/**
	 * Exclusive duration of child.
	 */
	private static final int EXCLUSIVE_DURATION_OF_CHILD = 20;

	/**
	 * Exclusive duration of grand child.
	 */
	private static final int EXCLUSIVE_DURATION_OF_GRAND_CHILD = 200;

	/**
	 * Baseline of diagnosis.
	 */
	private static final double DIAGNOSIS_BASELINE = 1000;

	/**
	 * Number of calls.
	 */
	@Param({ "1000", "2000", "3000", "4000"})
	private int numberOfCalls;

	/**
	 * {@link InvocationSequence} with iterative calls.
	 */
	private InvocationSequenceData rootIterativeInvocationSequence;

	/**
	 * {@link InvocationSequence} with recursive calls.
	 */
	private InvocationSequenceData rootRecursiveInvocationSequence;

	/**
	 * {@link Session} under test.
	 */
	private Session<InvocationSequenceData, DefaultSessionResult<InvocationSequenceData>> session;;

	/**
	 * Prepare diagnosis engine.
	 * 
	 * @throws RuleDefinitionException
	 */
	@Setup(Level.Trial)
	public void init() {
		// create iterative InvocationSequenceData
		rootIterativeInvocationSequence = getIterativeInvocationSequence(
				new InvocationSequenceData(new Timestamp(System.currentTimeMillis()), 0, 0, 0), numberOfCalls / 2);

		// create recursive InvocationSequenceData
		InvocationSequenceData leafRecursiveInvocationSequence = new InvocationSequenceData(
				new Timestamp(System.currentTimeMillis()), 0, 0, 1000);
		rootRecursiveInvocationSequence = getRecursiveInvocationSequence(leafRecursiveInvocationSequence, 0,
				numberOfCalls);
		
		// create Session
		try {
			session = new Session<>(
					Rules.define(GlobalContextRule.class, TimeWastingOperationsRule.class, ProblemContextRule.class,
							ProblemCauseRule.class, CauseStructureRule.class),
					new DefaultSessionResultCollector<InvocationSequenceData>());
		} catch (RuleDefinitionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Tests the rules with an iterative call.
	 * 
	 * @throws Exception
	 */
	@Benchmark
	public void testDiagnosisServiceWithIterativeInvocationSequences() {
		try {
			session.activate(rootIterativeInvocationSequence,
					Collections.singletonMap(RuleConstants.DIAGNOSIS_VAR_BASELINE, DIAGNOSIS_BASELINE));
			session.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		session.passivate();
	}

	/**
	 * Tests the rules with a recursive call
	 * 
	 * @throws Exception
	 */
	@Benchmark
	public void testDiagnosisServiceWithRecursiveInvocationSequences() {
		try {
			session.activate(rootRecursiveInvocationSequence,
					Collections.singletonMap(RuleConstants.DIAGNOSIS_VAR_BASELINE, DIAGNOSIS_BASELINE));
			session.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		session.passivate();
	}

	/**
	 * Creates an invocationSequence with recursive invocations.
	 * 
	 * @param leaf
	 *            The leaf of the recursive invocation
	 * @param currentHight
	 *            The current height of the invocation sequence (should be
	 *            initially set to 0)
	 * @param maxHeight
	 *            The max height of the invocation sequence
	 * @return
	 */
	private InvocationSequenceData getRecursiveInvocationSequence(InvocationSequenceData leaf, int currentHight,
			int maxHeight) {
		if (currentHight >= maxHeight) {
			return leaf;
		} else {
			InvocationSequenceData parent = new InvocationSequenceData(
					new Timestamp(System.currentTimeMillis() + (leaf.getTimeStamp().getTime() - 100)), 0, 0,
					leaf.getMethodIdent());
			leaf.setParentSequence(parent);
			parent.setDuration(leaf.getDuration() + EXCLUSIVE_DURATION_OF_PARENT);

			// Create TimerData
			TimerData timerData = new TimerData(parent.getTimeStamp(), parent.getPlatformIdent(),
					parent.getSensorTypeIdent(), parent.getMethodIdent());
			timerData.addDuration(leaf.getDuration() + EXCLUSIVE_DURATION_OF_PARENT);
			timerData.setExclusiveDuration(EXCLUSIVE_DURATION_OF_PARENT);
			timerData.addCpuDuration(20);
			timerData.calculateExclusiveMin(EXCLUSIVE_DURATION_OF_PARENT);
			parent.setTimerData(timerData);

			parent.getNestedSequences().add(leaf);
			return getRecursiveInvocationSequence(parent, currentHight + 1, maxHeight);
		}
	}

	/**
	 * Creates an invocationSequence which has a huge number of iterative calls
	 * 
	 * @param parentSequence
	 *            The root invocation Sequence
	 * @param numberOfChildren
	 *            the number of iterative children
	 * @return the parent sequence with added children
	 */
	private InvocationSequenceData getIterativeInvocationSequence(InvocationSequenceData parentSequence,
			int numberOfChildren) {
		List<InvocationSequenceData> nestedSequencesOfParent = new ArrayList<InvocationSequenceData>();

		double durationOfParent = 0;

		// create list of childs
		for (int i = 0; i < 2; i++) {
			InvocationSequenceData childSequence = new InvocationSequenceData(
					new Timestamp(System.currentTimeMillis() + (i + 100)), 0, 0, parentSequence.getMethodIdent() + 1);
			childSequence.setParentSequence(parentSequence);

			List<InvocationSequenceData> nestedSequencesOfChild = new ArrayList<InvocationSequenceData>();

			double durationOfChild = 0;

			// create list of grand childs
			for (int j = 0; j < numberOfChildren; j++) {
				InvocationSequenceData grandChildSequence = new InvocationSequenceData(
						new Timestamp(System.currentTimeMillis() + (j + 100)), 0, 0,
						childSequence.getMethodIdent() + (10000 + i));
				grandChildSequence.setParentSequence(childSequence);
				grandChildSequence.setDuration(EXCLUSIVE_DURATION_OF_GRAND_CHILD);

				// Create TimerData for grandchild
				TimerData timerDataGrandChild = new TimerData(grandChildSequence.getTimeStamp(),
						grandChildSequence.getPlatformIdent(), grandChildSequence.getSensorTypeIdent(),
						grandChildSequence.getMethodIdent());
				timerDataGrandChild.addDuration(EXCLUSIVE_DURATION_OF_GRAND_CHILD);
				timerDataGrandChild.setExclusiveDuration(EXCLUSIVE_DURATION_OF_GRAND_CHILD);
				timerDataGrandChild.addCpuDuration(20);
				timerDataGrandChild.calculateExclusiveMin(EXCLUSIVE_DURATION_OF_GRAND_CHILD);

				grandChildSequence.setTimerData(timerDataGrandChild);
				nestedSequencesOfChild.add(grandChildSequence);
				durationOfChild += EXCLUSIVE_DURATION_OF_GRAND_CHILD;
			}

			// Add exclusive duration
			durationOfChild += EXCLUSIVE_DURATION_OF_CHILD;

			childSequence.setDuration(durationOfChild);
			childSequence.getNestedSequences().addAll(nestedSequencesOfChild);
			nestedSequencesOfParent.add(childSequence);
			durationOfParent += durationOfChild;

			// Create TimerData for child
			TimerData timerDataChild = new TimerData(childSequence.getTimeStamp(), childSequence.getPlatformIdent(),
					childSequence.getSensorTypeIdent(), childSequence.getMethodIdent());
			timerDataChild.addDuration(durationOfChild);
			timerDataChild.setExclusiveDuration(EXCLUSIVE_DURATION_OF_CHILD);
			timerDataChild.addCpuDuration(20);
			timerDataChild.calculateExclusiveMin(EXCLUSIVE_DURATION_OF_CHILD);
			childSequence.setTimerData(timerDataChild);
		}
		// Add exclusive duration
		durationOfParent += EXCLUSIVE_DURATION_OF_PARENT;

		parentSequence.getNestedSequences().addAll(nestedSequencesOfParent);
		parentSequence.setDuration(durationOfParent);

		// Create TimerData for parent
		TimerData timerDataParent = new TimerData(parentSequence.getTimeStamp(), parentSequence.getPlatformIdent(),
				parentSequence.getSensorTypeIdent(), parentSequence.getMethodIdent());
		timerDataParent.addDuration(durationOfParent);
		timerDataParent.setExclusiveDuration(EXCLUSIVE_DURATION_OF_PARENT);
		timerDataParent.addCpuDuration(CPU_DURATION);
		timerDataParent.calculateExclusiveMin(EXCLUSIVE_DURATION_OF_PARENT);
		parentSequence.setTimerData(timerDataParent);

		return parentSequence;
	}
}
