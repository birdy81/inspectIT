package rocks.inspectit.server.diagnosis.service.rules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import rocks.inspectit.shared.all.cmr.model.MethodIdent;
import rocks.inspectit.shared.all.cmr.service.ICachedDataService;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.cmr.ApplicationData;
import rocks.inspectit.shared.all.communication.data.cmr.BusinessTransactionData;

/**
 * This class uses the {@link #ICachedDataService} to get names of BusinessTransactions, Methods and
 * Applications.
 *
 * @author Alexander Wert
 *
 */
@Component
@Scope(value = "singleton")
public class CachedDataMapper {

	/**
	 * This instance.
	 */
	private static CachedDataMapper instance;

	/**
	 * Default constructor.
	 */
	public CachedDataMapper() {
		setInstance(this);
	}

	/**
	 * @return CachedDataMapper
	 */
	public static CachedDataMapper getInstance() {
		return instance;
	}

	/**
	 * Provides platform, sensor, method identification and business context from the cache.
	 */
	@Autowired
	ICachedDataService cachedDataService;

	/**
	 * @param isd
	 *            InvocationSequenceData the methode name should be identified
	 * @return Fully qualified name of method
	 */
	public String getFQMethodeName(final InvocationSequenceData isd) {
		MethodIdent methodIdent = cachedDataService.getMethodIdentForId(isd.getMethodIdent());
		return methodIdent.getFQN() + "." + methodIdent.getMethodName();
	}

	/**
	 * @param isd
	 *            InvocationSequenceData the business transaction name should be identified
	 * @return name of business transaction
	 */
	public String getBusinessTransactionName(final InvocationSequenceData isd) {
		BusinessTransactionData businessTransactionData = cachedDataService.getBusinessTransactionForId(isd.getApplicationId(), isd.getBusinessTransactionId());
		return businessTransactionData.getName();
	}

	/**
	 * @param isd
	 *            InvocationSequenceData the application name should be identified
	 * @return name of application
	 */
	public String getApplicationName(final InvocationSequenceData isd) {
		ApplicationData applicationData = cachedDataService.getApplicationForId(isd.getApplicationId());
		return applicationData.getName();
	}

	public String getFQMethodeName(final long methodId) {
		MethodIdent methodIdent = cachedDataService.getMethodIdentForId(methodId);
		return methodIdent.getFQN() + "." + methodIdent.getMethodName();
	}

	public String getBusinessTransactionName(final int businessTransactionID, final int applicationID) {
		BusinessTransactionData businessTransactionData = cachedDataService.getBusinessTransactionForId(applicationID, businessTransactionID);
		return businessTransactionData.getName();
	}

	public String getApplicationName(final int applicationID) {
		ApplicationData applicationData = cachedDataService.getApplicationForId(applicationID);
		return applicationData.getName();
	}

	/**
	 * Sets {@link #instance}.
	 *
	 * @param instance
	 *            New value for {@link #instance}
	 */
	public static void setInstance(CachedDataMapper instance) {
		CachedDataMapper.instance = instance;
	}

}