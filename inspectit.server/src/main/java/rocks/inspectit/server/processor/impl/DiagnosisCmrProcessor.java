/**
 *
 */
package rocks.inspectit.server.processor.impl;

import java.util.Collection;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;

import rocks.inspectit.server.diagnosis.results.IDiagnosisResults;
import rocks.inspectit.server.diagnosis.service.IDiagnosisResultNotificationService;
import rocks.inspectit.server.diagnosis.service.IDiagnosisService;
import rocks.inspectit.server.diagnosis.service.rules.CachedDataMapper;
import rocks.inspectit.server.influx.builder.ProblemOccurrencePointBuilder;
import rocks.inspectit.server.processor.AbstractCmrDataProcessor;
import rocks.inspectit.shared.all.communication.DefaultData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.cs.communication.data.diagnosis.ProblemOccurrence;
import rocks.inspectit.shared.cs.communication.data.diagnosis.RootCause;

/**
 * This processor starts the {@link #diagnosisService} and stores the results in
 * {@link #diagnosisResults}.
 *
 * @author Claudio Waldvogel, Christian Voegele
 *
 */
public class DiagnosisCmrProcessor extends AbstractCmrDataProcessor implements IDiagnosisResultNotificationService {

	private int countProblems = 0;

	private int countToDiagnose = 0;

	/**
	 * Diagnosis service interface.
	 */
	@Autowired(required = false)
	private IDiagnosisService diagnosisService;

	/**
	 * Diagnosis service interface.
	 */
	@Autowired(required = false)
	private IDiagnosisResults<ProblemOccurrence> diagnosisResults;

	/**
	 * Needed to store the resulting ProblemOccurrence to influx.
	 */
	@Autowired
	ProblemOccurrencePointBuilder pb;

	/**
	 * Baseline value.
	 */
	private final double baseline;

	/**
	 * Basic constructor.
	 *
	 * @param baseline
	 *            The default baseline value defined in the configuration.
	 */
	public DiagnosisCmrProcessor(final double baseline) {
		this.baseline = baseline;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void processData(DefaultData defaultData, EntityManager entityManager) {
		diagnosisService.diagnose((InvocationSequenceData) defaultData, baseline);
		countToDiagnose++;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean canBeProcessed(DefaultData defaultData) {
		return (defaultData instanceof InvocationSequenceData) && (((InvocationSequenceData) defaultData).getDuration() > baseline);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onNewDiagnosisResult(ProblemOccurrence problemOccurrence) {
		diagnosisResults.getDiagnosisResults().add(problemOccurrence);
		pb.saveProblemOccurrenceToInflux(problemOccurrence);
		countProblems++;
		printProblemOccurence(problemOccurrence);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onNewDiagnosisResult(Collection<ProblemOccurrence> problemOccurrences) {
		diagnosisResults.getDiagnosisResults().addAll(problemOccurrences);
		for (ProblemOccurrence problemOccurrence : problemOccurrences) {
			pb.saveProblemOccurrenceToInflux(problemOccurrence);
		}
		countProblems = countProblems + problemOccurrences.size();
		for (ProblemOccurrence problemOccurrence : problemOccurrences) {
			printProblemOccurence(problemOccurrence);
		}
	}

	/* TODO: Remove */
	private void printProblemOccurence(ProblemOccurrence problemOccurrence) {
		System.out.println(" ------------------");
		System.out.println("Anzahl Probleme " + countProblems + " ToDiagnose " + countToDiagnose);
		System.out.println("+ " + CachedDataMapper.getInstance().getBusinessTransactionName(problemOccurrence.getBusinessTransactionNameIdent(), problemOccurrence.getApplicationNameIdent()));
		System.out.println("+ " + CachedDataMapper.getInstance().getApplicationName(problemOccurrence.getApplicationNameIdent()));
		System.out.println("+ " + problemOccurrence.getRequestRoot().getInvocationId());
		System.out.println("+ " + CachedDataMapper.getInstance().getFQMethodeName(problemOccurrence.getGlobalContext().getMethodIdent()));
		System.out.println("+ " + problemOccurrence.getGlobalContext().getTimerDataProblemOccurence().getExclusiveDuration());
		System.out.println("+ " + CachedDataMapper.getInstance().getFQMethodeName(problemOccurrence.getProblemContext().getMethodIdent()));
		System.out.println("+ " + problemOccurrence.getProblemContext().getTimerDataProblemOccurence().getExclusiveDuration());

		for (RootCause rootCause : problemOccurrence.getRootCauseList()) {
			System.out.println("+ " + CachedDataMapper.getInstance().getFQMethodeName(rootCause.getMethodIdent()));
			System.out.println("+ " + rootCause.getTimerDataProblemOccurence().getExclusiveDuration());
			System.out.println(" Average "
					+ ((rootCause.getTimerDataProblemOccurence().getExclusiveDuration() / rootCause.getTimerDataProblemOccurence().getExclusiveCount()) + " Count "
							+ rootCause.getTimerDataProblemOccurence().getExclusiveCount()));
			System.out.println("+ " + rootCause.getTimerDataProblemOccurence().getSql());
			System.out.println("+ " + rootCause.getTimerDataProblemOccurence().getUri());
		}

		System.out.println("+ " + problemOccurrence.getCauseStructure().getCauseType() + " " + problemOccurrence.getCauseStructure().getSourceType());
	}

}