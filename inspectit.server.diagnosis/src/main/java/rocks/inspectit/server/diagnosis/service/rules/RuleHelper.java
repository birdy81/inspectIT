package rocks.inspectit.server.diagnosis.service.rules;

import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.cs.communication.data.InvocationSequenceDataHelper;
import rocks.inspectit.shared.cs.communication.data.diagnosis.CauseStructure.SourceType;

/**
 * Helper class for the rules. Methods which are reused by multiple rules can be found here.
 *
 * @author Christian Voegele
 *
 */
public final class RuleHelper {

	/**
	 * Private constructor for utility class.
	 */
	private RuleHelper() {
	}

	/**
	 * Determines the type of calls of the rootCause.
	 *
	 * @param data
	 *            InvocationSequenceData the SourceType should be determined
	 * @return type of calls
	 */
	public static SourceType getSourceType(InvocationSequenceData data) {
		if (InvocationSequenceDataHelper.hasSQLData(data)) {
			return SourceType.DATABASE;
		} else if (InvocationSequenceDataHelper.hasHttpTimerData(data)) {
			return SourceType.HTTP;
		} else {
			return SourceType.TIMERDATA;
		}
	}

}
