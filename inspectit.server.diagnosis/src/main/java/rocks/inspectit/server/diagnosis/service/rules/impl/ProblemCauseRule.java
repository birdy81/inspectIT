package rocks.inspectit.server.diagnosis.service.rules.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import rocks.inspectit.server.diagnosis.engine.rule.annotation.Action;
import rocks.inspectit.server.diagnosis.engine.rule.annotation.Rule;
import rocks.inspectit.server.diagnosis.engine.rule.annotation.TagValue;
import rocks.inspectit.server.diagnosis.service.rules.RuleConstants;
import rocks.inspectit.shared.all.communication.data.AggregatedInvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceDataHelper;
import rocks.inspectit.shared.cs.indexing.aggregation.impl.InvocationSequenceDataAggregator;

/**
 * Rule for detecting <code>Root Causes</code> within an {@link InvocationSequenceData}. One
 * <code>Root Cause</code> is a method that characterizes a performance problem, hence, whose
 * exclusive time is very high. The <code>Root Causes</code> are aggregated to an object. This rule
 * is triggered fourth in the rule pipeline.
 *
 * @author Alexander Wert, Alper Hidiroglu
 *
 */
@Rule(name = "ProblemCauseRule")
public class ProblemCauseRule {

	/**
	 * Proportion value for comparison.
	 */
	private static final Double PROPORTION = 0.8;

	/**
	 * <code>Root Cause</code> candidates had to be in a <code>Time Wasting Operation</code> object.
	 */
	@TagValue(type = RuleConstants.TAG_TIME_WASTING_OPERATIONS)
	private AggregatedInvocationSequenceData timeWastingOperation;

	/**
	 * Injection of the <code>Problem Context</code>.
	 */
	@TagValue(type = RuleConstants.TAG_PROBLEM_CONTEXT)
	private InvocationSequenceData problemContext;

	/**
	 * Rule execution.
	 *
	 * @return TAG_PROBLEM_CAUSE
	 */
	@Action(resultTag = RuleConstants.TAG_PROBLEM_CAUSE)
	public AggregatedInvocationSequenceData action() {

		/**
		 * Holds all reachable {@link #InvocationSequenceData} from the <code>Problem Context</code>
		 * that were in the <code>Time Wasting Operation</code>.
		 */
		List<InvocationSequenceData> causeCandidates = asInvocationSequenceDataList(
				Collections.singletonList(problemContext), new ArrayList<InvocationSequenceData>());
		Collections.sort(causeCandidates, new Comparator<InvocationSequenceData>() {

			/**
			 * Sorts causeCandidates with the help of their exclusive times.
			 */
			@Override
			public int compare(InvocationSequenceData o1, InvocationSequenceData o2) {
				return Double.compare(InvocationSequenceDataHelper.calculateExclusiveTime(o2),
						InvocationSequenceDataHelper.calculateExclusiveTime(o1));
			}
		});

		double sumExclusiveTime = 0.0;
		int i = 0;
		InvocationSequenceDataAggregator aggregator = new InvocationSequenceDataAggregator();
		AggregatedInvocationSequenceData rootCause = null;

		// Root Cause candidates with highest exclusive times are aggregated.
		while ((sumExclusiveTime < (PROPORTION * InvocationSequenceDataHelper.calculateDuration(problemContext)))
				&& (i < causeCandidates.size())) {

			InvocationSequenceData invocation = causeCandidates.get(i);
			if (null == rootCause) {
				rootCause = (AggregatedInvocationSequenceData) aggregator.getClone(invocation);
			}
			aggregator.aggregate(rootCause, invocation);
			sumExclusiveTime += InvocationSequenceDataHelper.calculateDuration(invocation);
			i++;
		}

		// Three-Sigma Limit approach for further aggregation.
		if (i > 1) {
			double mean = sumExclusiveTime / i;
			double[] durations = new double[rootCause.size()];
			int j = 0;
			for (InvocationSequenceData invocation : rootCause.getRawInvocationsSequenceElements()) {
				durations[j] = InvocationSequenceDataHelper.calculateDuration(invocation);
				j++;
			}

			StandardDeviation standardDeviation = new StandardDeviation(false);
			double sd = standardDeviation.evaluate(durations, mean);
			double lowerThreshold = mean - (3 * sd);

			for (int k = i; k < causeCandidates.size(); k++) {
				InvocationSequenceData invocation = causeCandidates.get(k);
				double duration = InvocationSequenceDataHelper.calculateDuration(invocation);
				if (duration > lowerThreshold) {
					aggregator.aggregate(rootCause, invocation);
				} else {
					break;
				}
			}
		}

		return rootCause;
	}

	/**
	 * All {@link #InvocationSequenceData} that were in the <code>Time Wasting
	 * Operation</code> are possible cause candidates and are saved to this list. Starts at the
	 * <code>Problem Context</code>.
	 *
	 * @param invocationSequences
	 *            List that only holds the <code>Problem Context</code>.
	 * @param resultList
	 *            List that holds all {@link InvocationSequenceData} that are reachable from the
	 *            <code>Problem Context</code> and that were in the
	 *            <code>Time Wasting Operation</code>.
	 * @return List with {@link InvocationSequenceData}.
	 */
	private List<InvocationSequenceData> asInvocationSequenceDataList(List<InvocationSequenceData> invocationSequences,
			final List<InvocationSequenceData> resultList) {
		for (InvocationSequenceData invocationSequence : invocationSequences) {
			if ((invocationSequence.getMethodIdent() == timeWastingOperation.getMethodIdent())
					&& (InvocationSequenceDataHelper.calculateExclusiveTime(invocationSequence) > 0.0)) {
				resultList.add(invocationSequence);
			}
			asInvocationSequenceDataList(invocationSequence.getNestedSequences(), resultList);
		}

		return resultList;
	}

}
