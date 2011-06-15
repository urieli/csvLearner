package com.joliciel.csvLearner.utils;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.joliciel.csvLearner.NameValuePair;
import com.joliciel.csvLearner.utils.FeatureSplitter;
import com.joliciel.csvLearner.utils.FeatureSplitter.StopConditionTest;

public class FeatureSplitterTest {
	private static final Log LOG = LogFactory.getLog(FeatureSplitterTest.class);

	@Test
	public void testSplit() {
		FeatureSplitter splitter = new FeatureSplitter();
		splitter.setInformationGainThreshold(0.01);
		
		List<NameValuePair> weightedOutcomes = new Vector<NameValuePair>();
		weightedOutcomes.add(new NameValuePair("A", 1));
		weightedOutcomes.add(new NameValuePair("B", 2));
		weightedOutcomes.add(new NameValuePair("B", 3));
		weightedOutcomes.add(new NameValuePair("A", 4));
		weightedOutcomes.add(new NameValuePair("A", 4));
		
		int split = splitter.split(weightedOutcomes, 0, 4);
		assertEquals(2, split);
	}
	
	@Test
	public void testSplitMultiple() {
		FeatureSplitter splitter = new FeatureSplitter();
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
		FeatureSplitter splitter = new FeatureSplitter();
		splitter.setStopConditionTest(StopConditionTest.FAYYAD_IRANI);
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
