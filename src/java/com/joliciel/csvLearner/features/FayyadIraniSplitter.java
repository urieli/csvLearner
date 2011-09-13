//Copyright (C) 2011 Assaf Urieli
package com.joliciel.csvLearner.features;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.csvLearner.NameValuePair;

public class FayyadIraniSplitter extends AbstractFeatureSplitter {
	private static final Log LOG = LogFactory.getLog(FayyadIraniSplitter.class);

	@Override
	public boolean checkStopCondition(Split subset, int proposedSplit, double informationGain) {
		LOG.trace("applyFayyadAndIraniMDLTest: " + subset.getStartIndex() + ", " + proposedSplit + ", " + subset.getEndIndex());
		double priorEntropy, entropy, gain; 
		double entropyLeft, entropyRight, delta;
		int numClassesTotal, numClassesRight, numClassesLeft;

		// get the outcome counts for the whole set & for left and right splits
		Map<String,Integer> outcomeCounts = new TreeMap<String, Integer>();
		Map<String,Integer> outcomeCountsLeft = new TreeMap<String, Integer>();
		Map<String,Integer> outcomeCountsRight = new TreeMap<String, Integer>();
		Set<String> outcomes = new TreeSet<String>();
		for (int i = subset.getStartIndex(); i <= subset.getEndIndex(); i++) {
			outcomes.add(subset.getFeatureValues().get(i).getName());
		}
		for (String outcome : outcomes) {
			outcomeCounts.put(outcome, 0);
			outcomeCountsLeft.put(outcome, 0);
			outcomeCountsRight.put(outcome, 0);
		}
		for (int i = subset.getStartIndex(); i <= subset.getEndIndex(); i++) {
			NameValuePair dataPoint = subset.getFeatureValues().get(i);
			int outcomeCount = outcomeCounts.get(dataPoint.getName());
			outcomeCounts.put(dataPoint.getName(), outcomeCount+1);
			if (i<=proposedSplit) {
				int outcomeCountLeft = outcomeCountsLeft.get(dataPoint.getName());
				outcomeCountsLeft.put(dataPoint.getName(), outcomeCountLeft+1);
			} else {
				int outcomeCountRight = outcomeCountsRight.get(dataPoint.getName());
				outcomeCountsRight.put(dataPoint.getName(), outcomeCountRight+1);
			}
		}

		int totalCount = (subset.getEndIndex()-subset.getStartIndex())+1;
		int leftCount = (proposedSplit-subset.getStartIndex())+1;
		int rightCount = subset.getEndIndex()-proposedSplit;
		LOG.trace("totalCount: " + totalCount);
		LOG.trace("leftCount: " + leftCount);
		LOG.trace("rightCount: " + rightCount);

		// Compute entropy before split.
		priorEntropy = EntropyCalculator.getEntropy(outcomeCounts.values(), totalCount);


		// Compute entropy after split.
		entropyLeft = EntropyCalculator.getEntropy(outcomeCountsLeft.values(), leftCount);
		entropyRight = EntropyCalculator.getEntropy(outcomeCountsRight.values(), rightCount);
		entropy = ((double) leftCount / (double) totalCount) * entropyLeft 
			+ ((double) rightCount / (double) totalCount) * entropyRight ;

		// Compute information gain.
		gain = priorEntropy - entropy;
		LOG.trace("gain: " + gain);
		
		// Number of classes occuring in the set
		numClassesTotal = outcomes.size();

		// Number of classes occuring in the left subset
		numClassesLeft = 0;
		for (int i : outcomeCountsLeft.values()) {
			if (i > 0)
				numClassesLeft++;
		}

		// Number of classes occuring in the right subset
		numClassesRight = 0;
		for (int i : outcomeCountsRight.values()) {
			if (i > 0)
				numClassesRight++;
		}

		// Compute terms for MDL formula
		delta = log2(Math.pow(3, numClassesTotal) - 2) - 
			(((double) numClassesTotal * priorEntropy) - 
				(numClassesRight * entropyRight) - 
				(numClassesLeft * entropyLeft));
		LOG.trace("delta: " + delta);
		
		double mdl = (log2(totalCount-1) + delta) / (double)totalCount;
		LOG.trace("mdl: " + mdl);
		
		// Check if split should be rejected
		return (gain <= mdl);	
	}


	double log2(double value) {
		// for now we'll keep everything in base e
		return Math.log(value);
		//return Math.log(value) / LOG2;
	}
}
