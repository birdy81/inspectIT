package rocks.inspectit.shared.cs.communication.data.diagnosis;

import org.codehaus.jackson.annotate.JsonProperty;

import rocks.inspectit.shared.all.communication.data.TimerData;

/**
 * This class represents a rootCause of a problem. The rootCause consists of a methodName, the timer
 * of the methodName and the underlying MethodCalls and TimerData.
 *
 * @author Alexander Wert, Christian Voegele
 *
 */
public class RootCause {

	/**
	 * The identification of the root cause method.
	 */
	@JsonProperty(value = "methodIdent")
	private long methodIdent;

	/**
	 * Relevant timerData of RootCause.
	 */
	@JsonProperty(value = "timerDataProblemOccurence")
	private TimerDataProblemOccurrence timerDataProblemOccurence;

	/**
	 * This constructor creates new RootCause.
	 *
	 * @param methodIdent
	 *            The identification of the root cause method
	 * @param timerData
	 *            the timerData of the invocationSequenceData of the root Cause
	 */
	public RootCause(final long methodIdent, final TimerData timerData) {
		this.methodIdent = methodIdent;
		this.timerDataProblemOccurence = new TimerDataProblemOccurrence(timerData);
	}

	/**
	 * Gets {@link #methodIdent}.
	 *
	 * @return {@link #methodIdent}
	 */
	public final long getMethodIdent() {
		return this.methodIdent;
	}

	/**
	 * Sets {@link #methodIdent}.
	 *
	 * @param methodIdent
	 *            New value for {@link #methodIdent}
	 */
	public final void setMethodIdent(long methodIdent) {
		this.methodIdent = methodIdent;
	}

	/**
	 * Gets {@link #timerDataProblemOccurence}.
	 *
	 * @return {@link #timerDataProblemOccurence}
	 */
	public final TimerDataProblemOccurrence getTimerDataProblemOccurence() {
		return this.timerDataProblemOccurence;
	}

	/**
	 * Sets {@link #timerDataProblemOccurence}.
	 *
	 * @param timerDataProblemOccurence
	 *            New value for {@link #timerDataProblemOccurence}
	 */
	public final void setTimerDataProblemOccurence(TimerDataProblemOccurrence timerDataProblemOccurence) {
		this.timerDataProblemOccurence = timerDataProblemOccurence;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + (int) (this.methodIdent ^ (this.methodIdent >>> 32));
		result = (prime * result) + ((this.timerDataProblemOccurence == null) ? 0 : this.timerDataProblemOccurence.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RootCause other = (RootCause) obj;
		if (this.methodIdent != other.methodIdent) {
			return false;
		}
		if (this.timerDataProblemOccurence == null) {
			if (other.timerDataProblemOccurence != null) {
				return false;
			}
		} else if (!this.timerDataProblemOccurence.equals(other.timerDataProblemOccurence)) {
			return false;
		}
		return true;
	}

}