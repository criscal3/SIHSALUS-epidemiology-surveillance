package org.openmrs.module.sihsalusepidemiologicalsurveillance;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;
import org.openmrs.module.sihsalusepidemiologicalsurveillance.api.EndemicChannel;

public class EndemicChannelTest {
	
	private final EndemicChannel channel = new EndemicChannel();
	
	@Test
	public void interpolatesQuartiles() {
		assertEquals(2.5, channel.quantile(Arrays.asList(0, 10), .25), .00001);
		assertEquals(15, channel.quantile(Arrays.asList(20, 0, 10, 30), .5), .00001);
	}
	
	@Test
	public void classifiesAllFourZones() {
		List<Integer> h = Arrays.asList(4, 8, 12, 16, 20);
		assertEquals("SUCCESS", channel.evaluate(h, 7, 3).zone);
		assertEquals("SAFETY", channel.evaluate(h, 8, 3).zone);
		assertEquals("ALERT", channel.evaluate(h, 12, 3).zone);
		assertEquals("ALERT", channel.evaluate(h, 16, 3).zone);
		assertEquals("EPIDEMIC", channel.evaluate(h, 17, 3).zone);
	}
	
	@Test
	public void missingHistoryIsNotZero() {
		assertEquals("INSUFFICIENT_HISTORY", channel.evaluate(Arrays.asList(0, 0), 20, 3).zone);
		assertNull(channel.evaluate(Collections.<Integer> emptyList(), 0, 3).q3);
	}
	
	@Test
	public void zeroBaselineIsHandled() {
		assertEquals("SUCCESS", channel.evaluate(Arrays.asList(0, 0, 0), 0, 3).zone);
		assertEquals("EPIDEMIC", channel.evaluate(Arrays.asList(0, 0, 0), 1, 3).zone);
	}
}
