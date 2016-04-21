package rocks.inspectit.server.diagnosis.service.rules.impl;

import java.util.Stack;

import rocks.inspectit.server.diagnosis.engine.rule.annotation.Action;
import rocks.inspectit.server.diagnosis.engine.rule.annotation.Rule;
import rocks.inspectit.server.diagnosis.engine.rule.annotation.TagValue;
import rocks.inspectit.server.diagnosis.service.aggregation.AggregatedDiagnosisInvocationData;
import rocks.inspectit.server.diagnosis.service.data.CauseCluster;
import rocks.inspectit.server.diagnosis.service.rules.InvocationSequenceDataIterator;
import rocks.inspectit.server.diagnosis.service.rules.RuleConstants;
import rocks.inspectit.server.diagnosis.service.rules.RuleHelper;
import rocks.inspectit.shared.all.communication.data.HttpTimerData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.SqlStatementData;
import rocks.inspectit.shared.cs.communication.data.InvocationSequenceDataHelper;
import rocks.inspectit.shared.cs.communication.data.diagnosis.CauseStructure;
import rocks.inspectit.shared.cs.communication.data.diagnosis.CauseStructure.CauseType;

/**
 * This rule investigates if the <code>Root Cause</code> methods are called iterative or recursive.
 * This rule is triggered fifth and last in the rule pipeline.
 *
 * @author Alexander Wert, Alper Hidiroglu
 *
 */
@Rule(name = "CauseStructureRule")
public class CauseStructureRule {

	/**
	 * Max number of calls that should be checked for recursion.
	 */
	private static final int MAX_CALLS_TO_CHECK = 100;

	/**
	 * Injection of the <code>Root Causes</code>.
	 */
	@TagValue(type = RuleConstants.DIAGNOSIS_TAG_PROBLEM_CAUSE)
	private AggregatedDiagnosisInvocationData rootCause;

	/**
	 * Injection of the <code>Problem Context</code>.
	 */
	@TagValue(type = RuleConstants.DIAGNOSIS_TAG_PROBLEM_CONTEXT)
	private CauseCluster problemContext;

	/**
	 * Rule execution.
	 *
	 * @return DIAGNOSIS_TAG_CAUSE_STRUCTURE
	 */
	@Action(resultTag = RuleConstants.DIAGNOSIS_TAG_CAUSE_STRUCTURE)
	public CauseStructure action() {

		// In case there is just one Root Cause method.
		if (rootCause.size() == 1) {
			return new CauseStructure(CauseType.SINGLE, RuleHelper.getSourceType(rootCause));
		}

		// The Root Causes can only be in the invocation tree with the Problem Context as root node.
		InvocationSequenceDataIterator iterator = new InvocationSequenceDataIterator(problemContext.getCommonContext());

		// Checks if a Root Cause method is called by another Root Cause method. If so, there is
		// recursion.
		Stack<Integer> recursionStack = new Stack<>();
		int maxRecursionDepth = 0;
		int maxIterationsToCheck = 0;
		while (iterator.hasNext() && (maxIterationsToCheck < MAX_CALLS_TO_CHECK) && (maxRecursionDepth < 2)) {
			InvocationSequenceData invocation = iterator.next();
			if (!recursionStack.isEmpty() && (recursionStack.peek() >= iterator.currentDepth())) {
				recursionStack.pop();
			}

			if (isCauseInvocation(invocation)) {
				recursionStack.push(iterator.currentDepth());
				if (recursionStack.size() > maxRecursionDepth) {
					maxRecursionDepth = recursionStack.size();
				}
				maxIterationsToCheck++;
			}
		}

		// The Root Causes are called either recursive
		if (maxRecursionDepth > 1) {
			return new CauseStructure(CauseType.RECURSIVE, RuleHelper.getSourceType(rootCause));
			// or iterative.
		} else {
			return new CauseStructure(CauseType.ITERATIVE, RuleHelper.getSourceType(rootCause));
		}
	}

	/**
	 * Checks whether the passed {@link #InvocationSequenceData} is a <code>Root
	 * Cause</code>.
	 *
	 * @param invocation
	 *            The {@link InvocationSequenceData} that is investigated.
	 * @return Whether the {@link InvocationSequenceData} is a <code>Root Cause</code>.
	 */
	private boolean isCauseInvocation(InvocationSequenceData invocation) {
		if (InvocationSequenceDataHelper.hasSQLData(invocation) && InvocationSequenceDataHelper.hasSQLData(rootCause)) {
			SqlStatementData sqlStatementDataInvocation = invocation.getSqlStatementData();
			SqlStatementData sqlStatementDataCause = rootCause.getSqlStatementData();
			return (invocation.getMethodIdent() == rootCause.getMethodIdent())
					&& sqlStatementDataInvocation.getSql().equals(sqlStatementDataCause.getSql());
		} else if (InvocationSequenceDataHelper.hasHttpTimerData(invocation) && InvocationSequenceDataHelper.hasHttpTimerData(rootCause)) {
			HttpTimerData httpTimerDataInvocation = (HttpTimerData) invocation.getTimerData();
			HttpTimerData httpTimerDataCause = (HttpTimerData) invocation.getTimerData();
			return (invocation.getMethodIdent() == rootCause.getMethodIdent())
					&& httpTimerDataInvocation.getHttpInfo().getUri().equals(httpTimerDataCause.getHttpInfo().getUri());
		} else if (InvocationSequenceDataHelper.hasTimerData(invocation)) {
			return invocation.getMethodIdent() == rootCause.getMethodIdent();
		}
		return false;
	}

}
