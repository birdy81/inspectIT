package rocks.inspectit.server.diagnosis.service.rules.impl;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import rocks.inspectit.server.diagnosis.engine.rule.annotation.Action;
import rocks.inspectit.server.diagnosis.engine.rule.annotation.Rule;
import rocks.inspectit.server.diagnosis.engine.rule.annotation.TagValue;
import rocks.inspectit.server.diagnosis.service.rules.RuleConstants;
import rocks.inspectit.shared.all.communication.data.AggregatedInvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceDataHelper;
import rocks.inspectit.shared.all.communication.data.diagnosis.results.CauseCluster;
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
	 * Injection of the <code>Problem Context</code>.
	 */
	@TagValue(type = RuleConstants.TAG_PROBLEM_CONTEXT)
	private CauseCluster problemContext;

	/**
	 * Rule execution.
	 *
	 * @return TAG_PROBLEM_CAUSE
	 */
	@Action(resultTag = RuleConstants.TAG_PROBLEM_CAUSE)
	public AggregatedInvocationSequenceData action() {

		/**
		 * Holds all reachable {@link #InvocationSequenceData} from the <code>Problem Context</code>
		 * that were in the <code>Time Wasting Operation</code>. We do not sort the causeCandidates
		 * for reasons of performance .
		 */
		List<InvocationSequenceData> causeCandidates = problemContext.getCauseInvocations();
		double sumExclusiveTime = 0.0;
		int i = 0;
		InvocationSequenceDataAggregator aggregator = new InvocationSequenceDataAggregator();
		AggregatedInvocationSequenceData rootCause = null;

		// Root Cause candidates with highest exclusive times are aggregated.
		while ((sumExclusiveTime < (PROPORTION * InvocationSequenceDataHelper.calculateDuration(problemContext.getCommonContext())))
				&& (i < causeCandidates.size())) {
			InvocationSequenceData invocation = causeCandidates.get(i);
			if (null == rootCause) {
				rootCause = (AggregatedInvocationSequenceData) aggregator.getClone(invocation);
			}
			aggregator.aggregate(rootCause, invocation);
			sumExclusiveTime += InvocationSequenceDataHelper.calculateExclusiveTime(invocation);
			i++;
		}

		// Three-Sigma Limit approach for further aggregation.
		if ((i > 1) && (i < causeCandidates.size())) {
			double mean = sumExclusiveTime / i;
			double[] durations = new double[rootCause.size()];
			int j = 0;
			for (InvocationSequenceData invocation : rootCause.getRawInvocationsSequenceElements()) {
				durations[j] = InvocationSequenceDataHelper.calculateExclusiveTime(invocation);
				j++;
			}

			StandardDeviation standardDeviation = new StandardDeviation(false);
			double sd = standardDeviation.evaluate(durations, mean);
			double lowerThreshold = mean - (3 * sd);

			for (int k = i; k < causeCandidates.size(); k++) {
				InvocationSequenceData invocation = causeCandidates.get(k);
				double duration = InvocationSequenceDataHelper.calculateExclusiveTime(invocation);
				if (duration > lowerThreshold) {
					aggregator.aggregate(rootCause, invocation);
				} else {
					break;
				}
			}
		}

		return rootCause;
	}

}
