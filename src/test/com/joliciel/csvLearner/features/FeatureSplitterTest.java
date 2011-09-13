package com.joliciel.csvLearner.features;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.joliciel.csvLearner.NameValuePair;

public class FeatureSplitterTest {
	private static final Log LOG = LogFactory.getLog(FeatureSplitterTest.class);

	@Test
	public void testSplit() {
		InformationGainSplitter splitter = new InformationGainSplitter();
		splitter.setInformationGainThreshold(0.01);
		
		List<NameValuePair> weightedOutcomes = new Vector<NameValuePair>();
		weightedOutcomes.add(new NameValuePair("A", 1));
		weightedOutcomes.add(new NameValuePair("B", 2));
		weightedOutcomes.add(new NameValuePair("B", 3));
		weightedOutcomes.add(new NameValuePair("A", 4));
		weightedOutcomes.add(new NameValuePair("A", 4));
		
		Split subset = new Split(weightedOutcomes, 0, 4);
		int split = splitter.split(subset);
		assertEquals(2, split);
	}
	
	@Test
	public void testSplitMultiple() {
		InformationGainSplitter splitter = new InformationGainSplitter();
		splitter.setInformationGainThreshold(0.01);
		List<NameValuePair> weightedOutcomes = new Vector<NameValuePair>();
		weightedOutcomes.add(new NameValuePair("A", 1));
		weightedOutcomes.add(new NameValuePair("B", 2));
		weightedOutcomes.add(new NameValuePair("B", 3));
		weightedOutcomes.add(new NameValuePair("B", 4));
		weightedOutcomes.add(new NameValuePair("A", 5));
		weightedOutcomes.add(new NameValuePair("A", 5));
		
		List<Integer> splits = splitter.split(weightedOutcomes);
		for (int split : splits) {
			LOG.debug("Split: " + split);
		}
		assertEquals(2, splits.size());
	}
	
	@Test
	public void testSplitMultipleFayyadAndIrani() {
		FeatureSplitter splitter = new FayyadIraniSplitter();
		List<NameValuePair> weightedOutcomes = new Vector<NameValuePair>();
		weightedOutcomes.add(new NameValuePair("A", 1));
		weightedOutcomes.add(new NameValuePair("B", 2));
		weightedOutcomes.add(new NameValuePair("B", 3));
		weightedOutcomes.add(new NameValuePair("B", 4));
		weightedOutcomes.add(new NameValuePair("B", 4));
		weightedOutcomes.add(new NameValuePair("A", 5));
		weightedOutcomes.add(new NameValuePair("A", 5));
		weightedOutcomes.add(new NameValuePair("A", 5));
		weightedOutcomes.add(new NameValuePair("A", 6));
		
		List<Integer> splits = splitter.split(weightedOutcomes);
		for (int split : splits) {
			LOG.debug("Split: " + split);
		}
		assertEquals(2, splits.size());
	}

}
