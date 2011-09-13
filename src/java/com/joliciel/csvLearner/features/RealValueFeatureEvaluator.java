///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2011 Assaf Urieli
//
//This file is part of csvLearner.
//
//csvLearner is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//csvLearner is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with csvLearner.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.csvLearner.features;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.csvLearner.GenericEvent;
import com.joliciel.csvLearner.GenericEvents;
import com.joliciel.csvLearner.NameValuePair;

/**
 * Take a real-valued feature, and try to evaluate it's value based on potential information gain.
 * @author Assaf Urieli
 *
 */
public class RealValueFeatureEvaluator {
	private static final Log LOG = LogFactory.getLog(RealValueFeatureEvaluator.class);

	private FeatureSplitter featureSplitter = null;
	private long totalTime = 0;
	private long totalTimeSplit = 0;
	private long totalTimeInitialEntropy = 0;
	private long totalTimeInitialise = 0;
	private long totalTimeOrdering = 0;
	private long totalTimeSplitEntropy = 0;
	private long totalTimeFindFeature = 0;
	
	public List<Double> evaluateFeature(GenericEvents events, String feature) {
		return this.evaluateFeature(events, feature, null);
	}
	
	/**
	 * For a given feature, calculate the entropy after each level of splitting.
	 * Level 0: the entropy taking into account only those events which have a value as opposed to those which don't
	 * Level 1: entropy for events without a value (where value=0) + entropy of other events after first split
	 * Level 2: entropy for events without a value (where value=0) + entropy of other events after second split
	 * etc.
	 * @param events the list of events
	 * @param feature the feature to consider for splitting
	 * @return 
	 */
	public List<Double> evaluateFeature(GenericEvents events, String feature, String testOutcome) {
		long startTime = (new Date()).getTime();
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("Evaluating feature: " + feature);
			LOG.trace("Test outcome: " + testOutcome);
		}
		long startTimeInitialise = (new Date()).getTime();

		PriorityQueue<NameValuePair> heap = new PriorityQueue<NameValuePair>(events.size());
		Collection<NameValuePair> featureValues = new Vector<NameValuePair>();
		Map<String,Integer> eventOutcomeMap = new TreeMap<String,Integer>();
		Map<String,Integer> featureOutcomeMap = new TreeMap<String,Integer>();
		Map<String,Integer> nonFeatureOutcomeMap = new TreeMap<String,Integer>();
		
		List<String> outcomes = null;
		if (testOutcome==null) {
			Set<String> outcomeSet = events.getOutcomes();
			outcomes = new Vector<String>(outcomeSet);
		} else {
			outcomes = new Vector<String>();
			outcomes.add(testOutcome);
			outcomes.add("");
		}
		int[] eventOutcomeCounts = new int[outcomes.size()];
		int[] featureOutcomeCounts = new int[outcomes.size()];
		int[] nonFeatureOutcomeCounts = new int[outcomes.size()];
		
		int eventCount = events.size();
		int featureCount = 0;
		for (GenericEvent event : events) {
			if (!event.isTest()) {
				String outcome = event.getOutcome();
				int outcomeIndex = 0;
				if (testOutcome==null) {
					outcomeIndex = outcomes.indexOf(outcome);
				} else {
					if (!outcome.equals(testOutcome)) {
						outcome = "";
						outcomeIndex = 1;
					} else {
						outcomeIndex = 0;
					}
				}
				
				long startTimeFindFeature = (new Date()).getTime();
				int featureIndex = event.getFeatureIndex(feature);
				long endTimeFindFeature = (new Date()).getTime();
				totalTimeFindFeature += (endTimeFindFeature - startTimeFindFeature);
				if (featureIndex>=0) {
					long startTimeOrdering = (new Date()).getTime();
					heap.add(new NameValuePair(outcome, event.getWeights().get(featureIndex)));
					long endTimeOrdering = (new Date()).getTime();
					totalTimeOrdering += (endTimeOrdering - startTimeOrdering);
					featureOutcomeCounts[outcomeIndex]++;
					featureCount++;
				} else {
					nonFeatureOutcomeCounts[outcomeIndex]++;
				}
				eventOutcomeCounts[outcomeIndex]++;
			}
		}
		int nonFeatureCount = eventCount - featureCount;

		long startTimeOrdering = (new Date()).getTime();
		while (!heap.isEmpty())
			featureValues.add(heap.poll());
		long endTimeOrdering = (new Date()).getTime();
		totalTimeOrdering += (endTimeOrdering - startTimeOrdering);
		
		int i = 0;
		for (String outcome : outcomes) {
			eventOutcomeMap.put(outcome, eventOutcomeCounts[i]);
			featureOutcomeMap.put(outcome, featureOutcomeCounts[i]);
			nonFeatureOutcomeMap.put(outcome, nonFeatureOutcomeCounts[i]);
			i++;
		}
		
		long endTimeInitialise = (new Date()).getTime();
		totalTimeInitialise+= (endTimeInitialise - startTimeInitialise);
		
		long startTimeInitialEntropy = (new Date()).getTime();
		double eventSpaceEntropy = EntropyCalculator.getEntropy(eventOutcomeMap.values(), eventCount);
		double featureEntropy = EntropyCalculator.getEntropy(featureOutcomeMap.values(), featureCount);
		double nonFeatureEntropy = EntropyCalculator.getEntropy(nonFeatureOutcomeMap.values(), nonFeatureCount);
		long endTimeInitialEntropy = (new Date()).getTime();
		totalTimeInitialEntropy += (endTimeInitialEntropy - startTimeInitialEntropy);
		
		List<Double> entropyByLevel = new Vector<Double>();
		entropyByLevel.add(eventSpaceEntropy);
		
		double proportionalFeatureEntropy = ((double)featureCount/(double)eventCount)*featureEntropy;
		double proportionalNonFeatureEntropy = ((double)nonFeatureCount/(double)eventCount)*nonFeatureEntropy;
		double level0Entropy = proportionalFeatureEntropy + proportionalNonFeatureEntropy;
		entropyByLevel.add(level0Entropy);
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("eventSpaceEntropy: " + eventSpaceEntropy);
			LOG.trace("proportionalFeatureEntropy: " + proportionalFeatureEntropy);
			LOG.trace("proportionalNonFeatureEntropy: " + proportionalNonFeatureEntropy);
			LOG.trace("level 0 Entropy: " + level0Entropy);
		}
		
		List<NameValuePair> featureValueList = new Vector<NameValuePair>(featureValues);
		long startTimeSplit = (new Date()).getTime();
		featureSplitter.split(featureValueList);
		long endTimeSplit = (new Date()).getTime();
		totalTimeSplit += (endTimeSplit - startTimeSplit);
		
		Map<Integer,Set<Split>> splitsByDepth = featureSplitter.getSplitsByDepth();
		
		for (int level : splitsByDepth.keySet()) {
			double levelEntropy = proportionalNonFeatureEntropy;
			if (splitsByDepth.get(level).size()==0)
				levelEntropy += proportionalFeatureEntropy;
			else {
				for (Split split : splitsByDepth.get(level)) {
					long startTimeSplitEntropy = (new Date()).getTime();
					double proprotionalEntropy = ((double) split.getSize() / (double) eventCount) * split.getEntropy();
					long endTimeSplitEntropy = (new Date()).getTime();
					totalTimeSplitEntropy += (endTimeSplitEntropy - startTimeSplitEntropy);
					levelEntropy += proprotionalEntropy;
				}
			}
			entropyByLevel.add(levelEntropy);
			if (LOG.isTraceEnabled())
				LOG.trace("level " + level + " Entropy: " + levelEntropy);
		}
		long endTime = (new Date()).getTime();
		totalTime += (endTime - startTime);

		return entropyByLevel;
	}

	public void logPerformance() {
		DecimalFormat df = new DecimalFormat("0.00");
		LOG.info("total time: " + totalTime + " milliseconds");
		LOG.info("total initialise time: " + totalTimeInitialise + " milliseconds. " + df.format(((double)totalTimeInitialise/(double)totalTime)*100.0) + "%");
		LOG.info("total find feature time: " + totalTimeFindFeature + " milliseconds. " + df.format(((double)totalTimeFindFeature/(double)totalTime)*100.0) + "%");
		LOG.info("total sort time: " + totalTimeOrdering + " milliseconds. " + df.format(((double)totalTimeOrdering/(double)totalTime)*100.0) + "%");
		LOG.info("total split time: " + totalTimeSplit + " milliseconds. " + df.format(((double)totalTimeSplit/(double)totalTime)*100.0) + "%");
		LOG.info("total initial entropy time: " + totalTimeInitialEntropy + " milliseconds. " + df.format(((double)totalTimeInitialEntropy/(double)totalTime)*100.0) + "%");
		LOG.info("total split entropy time: " + totalTimeSplitEntropy + " milliseconds. " + df.format(((double)totalTimeSplitEntropy/(double)totalTime)*100.0) + "%");
	}
	
	public FeatureSplitter getFeatureSplitter() {
		return featureSplitter;
	}

	public void setFeatureSplitter(FeatureSplitter featureSplitter) {
		this.featureSplitter = featureSplitter;
	}

}
