package rocks.inspectit.server.diagnosis.service.rules.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.testbase.TestBase;

public class GlobalContextRuleTest extends TestBase {

	@InjectMocks
	GlobalContextRule globalContextRule;

	@Mock
	InvocationSequenceData invocationSequenceRoot;


	public static class Action extends GlobalContextRuleTest {

		private static final double BASELINE = 1000d;
		private static final double HIGH_DURATION = 4700d;
		private static final long METHOD_IDENT = 108L;
		private static final Timestamp DEF_DATE = new Timestamp(new Date().getTime());
		private static final Random RANDOM = new Random();
		private static final long PLATFORM_IDENT = RANDOM.nextLong();
		private static final long SENSOR_TYPE_IDENT = RANDOM.nextLong();

		@BeforeMethod
		private void init() {
			try {
				Field field = GlobalContextRule.class.getDeclaredField("baseline");
				field.setAccessible(true);
				field.set(globalContextRule, BASELINE);
			} catch (NoSuchFieldException | IllegalArgumentException | SecurityException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		@Test
		private void currentGlobalContextRuleMustNotBeNull() {
			when(invocationSequenceRoot.getDuration()).thenReturn(HIGH_DURATION);

			InvocationSequenceData currentGlobalContextRule = globalContextRule.action();

			assertThat("Invocation sequence root must not be null", currentGlobalContextRule, notNullValue());
		}

		@Test
		private void currentGlobalContextRuleMustBeTheSequenceWithMaximumDuration() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<>();
			InvocationSequenceData firstChildSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			firstChildSequence.setDuration(200d);
			InvocationSequenceData higherDurationChild = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			higherDurationChild.setDuration(4000d);
			InvocationSequenceData secondChildSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			secondChildSequence.setDuration(500d);
			nestedSequences.add(firstChildSequence);
			nestedSequences.add(higherDurationChild);
			nestedSequences.add(secondChildSequence);
			when(invocationSequenceRoot.getDuration()).thenReturn(HIGH_DURATION);
			when(invocationSequenceRoot.getNestedSequences()).thenReturn(nestedSequences);

			InvocationSequenceData currentGlobalContextRule = globalContextRule.action();

			assertThat("The returned global context rule must be the child with higher duration", currentGlobalContextRule, is(higherDurationChild));
		}

		@Test
		private void currentGlobalContextRuleMustNotBeTheSequenceWithMaximumDuration() {
			List<InvocationSequenceData> nestedSequences = new ArrayList<>();
			InvocationSequenceData firstChildSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			firstChildSequence.setDuration(200d);
			InvocationSequenceData higherDurationChild = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			higherDurationChild.setDuration(3000d);
			InvocationSequenceData secondChildSequence = new InvocationSequenceData(DEF_DATE, PLATFORM_IDENT, SENSOR_TYPE_IDENT, METHOD_IDENT);
			secondChildSequence.setDuration(500d);
			nestedSequences.add(firstChildSequence);
			nestedSequences.add(higherDurationChild);
			nestedSequences.add(secondChildSequence);
			when(invocationSequenceRoot.getDuration()).thenReturn(HIGH_DURATION);
			when(invocationSequenceRoot.getNestedSequences()).thenReturn(nestedSequences);

			InvocationSequenceData currentGlobalContextRule = globalContextRule.action();

			assertThat("The returned global context rule must be the child with higher duration", currentGlobalContextRule, not(is(higherDurationChild)));
		}
	}
}
