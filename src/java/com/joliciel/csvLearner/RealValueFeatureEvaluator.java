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
package com.joliciel.csvLearner;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.csvLearner.utils.FeatureSplitter;
import com.joliciel.csvLearner.utils.Split;

/**
 * Take a real-valued feature, and try to evaluate it's value based on potential information gain.
 * @author Assaf Urieli
 *
 */
public class RealValueFeatureEvaluator {
	private static final Log LOG = LogFactory.getLog(RealValueFeatureEvaluator.class);

	
	/**
	 * Transform a given real-valued feature into a set of 
	 * @param events the list of events
	 * @param feature the feature to consider for splitting
	 * @param informationGainThreshold as a % of the total entropy - if max gain < this threshold, we won't bother splitting
	 * @param minNodeSize the min node size to consider for splitting
	 * @param maxDepth the max depth to consider for splitting - if maxDepth=n, we will have at most 2^n splits
	 * @param minErrorRate if a node has error rate < minErrorRate, don't bother splitting
	 * @return
	 */
	public List<Double> evaluateFeature(GenericEvents events, String feature,
			double informationGainThreshold, int minNodeSize, int maxDepth, double minErrorRate) {
		LOG.debug("Splitting feature: " + feature);
		Collection<NameValuePair> featureValues = new TreeSet<NameValuePair>();
		Map<String,Integer> eventOutcomeMap = new TreeMap<String,Integer>();
		Map<String,Integer> featureOutcomeMap = new TreeMap<String,Integer>();
		Map<String,Integer> nonFeatureOutcomeMap = new TreeMap<String,Integer>();
		int eventCount = events.size();
		int featureCount = 0;
		for (GenericEvent event : events) {
			if (!event.isTest()) {
				int featureIndex = event.getFeatures().indexOf(feature);
				if (featureIndex>=0) {
					featureValues.add(new NameValuePair(event.getOutcome(), event.getWeights().get(featureIndex)));
					Integer countObj = featureOutcomeMap.get(event.getOutcome());
					int count = (countObj==null) ? 0 : countObj.intValue();
					featureOutcomeMap.put(event.getOutcome(), count+1);
					featureCount++;
				} else {
					Integer countObj = nonFeatureOutcomeMap.get(event.getOutcome());
					int count = (countObj==null) ? 0 : countObj.intValue();
					nonFeatureOutcomeMap.put(event.getOutcome(), count+1);
				}
				Integer countObj = eventOutcomeMap.get(event.getOutcome());
				int count = (countObj==null) ? 0 : countObj.intValue();
				eventOutcomeMap.put(event.getOutcome(), count+1);

			}
		}
		double eventSpaceEntropy = 0;
		for (int outcomeCount : eventOutcomeMap.values()) {
			double proportion = ((double) outcomeCount / (double) eventCount);
			eventSpaceEntropy -=  proportion * Math.log(proportion);
		}
		double featureEntropy = 0;
		for (int outcomeCount : featureOutcomeMap.values()) {
			double proportion = ((double) outcomeCount / (double) featureCount);
			featureEntropy -=  proportion * Math.log(proportion);
		}
		int nonFeatureCount = eventCount - featureCount;
		double nonFeatureEntropy = 0;
		for (int outcomeCount : nonFeatureOutcomeMap.values()) {
			double proportion = ((double) outcomeCount / (double) nonFeatureCount);
			nonFeatureEntropy -=  proportion * Math.log(proportion);
		}
		
		List<Double> entropyByLevel = new Vector<Double>();
		entropyByLevel.add(eventSpaceEntropy);
		LOG.debug("eventSpaceEntropy: " + eventSpaceEntropy);
		
		double proportionalFeatureEntropy = ((double)featureCount/(double)eventCount)*featureEntropy;
		double proportionalNonFeatureEntropy = ((double)nonFeatureCount/(double)eventCount)*nonFeatureEntropy;
		double level0Entropy = proportionalFeatureEntropy + proportionalNonFeatureEntropy;
		entropyByLevel.add(level0Entropy);
		LOG.debug("level 0 Entropy: " + eventSpaceEntropy);
		
		List<NameValuePair> featureValueList = new Vector<NameValuePair>(featureValues);
		FeatureSplitter splitter = new FeatureSplitter();
		splitter.setInformationGainThreshold(informationGainThreshold);
		splitter.setMinNodeSize(minNodeSize);
		splitter.setMinErrorRate(minErrorRate);
		splitter.setMaxDepth(maxDepth);
		splitter.split(featureValueList);
		
		Map<Integer,Set<Split>> splitsByDepth = splitter.getSplitsByDepth();
		
		for (int level : splitsByDepth.keySet()) {
			LOG.debug("Level: " + level);
			double levelEntropy = proportionalNonFeatureEntropy;
			for (Split split : splitsByDepth.get(level)) {
				double proprotionalEntropy = ((double) split.getSize() / (double) eventCount) * split.getEntropy();
				levelEntropy += proprotionalEntropy;
			}
			entropyByLevel.add(levelEntropy);
			LOG.debug("level " + level + " Entropy: " + levelEntropy);
		}
		return entropyByLevel;
	}

}
