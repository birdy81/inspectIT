package rocks.inspectit.server.diagnosis.service.rules.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rocks.inspectit.server.diagnosis.engine.rule.annotation.Action;
import rocks.inspectit.server.diagnosis.engine.rule.annotation.Rule;
import rocks.inspectit.server.diagnosis.engine.rule.annotation.SessionVariable;
import rocks.inspectit.server.diagnosis.engine.rule.annotation.TagValue;
import rocks.inspectit.server.diagnosis.service.aggregation.AggregatedDiagnosisData;
import rocks.inspectit.server.diagnosis.service.aggregation.DiagnosisDataAggregationPerformer;
import rocks.inspectit.server.diagnosis.service.rules.RuleConstants;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.cs.communication.data.InvocationSequenceDataHelper;

/**
 * Rule for detecting <code>Time Wasting Operations</code> within an {@link InvocationSequenceData}.
 * The search starts from the <code>Global Context</code>. A <code>Time Wasting Operation</code> is
 * an {@link AggregatedDiagnosisData} that holds all methods with the same key which
 * together have a high exclusive time or simply put, are the biggest time waster. This rule is
 * triggered second in the rule pipeline.
 *
 * @author Alexander Wert, Alper Hidiroglu, Christian Voegele
 *
 */
@Rule(name = "TimeWastingOperationsRule")
public class TimeWastingOperationsRule {

	/**
	 * An {@link AggregatedDiagnosisData} is considered as a
	 * <code>Time Wasting Operation</code>, if the cumulative exclusive time of already found
	 * <code>Time Wasting Operations</code> is lower than 80 percent of the
	 * <code>Global Context's</code> duration.
	 */
	private static final Double PROPORTION = 0.8;

	/**
	 * An {@link AggregatedDiagnosisData} is considered as a
	 * <code>Time Wasting Operation</code>, if the cumulative exclusive time of already found
	 * <code>Time Wasting Operations</code> subtracted from the <code>Global Context's</code>
	 * duration is higher than the baseline (= 1000).
	 */
	@SessionVariable(name = RuleConstants.DIAGNOSIS_VAR_BASELINE, optional = false)
	private double baseline;

	/**
	 * The search for <code>Time Wasting Operations</code> starts from the
	 * <code>Global Context</code>.
	 */
	@TagValue(type = RuleConstants.DIAGNOSIS_TAG_GLOBAL_CONTEXT)
	private InvocationSequenceData globalContext;

	/**
	 * Rule execution.
	 *
	 * @return DIAGNOSIS_TAG_TIME_WASTING_OPERATIONS
	 */
	@Action(resultTag = RuleConstants.DIAGNOSIS_TAG_TIME_WASTING_OPERATIONS, resultQuantity = Action.Quantity.MULTIPLE)
	public List<AggregatedDiagnosisData> action() {

		List<InvocationSequenceData> invocationSequenceDataList = asInvocationSequenceDataList(Collections.singletonList(globalContext),
				new ArrayList<InvocationSequenceData>(globalContext.getNestedSequences().size()));

		// in case no timer are available, this should not happen. Just to be sure.
		if (invocationSequenceDataList.isEmpty()) {
			invocationSequenceDataList.add(globalContext);
		}

		DiagnosisDataAggregationPerformer diagnosisDataAggregationPerformer = new DiagnosisDataAggregationPerformer();

		// Aggregates all methods with the same key to an object of type
		// AggregatedInvocationSequenceData. Exclusive times are summed up.
		diagnosisDataAggregationPerformer.aggregateInvocationSequenceDataList(invocationSequenceDataList);
		List<AggregatedDiagnosisData> timeWastingOperationsTmp = diagnosisDataAggregationPerformer.getAggregationResultList();

		Collections.sort(timeWastingOperationsTmp, new Comparator<AggregatedDiagnosisData>() {
			/**
			 * Sorts list with aggregated {@link InvocationSequenceData} with the help of the summed
			 * up exclusive times.
			 */
			@Override
			public int compare(AggregatedDiagnosisData o1, AggregatedDiagnosisData o2) {
				return Double.compare(o2.getAggregatedDiagnosisTimerData().getExclusiveDuration(), o1.getAggregatedDiagnosisTimerData().getExclusiveDuration());
			}
		});

		// Only AggregatedInvocationSequenceData with highest exclusive
		// times are Time Wasting Operations
		List<AggregatedDiagnosisData> timeWastingOperations = new ArrayList<>();
		double sumExecTime = 0;
		for (AggregatedDiagnosisData aggregatedDiagnosisData : timeWastingOperationsTmp) {
			if ((sumExecTime < (PROPORTION * globalContext.getDuration())) || (aggregatedDiagnosisData.getAggregatedDiagnosisTimerData().getExclusiveDuration() > baseline)) {
				// increase sumExclusiveTime by duration of Time Wasting Operation.
				sumExecTime += aggregatedDiagnosisData.getAggregatedDiagnosisTimerData().getExclusiveDuration();
				timeWastingOperations.add(aggregatedDiagnosisData);
			}
		}

		return timeWastingOperations;
	}

	/**
	 * Saves beside the <code>Global Context</code> all reachable {@link InvocationSequenceData}
	 * from the <code>Global Context</code> in {@link resultList} when ExclusiveTimeData is
	 * available.
	 *
	 * @param invocationSequences
	 *            List that only holds the <code>Global Context</code>.
	 * @param resultList
	 *            List with <code>Global Context</code> and invocation sequences reachable from the
	 *            <code>Global Context</code>.
	 * @return List with {@link InvocationSequenceData}.
	 */
	private List<InvocationSequenceData> asInvocationSequenceDataList(List<InvocationSequenceData> invocationSequences, final List<InvocationSequenceData> resultList) {
		for (InvocationSequenceData invocationSequence : invocationSequences) {
			// Either timer data has to be available
			if (InvocationSequenceDataHelper.hasTimerData(invocationSequence) && invocationSequence.getTimerData().isExclusiveTimeDataAvailable()) {
				resultList.add(invocationSequence);
				// Or SQL statement data
			} else if (InvocationSequenceDataHelper.hasSQLData(invocationSequence) && invocationSequence.getSqlStatementData().isExclusiveTimeDataAvailable()) {
				resultList.add(invocationSequence);
			}
			asInvocationSequenceDataList(invocationSequence.getNestedSequences(), resultList);
		}
		return resultList;
	}
}
