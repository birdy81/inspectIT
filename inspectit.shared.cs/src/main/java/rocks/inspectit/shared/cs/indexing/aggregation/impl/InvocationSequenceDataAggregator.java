package rocks.inspectit.shared.cs.indexing.aggregation.impl;

import org.apache.commons.math3.util.Pair;

import rocks.inspectit.shared.all.communication.IAggregatedData;
import rocks.inspectit.shared.all.communication.data.AggregatedHttpTimerData;
import rocks.inspectit.shared.all.communication.data.AggregatedInvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.AggregatedSqlStatementData;
import rocks.inspectit.shared.all.communication.data.AggregatedTimerData;
import rocks.inspectit.shared.all.communication.data.HttpTimerData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceDataHelper;
import rocks.inspectit.shared.all.communication.data.SqlStatementData;
import rocks.inspectit.shared.all.communication.data.TimerData;
import rocks.inspectit.shared.cs.indexing.aggregation.IAggregator;

/**
 * Aggregation for {@link InvocationSequenceData}.
 *
 * Note: When {@link InvocationSequenceData} elements are aggregated, then the invocation structure
 * is ignored for aggregation.
 *
 * @author Alexander Wert
 *
 */
public class InvocationSequenceDataAggregator implements IAggregator<InvocationSequenceData> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void aggregate(IAggregatedData<InvocationSequenceData> aggregatedObject, InvocationSequenceData objectToAdd) {
		if (aggregatedObject.getData().getMethodIdent() != objectToAdd.getMethodIdent()) {
			throw new IllegalArgumentException("Invalid data for aggregation!");
		}

		if (InvocationSequenceDataHelper.hasTimerData(aggregatedObject.getData())) {
			aggregatedObject.getData().getTimerData().aggregateTimerData(objectToAdd.getTimerData());
		} else {
			throw new IllegalArgumentException("No timer data available!");
		}

		aggregatedObject.aggregate(objectToAdd);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IAggregatedData<InvocationSequenceData> getClone(InvocationSequenceData invocationSequenceData) {
		AggregatedInvocationSequenceData clone = new AggregatedInvocationSequenceData();
		clone.setPlatformIdent(invocationSequenceData.getPlatformIdent());
		clone.setSensorTypeIdent(invocationSequenceData.getSensorTypeIdent());
		clone.setMethodIdent(invocationSequenceData.getMethodIdent());

		if (InvocationSequenceDataHelper.hasTimerData(invocationSequenceData)) {
			if (invocationSequenceData.getTimerData() instanceof SqlStatementData) {
				SqlStatementData sqlStatementData = invocationSequenceData.getSqlStatementData();
				SqlStatementData sqlStatementDataClone = new AggregatedSqlStatementData();
				sqlStatementDataClone.setPlatformIdent(sqlStatementData.getPlatformIdent());
				sqlStatementDataClone.setSensorTypeIdent(sqlStatementData.getSensorTypeIdent());
				sqlStatementDataClone.setPreparedStatement(sqlStatementData.isPreparedStatement());
				sqlStatementDataClone.setSql(sqlStatementData.getSql());
				sqlStatementDataClone.setDatabaseProductName(sqlStatementData.getDatabaseProductName());
				sqlStatementDataClone.setDatabaseProductVersion(sqlStatementData.getDatabaseProductVersion());
				sqlStatementDataClone.setDatabaseUrl(sqlStatementData.getDatabaseUrl());
				clone.setTimerData(sqlStatementDataClone);
			} else if (invocationSequenceData.getTimerData() instanceof HttpTimerData) {
				HttpTimerData httpTimerData = (HttpTimerData) invocationSequenceData.getTimerData();
				HttpTimerData httpTimerDataClone = new AggregatedHttpTimerData();
				httpTimerDataClone.setPlatformIdent(httpTimerData.getPlatformIdent());
				httpTimerDataClone.setSensorTypeIdent(httpTimerData.getSensorTypeIdent());
				httpTimerDataClone.setMethodIdent(httpTimerData.getMethodIdent());
				httpTimerDataClone.setCharting(httpTimerData.isCharting());
				httpTimerDataClone.setHttpInfo(httpTimerData.getHttpInfo());
				clone.setTimerData(httpTimerDataClone);
			} else {
				TimerData timerData = invocationSequenceData.getTimerData();
				TimerData timerDataClone = new AggregatedTimerData();
				timerDataClone.setPlatformIdent(timerData.getPlatformIdent());
				timerDataClone.setSensorTypeIdent(timerData.getSensorTypeIdent());
				timerDataClone.setMethodIdent(timerData.getMethodIdent());
				timerDataClone.setCharting(timerData.isCharting());
				clone.setTimerData(timerDataClone);
			}
		} else {
			throw new IllegalArgumentException("No timer data available!");
		}
		return clone;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getAggregationKey(InvocationSequenceData invocationSequenceData) {
		if (InvocationSequenceDataHelper.hasSQLData(invocationSequenceData)) {
			return new Pair<Long, String>(invocationSequenceData.getMethodIdent(), invocationSequenceData.getSqlStatementData().getSql());
		} else if (InvocationSequenceDataHelper.hasHttpTimerData(invocationSequenceData)) {
			return new Pair<Long, String>(invocationSequenceData.getMethodIdent(), ((HttpTimerData) invocationSequenceData.getTimerData()).getHttpInfo().getUri());
		} else {
			return invocationSequenceData.getMethodIdent();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		// we must make constant hashCode because of the caching
		result = (prime * result) + this.getClass().getName().hashCode();
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
		return true;
	}

}
