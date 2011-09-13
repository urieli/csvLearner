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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.csvLearner.NameValuePair;

/**
 * A feature splitter that attempts to split at the point
 * giving maximum information gain, assuming a certain condition is met
 * regarding the two splits, which is left abstract.
 * @author Assaf Urieli
 *
 */
public abstract class AbstractFeatureSplitter implements FeatureSplitter {
	private static final Log LOG = LogFactory.getLog(AbstractFeatureSplitter.class);
		
	/** The natural logarithm of 2 */
//	private static double LOG2 = Math.log(2);

	private int minNodeSize = 1;
	private int maxDepth = -1;
	private double minErrorRate = -1;

	private double originalEntropy = -1;
	private Map<Integer,Set<Split>> splitsByDepth = null;

	/* (non-Javadoc)
	 * @see com.joliciel.csvLearner.features.FeatureSplitter#split(java.util.List)
	 */
	@Override
	public List<Integer> split(List<NameValuePair> featureValues) {
		Set<Integer> splits = new TreeSet<Integer>();
		splitsByDepth = new TreeMap<Integer, Set<Split>>();
		Split subset = new Split(featureValues, 0, featureValues.size()-1);
		this.splitInternal(subset, splits, 1);
		List<Integer> splitList = new Vector<Integer>(splits);
		return splitList;
	}

	void splitInternal(Split subset, Set<Integer> splits, int depth) {
		if (maxDepth>0 && depth>maxDepth)
			return;
		Collection<Split> theSplits = new Vector<Split>();
		int splitIndex = this.split(subset, theSplits);

		Set<Split> currentLevelSplits = this.splitsByDepth.get(depth);
		if (currentLevelSplits==null) {
			currentLevelSplits = new TreeSet<Split>();
			this.splitsByDepth.put(depth, currentLevelSplits);
		}
		currentLevelSplits.addAll(theSplits);

		if (splitIndex >= 0) {
			splits.add(splitIndex);
			if (splitIndex - subset.getStartIndex() >= minNodeSize * 2) {
				Split leftSubset = new Split(subset.getFeatureValues(), subset.getStartIndex(), splitIndex);
				this.splitInternal(leftSubset, splits, depth+1);
			}
			if (subset.getEndIndex() - (splitIndex+1) >= minNodeSize * 2) {
				Split rightSubset = new Split(subset.getFeatureValues(), splitIndex+1, subset.getEndIndex());
				this.splitInternal(rightSubset, splits, depth+1);
			}
		}
	}

	/**
	 * Splits a list at the index which gives the maximum information gain, if >= a certain threshold.
	 * @param featureValues an ordered list of weighted outcomes (by weight)
	 * @param startIndex the start index of the splittable range in featureValues
	 * @param endIndex the end index of the splittable range in featureValues
	 * @return the index at which to split the weightedOutcomes, or -1 if no split. Everything <= this index is in one class, the remainder in another class.
	 */
	int split(Split subset) {
		return this.split(subset, null);
	}

	/**
	 * Splits a list at the index which gives the maximum information gain, if >= a certain threshold.
	 * @param featureValues an ordered list of weighted outcomes (by weight)
	 * @param startIndex the start index of the splittable range in featureValues
	 * @param endIndex the end index of the splittable range in featureValues
	 * @param theSplits if not null, will store the two splits
	 * @return the index at which to split the weightedOutcomes, or -1 if no split. Everything <= this index is in one class, the remainder in another class.
	 */
	int split(Split subset, Collection<Split> theSplits) {
		// TODO: it seems likely that the information gain peaks at each level of recursion fall at the same places
		// as the peaks at the previous levels of recursion. Therefore, if we pass in the positions of
		// all the peaks, we could limit ourselves to looking at information gain at those positions only.
		Map<String,Integer> outcomeCountsRight = new TreeMap<String, Integer>();
		outcomeCountsRight.putAll(subset.getOutcomeCounts());
		int maxOutcomeCount = subset.getMaxOutcomeCount();
		
		Map<String,Integer> outcomeCountsLeft = new TreeMap<String, Integer>();
		for (String outcome : outcomeCountsRight.keySet()) {
			outcomeCountsLeft.put(outcome, 0);
		}
		int totalCount = (subset.getEndIndex() - subset.getStartIndex()) + 1;
		int totalRight = totalCount;
		int totalLeft = 0;

		double entropy = subset.getEntropy();
		if (entropy==0)
			return -1;
		if (originalEntropy<0)
			originalEntropy = entropy;

		LOG.trace("Split size: " + totalCount);
		double errorRateForMajorityOutcome = ((double)(totalCount - maxOutcomeCount) / (double) totalCount) * 100;
		LOG.trace("maxOutcomeCount: " + maxOutcomeCount + "(error rate: " + errorRateForMajorityOutcome + ")");
		if (this.minErrorRate>0 && errorRateForMajorityOutcome<this.minErrorRate)
			return -1;

		LOG.trace("startIndex: " + subset.getStartIndex());
		LOG.trace("endIndex: " + subset.getEndIndex());
		LOG.trace("entropy: " + entropy);
		double maxInformationGain = 0;
		int maxGainIndex = -1;
		double maxGainValue = 0;
		double maxGainEntropyLeft = 0;
		double maxGainEntropyRight = 0;
		for (int i = subset.getStartIndex(); i <= subset.getEndIndex() - 1; i++) {
			NameValuePair dataPoint = subset.getFeatureValues().get(i);
			LOG.trace("index "+ i);
			String outcome = dataPoint.getName();
			int leftCount = outcomeCountsLeft.get(outcome);
			int rightCount = outcomeCountsRight.get(outcome);
			outcomeCountsLeft.put(outcome, leftCount+1);
			outcomeCountsRight.put(outcome, rightCount-1);
			totalLeft += 1;
			totalRight -= 1;
			if (i<subset.getEndIndex()) {
				// if the next outcome has the same value as the current one, then skip this one
				if (subset.getFeatureValues().get(i+1).getValue()==dataPoint.getValue()) {
					LOG.trace("Skipping");
					continue;
				}
			}
			// only consider nodes >= minNodeSize
			if ((i-subset.getStartIndex())+1 < minNodeSize)
				continue;
			if ((subset.getEndIndex()-i)+1 < minNodeSize)
				continue;
			double entropyLeft = EntropyCalculator.getEntropy(outcomeCountsLeft.values(), totalLeft);
			LOG.trace("entropyLeft: " + entropyLeft);

			double proportionalEntropyLeft = ((double)totalLeft / (double)totalCount) * entropyLeft;

			double entropyRight = EntropyCalculator.getEntropy(outcomeCountsRight.values(), totalRight);

			LOG.trace("entropyRight: " + entropyRight);

			double proportionalEntropyRight = ((double)totalRight / (double)totalCount) * entropyRight;

			double splitEntropy = proportionalEntropyLeft + proportionalEntropyRight;
			double informationGain = entropy - splitEntropy;
			LOG.trace("Split at index " + i + ", value " +  dataPoint.getValue() + ": " + informationGain);
			if (informationGain > maxInformationGain) {
				maxInformationGain = informationGain;
				maxGainIndex = i;
				maxGainValue = dataPoint.getValue();
				maxGainEntropyLeft = entropyLeft;
				maxGainEntropyRight = entropyRight;
			}
		}

		LOG.trace("maxInformationGain: " + maxInformationGain);
		LOG.trace("maxGainIndex: " + maxGainIndex);
		LOG.trace("maxGainValue: " + maxGainValue);
		LOG.trace("originalEntropy: " + originalEntropy);
		int splitIndex = -1;
		
		if (!this.checkStopCondition(subset, maxGainIndex, maxInformationGain))
			splitIndex = maxGainIndex;
		
		if (splitIndex>=0) {
			LOG.trace("Adding split " + maxGainIndex);
			if (theSplits!=null) {
				Split leftSplit = new Split(subset.getFeatureValues(), subset.getStartIndex(), splitIndex);
				leftSplit.setEntropy(maxGainEntropyLeft);
				theSplits.add(leftSplit);
				Split rightSplit = new Split(subset.getFeatureValues(), splitIndex+1, subset.getEndIndex());
				rightSplit.setEntropy(maxGainEntropyRight);
				theSplits.add(rightSplit);
			}
		} else {
			if (theSplits!=null) {
				theSplits.add(subset);
			}			
			LOG.trace("No split");
		}

		return splitIndex;
	}

	/**
	 * The min node size to allow for the splits.
	 */
	public int getMinNodeSize() {
		return minNodeSize;
	}

	public void setMinNodeSize(int minNodeSize) {
		this.minNodeSize = minNodeSize;
	}

	/**
	 * The max splitting depth - if maxDepth=n, we will have at most 2^n splits.
	 * @return
	 */
	public int getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	/**
	 * If a node has error rate < minErrorRate, don't bother splitting.
	 * The error rate is defined as the (|node| - |majorityClass|)/|node|.
	 * @return
	 */
	public double getMinErrorRate() {
		return minErrorRate;
	}

	public void setMinErrorRate(double minErrorRate) {
		this.minErrorRate = minErrorRate;
	}

	@Override
	public Map<Integer, Set<Split>> getSplitsByDepth() {
		return splitsByDepth;
	}
	
	public abstract boolean checkStopCondition(Split subset, int proposedSplit, double informationGain);

	


	
}
