package rocks.inspectit.server.diagnosis.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import rocks.inspectit.server.diagnosis.engine.session.ISessionResultCollector;
import rocks.inspectit.server.diagnosis.engine.session.SessionContext;
import rocks.inspectit.server.diagnosis.engine.tag.Tag;
import rocks.inspectit.server.diagnosis.engine.tag.TagState;
import rocks.inspectit.server.diagnosis.service.aggregation.AggregatedDiagnosisInvocationData;
import rocks.inspectit.server.diagnosis.service.data.CauseCluster;
import rocks.inspectit.server.diagnosis.service.rules.RuleConstants;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.cs.communication.data.InvocationSequenceDataHelper;
import rocks.inspectit.shared.cs.communication.data.diagnosis.CauseStructure;
import rocks.inspectit.shared.cs.communication.data.diagnosis.ProblemOccurrence;
import rocks.inspectit.shared.cs.communication.data.diagnosis.RootCause;

/**
 * This class collects the results of the diagnosis engine and converts the results in a List of
 * {@link #ProblemOccurrence}. This collector expects the results of a certain structure of the
 * rules following the logic GlobalContext, ProblemContext, RootCause, and CauseStructure. In case
 * other Rules are used this collector must be adapted.
 *
 * @author Alexander Wert
 *
 */
@Component
public class ProblemOccurenceResultCollector implements ISessionResultCollector<InvocationSequenceData, List<ProblemOccurrence>> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ProblemOccurrence> collect(SessionContext<InvocationSequenceData> sessionContext) {
		List<ProblemOccurrence> problems = new ArrayList<>();
		InvocationSequenceData inputInvocationSequence = sessionContext.getInput();
		Collection<Tag> leafTags = sessionContext.getStorage().mapTags(TagState.LEAF).values();
		for (Tag leafTag : leafTags) {
			InvocationSequenceData globalContext = getGlobalContext(leafTag);
			CauseCluster problemContext = getProblemContext(leafTag);
			List<AggregatedDiagnosisInvocationData> rootCauseInvocations = getRootCauseInvocations(leafTag);
			CauseStructure causeStructure = getCauseStructure(leafTag);
			List<RootCause> rootCauseList = new ArrayList<RootCause>();
			for (AggregatedDiagnosisInvocationData aggregatedDiagnosisInvocationData : rootCauseInvocations) {
				RootCause rootCause = new RootCause(aggregatedDiagnosisInvocationData.getMethodIdent(), InvocationSequenceDataHelper.getTimerOrSQLData(aggregatedDiagnosisInvocationData));
				rootCauseList.add(rootCause);
			}

			// create new ProblemOccurrence
			ProblemOccurrence problem = new ProblemOccurrence(inputInvocationSequence, InvocationSequenceDataHelper.calculateDuration(inputInvocationSequence), globalContext,
					problemContext.getCommonContext(), rootCauseList, causeStructure);
			problems.add(problem);
		}

		return problems;
	}

	/**
	 * Returns the InvocationSequenceData of GlobalContext Tag.
	 *
	 * @param leafTag
	 *            leafTag for which the InvocationSequenceData should be returned
	 * @return InvocationSequenceData of GlobalContext
	 */
	private InvocationSequenceData getGlobalContext(Tag leafTag) {
		while (null != leafTag) {
			if (leafTag.getType().equals(RuleConstants.DIAGNOSIS_TAG_GLOBAL_CONTEXT)) {

				if (leafTag.getValue() instanceof InvocationSequenceData) {
					return (InvocationSequenceData) leafTag.getValue();
				} else {
					throw new RuntimeException("Global context has wrong datatype!");
				}

			}
			leafTag = leafTag.getParent();
		}

		throw new RuntimeException("Global context could not be found!");
	}

	/**
	 * Returns the InvocationSequenceData of ProblemContext Tag.
	 *
	 * @param leafTag
	 *            leafTag for which the InvocationSequenceData should be returned
	 * @return InvocationSequenceData of ProblemContext
	 */
	private CauseCluster getProblemContext(Tag leafTag) {
		while (null != leafTag) {
			if (leafTag.getType().equals(RuleConstants.DIAGNOSIS_TAG_PROBLEM_CONTEXT)) {

				if (leafTag.getValue() instanceof CauseCluster) {
					return (CauseCluster) leafTag.getValue();
				} else {
					throw new RuntimeException("Problem context has wrong datatype!");
				}

			}
			leafTag = leafTag.getParent();
		}

		throw new RuntimeException("Problem context could not be found!");
	}

	/**
	 * Returns the AggregatedInvocationSequenceData of RootCauseInvocations Tag.
	 *
	 * @param leafTag
	 *            leafTag for which the AggregatedInvocationSequenceData should be returned
	 * @return AggregatedInvocationSequenceData of RootCauseInvocations
	 */
	@SuppressWarnings("unchecked")
	private List<AggregatedDiagnosisInvocationData> getRootCauseInvocations(Tag leafTag) {
		while (null != leafTag) {
			if (leafTag.getType().equals(RuleConstants.DIAGNOSIS_TAG_PROBLEM_CAUSE)) {

				if (leafTag.getValue() instanceof AggregatedDiagnosisInvocationData) {
					return Collections.singletonList((AggregatedDiagnosisInvocationData) leafTag.getValue());
				} else {
					throw new RuntimeException("Problem cause has wrong datatype!");
				}
			} else if (leafTag.getType().equals(RuleConstants.DIAGNOSIS_TAG_PROBLEM_CAUSE_NPLUSONE_DATABASE)) {

				if (leafTag.getValue() instanceof List<?>) {
					return (List<AggregatedDiagnosisInvocationData>) leafTag.getValue();
				} else {
					throw new RuntimeException("Problem cause has wrong datatype!");
				}
			}

			leafTag = leafTag.getParent();
		}

		throw new RuntimeException("Problem root cause could not be found!");
	}

	/**
	 * Returns the CauseStructure of the CauseStructure Tag.
	 *
	 * @param leafTag
	 *            leafTag for which the CauseStructure should be returned
	 * @return CauseStructure of leafTag
	 */
	private CauseStructure getCauseStructure(Tag leafTag) {
		while (null != leafTag) {
			if (leafTag.getType().equals(RuleConstants.DIAGNOSIS_TAG_CAUSE_STRUCTURE)) {
				if (leafTag.getValue() instanceof CauseStructure) {
					return (CauseStructure) leafTag.getValue();
				} else {
					throw new RuntimeException("Cause structure has wrong datatype!");
				}

			}
			leafTag = leafTag.getParent();
		}

		throw new RuntimeException("Cause structure could not be found!");
	}
}