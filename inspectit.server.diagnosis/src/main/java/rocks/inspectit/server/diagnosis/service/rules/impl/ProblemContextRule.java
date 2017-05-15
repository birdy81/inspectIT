package rocks.inspectit.server.diagnosis.service.rules.impl;

import java.util.LinkedList;
import java.util.List;

import rocks.inspectit.server.diagnosis.engine.rule.annotation.Action;
import rocks.inspectit.server.diagnosis.engine.rule.annotation.Rule;
import rocks.inspectit.server.diagnosis.engine.rule.annotation.TagValue;
import rocks.inspectit.server.diagnosis.service.rules.InvocationSequenceDataIterator;
import rocks.inspectit.server.diagnosis.service.rules.RuleConstants;
import rocks.inspectit.shared.all.communication.data.AggregatedInvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.diagnosis.results.CauseCluster;

/**
 * Rule for detecting the <code>Problem Context</code> within an {@link InvocationSequenceData}. The
 * <code>Problem Context</code> is located between the <code>Global Context</code> and a
 * <code>Time Wasting Operation</code>. The <code>Problem Context</code> is the deepest node in the
 * invocation tree that subsumes one performance problem. This rule is triggered third in the rule
 * pipeline.
 *
 * @author Alexander Wert, Alper Hidiroglu
 *
 */
@Rule(name = "ProblemContextRule")
public class ProblemContextRule {

	/**
	 * Injection of the <code>Global Context</code>.
	 */
	@TagValue(type = RuleConstants.TAG_GLOBAL_CONTEXT)
	private InvocationSequenceData globalContext;

	/**
	 * Each <code>Time Wasting Operation</code> has exactly one corresponding <code>Problem
	 * Context</code>.
	 */
	@TagValue(type = RuleConstants.TAG_TIME_WASTING_OPERATIONS)
	private AggregatedInvocationSequenceData timeWastingOperation;

	/**
	 * Rule execution.
	 *
	 * @return TAG_PROBLEM_CONTEXT
	 */
	@Action(resultTag = RuleConstants.TAG_PROBLEM_CONTEXT)
	public CauseCluster action() {

		List<InvocationSequenceData> causeInvocations = timeWastingOperation.getRawInvocationsSequenceElements();

		if (causeInvocations.size() > 1) {
			double overallExclusiveDuration = 0.0;

			// Creates list with clusters. For each InvocationSequenceData in the Time Wasting
			// Operation a cluster is build initially. The exclusive times of
			// all InvocationSequenceData in the Time Wasting Operation are summed up. The
			// InvocationSequenceData are clustered until there is a cluster with a significant high
			// exclusive time.
			List<CauseCluster> causeClusters = new LinkedList<>();
			for (InvocationSequenceData invocation : causeInvocations) {
				causeClusters.add(new CauseCluster(invocation));
				overallExclusiveDuration += invocation.getTimerData().isExclusiveTimeDataAvailable() ? invocation.getTimerData().getExclusiveDuration() : 0.0;
			}
			// Checks if there is already a cluster with high ratio from
			// overallExclusiveDuration.
			CauseCluster significantCluster = getSignificantCluster(causeClusters, overallExclusiveDuration);

			// Iterates as long as there is no significantCluster.
			while (null == significantCluster) {
				calculateDistancesToNextCluster(causeClusters);
				causeClusters = mergeClusters(causeClusters);
				significantCluster = getSignificantCluster(causeClusters, overallExclusiveDuration);
			}

			// The Problem Context is the deepest node in the invocation tree that subsumes all
			// InvocationSequenceData the significant cluster holds.
			return significantCluster;

			// If there is only one element in the Time Wasting Operation and it
			// is the Global Context, then the Global Context is the Problem
			// Context. When it is not the Global Context, then the parent of
			// the only element is the Problem Context. If there is no parent,
			// then the element itself is the Problem Context.
		} else {
			return new CauseCluster(causeInvocations.get(0));
		}
	}

	/**
	 * Merges {@link #CauseCluster}.
	 *
	 * @param causeClusters
	 *            List with clusters.
	 * @return List with merged clusters.
	 */
	private List<CauseCluster> mergeClusters(List<CauseCluster> causeClusters) {
		boolean merged = false;
		int distance = 0;
		List<CauseCluster> newClusters = new LinkedList<>();
		List<CauseCluster> clustersToMerge = new LinkedList<>();
		while (!merged) {
			clustersToMerge.clear();
			newClusters.clear();
			for (CauseCluster cluster : causeClusters) {
				clustersToMerge.add(cluster);
				if (cluster.getDistanceToNextCluster() > distance) {
					if (clustersToMerge.size() > 1) {
						newClusters.add(new CauseCluster(clustersToMerge));
						merged = true;
					} else {
						newClusters.add(cluster);
					}
					clustersToMerge.clear();
				}
			}
			distance++;
		}
		return newClusters;
	}

	/**
	 * Identifies after each merge if there is a {@link #CauseCluster} with a significant high
	 * exclusive time. If so, the {@link #CauseCluster} is returned. Otherwise returns
	 * <code>null</code>.
	 *
	 * @param causeClusters
	 *            List with clusters.
	 * @param overallExclusiveDuration
	 *            The summed up exclusive time of all {@link InvocationSequenceData} the
	 *            <code>Time Wasting Operation</code> holds.
	 * @return Significant cluster.
	 */
	private CauseCluster getSignificantCluster(List<CauseCluster> causeClusters, double overallExclusiveDuration) {
		for (CauseCluster cluster : causeClusters) {
			double exclusiveDurationSum = 0.0;
			for (InvocationSequenceData invocation : cluster.getCauseInvocations()) {
				exclusiveDurationSum += invocation.getTimerData().isExclusiveTimeDataAvailable() ? invocation.getTimerData().getExclusiveDuration() : 0.0;
			}
			if (exclusiveDurationSum > (0.8 * overallExclusiveDuration)) {
				return cluster;
			}
		}
		return null;
	}

	/**
	 * Calculates for each {@link #CauseCluster} the distance to the next cluster. With the
	 * calculated distances it is decided which clusters will be merged.
	 *
	 * @param causeClusters
	 *            List with clusters from which the distances are calculated.
	 */
	private void calculateDistancesToNextCluster(List<CauseCluster> causeClusters) {

		int nextClusterIndex = 0;
		CauseCluster nextCluster = causeClusters.get(nextClusterIndex);
		CauseCluster currentCluster = null;
		// Starts from Global Context
		InvocationSequenceDataIterator iterator = new InvocationSequenceDataIterator(globalContext, true);

		int currentCauseDepth = -1;
		int minDepth = Integer.MAX_VALUE;
		InvocationSequenceData invocation;

		while (iterator.hasNext() && (nextClusterIndex < causeClusters.size())) {

			invocation = iterator.next();

			if (iterator.currentDepth() < minDepth) {
				minDepth = iterator.currentDepth();
			}

			if (nextCluster.getCommonContext() == invocation) {

				if (null != currentCluster) {
					int depthDistance = Math.max((currentCauseDepth - minDepth) + 1, 0);
					currentCluster.setDistanceToNextCluster(depthDistance);
				}

				currentCluster = nextCluster;
				nextClusterIndex++;
				if (nextClusterIndex < causeClusters.size()) {
					nextCluster = causeClusters.get(nextClusterIndex);
				}
				currentCauseDepth = iterator.currentDepth();
				// minDepth reset
				minDepth = Integer.MAX_VALUE;
			}
		}
		currentCluster.setDistanceToNextCluster(Integer.MAX_VALUE);
	}

}
