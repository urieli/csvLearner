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
package com.joliciel.csvLearner.utils;

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
 * Given a training sample as a set of (x,y) where x is a real number and y is a class,
 * finds the value xsplit such that,
 * if the training sample was split into two subsets, one for all items
 * where x<=xsplit, and one for all items where x>xsplit
 * would provide the maximum information gain.
 * The information gain is measured in terms of information entropy.
 * @author Assaf Urieli
 *
 */
public class FeatureSplitter {
	private static final Log LOG = LogFactory.getLog(FeatureSplitter.class);
	
	public enum StopConditionTest {
		INFORMATION_GAIN_PERCENT,
		FAYYAD_IRANI
	}
	
	/** The natural logarithm of 2 */
//	private static double LOG2 = Math.log(2);

	private int minNodeSize = 1;
	private double informationGainThreshold = 0;
	private int maxDepth = -1;
	private double minErrorRate = -1;
	private StopConditionTest stopConditionTest = StopConditionTest.INFORMATION_GAIN_PERCENT;

	private double originalEntropy = -1;
	private Map<Integer,Set<Split>> splitsByDepth = new TreeMap<Integer, Set<Split>>();

	/**
	 * Split a list multiple times, until no split is found with sufficient information gain.
	 * @param featureValues
	 * @return
	 */
	public List<Integer> split(List<NameValuePair> featureValues) {
		Set<Integer> splits = new TreeSet<Integer>();
		this.splitInternal(featureValues, 0, featureValues.size()-1, splits, 1);
		List<Integer> splitList = new Vector<Integer>(splits);
		return splitList;
	}

	void splitInternal(List<NameValuePair> featureValues, int startIndex, int endIndex, Set<Integer> splits, int depth) {
		if (maxDepth>0 && depth>maxDepth)
			return;
		Collection<Split> theSplits = new Vector<Split>();
		int splitIndex = this.split(featureValues, startIndex, endIndex, theSplits);

		Set<Split> currentLevelSplits = this.splitsByDepth.get(depth);
		if (currentLevelSplits==null) {
			currentLevelSplits = new TreeSet<Split>();
			this.splitsByDepth.put(depth, currentLevelSplits);
		}
		currentLevelSplits.addAll(theSplits);

		if (splitIndex >= 0) {
			splits.add(splitIndex);
			if (splitIndex - startIndex >= minNodeSize * 2)
				this.splitInternal(featureValues, startIndex, splitIndex, splits, depth+1);
			if (endIndex - (splitIndex+1) >= minNodeSize * 2)
				this.splitInternal(featureValues, splitIndex+1, endIndex, splits, depth+1);
		}
	}

	/**
	 * Splits a list at the index which gives the maximum information gain, if >= a certain threshold.
	 * @param featureValues an ordered list of weighted outcomes (by weight)
	 * @param startIndex the start index of the splittable range in featureValues
	 * @param endIndex the end index of the splittable range in featureValues
	 * @return the index at which to split the weightedOutcomes, or -1 if no split. Everything <= this index is in one class, the remainder in another class.
	 */
	public int split(List<NameValuePair> featureValues, int startIndex, int endIndex) {
		return this.split(featureValues, startIndex, endIndex, null);
	}

	/**
	 * Splits a list at the index which gives the maximum information gain, if >= a certain threshold.
	 * @param featureValues an ordered list of weighted outcomes (by weight)
	 * @param startIndex the start index of the splittable range in featureValues
	 * @param endIndex the end index of the splittable range in featureValues
	 * @param theSplits if not null, will store the two splits
	 * @return the index at which to split the weightedOutcomes, or -1 if no split. Everything <= this index is in one class, the remainder in another class.
	 */
	public int split(List<NameValuePair> featureValues, int startIndex, int endIndex, Collection<Split> theSplits) {
		// TODO: it seems likely that the information gain peaks at each level of recursion fall at the same places
		// as the peaks at the previous levels of recursion. Therefore, if we pass in the positions of
		// all the peaks, we could limit ourselves to looking at information gain at those positions only.
		Map<String,Integer> outcomeCountsRight = new TreeMap<String, Integer>();
		int maxOutcomeCount = 0;
		for (int i = startIndex; i <= endIndex; i++) {
			NameValuePair dataPoint = featureValues.get(i);
			Integer outcomeCountObj = outcomeCountsRight.get(dataPoint.getName());
			int outcomeCount = outcomeCountObj==null ? 0 : outcomeCountObj.intValue();
			outcomeCount++;
			if (outcomeCount>maxOutcomeCount)
				maxOutcomeCount = outcomeCount;
			outcomeCountsRight.put(dataPoint.getName(), outcomeCount);
		}
		Map<String,Integer> outcomeCountsLeft = new TreeMap<String, Integer>();
		for (String outcome : outcomeCountsRight.keySet()) {
			outcomeCountsLeft.put(outcome, 0);
		}
		int totalCount = (endIndex - startIndex) + 1;
		int totalRight = totalCount;
		int totalLeft = 0;

		double entropy = this.getEntropy(outcomeCountsRight.values(), totalRight);
		if (entropy==0)
			return -1;
		if (originalEntropy<0)
			originalEntropy = entropy;

		LOG.trace("Split size: " + totalCount);
		double errorRateForMajorityOutcome = ((double)(totalCount - maxOutcomeCount) / (double) totalCount) * 100;
		LOG.trace("maxOutcomeCount: " + maxOutcomeCount + "(error rate: " + errorRateForMajorityOutcome + ")");
		if (this.minErrorRate>0 && errorRateForMajorityOutcome<this.minErrorRate)
			return -1;

		LOG.trace("startIndex: " + startIndex);
		LOG.trace("endIndex: " + endIndex);
		LOG.trace("entropy: " + entropy);
		double maxInformationGain = 0;
		int maxGainIndex = -1;
		double maxGainValue = 0;
		double maxGainEntropyLeft = 0;
		double maxGainEntropyRight = 0;
		for (int i = startIndex; i <= endIndex - 1; i++) {
			NameValuePair dataPoint = featureValues.get(i);
			LOG.trace("index "+ i);
			String outcome = dataPoint.getName();
			int leftCount = outcomeCountsLeft.get(outcome);
			int rightCount = outcomeCountsRight.get(outcome);
			outcomeCountsLeft.put(outcome, leftCount+1);
			outcomeCountsRight.put(outcome, rightCount-1);
			totalLeft += 1;
			totalRight -= 1;
			if (i<endIndex) {
				// if the next outcome has the same value as the current one, then skip this one
				if (featureValues.get(i+1).getValue()==dataPoint.getValue()) {
					LOG.trace("Skipping");
					continue;
				}
			}
			// only consider nodes >= minNodeSize
			if ((i-startIndex)+1 < minNodeSize)
				continue;
			if ((endIndex-i)+1 < minNodeSize)
				continue;
			double entropyLeft = this.getEntropy(outcomeCountsLeft.values(), totalLeft);
			LOG.trace("entropyLeft: " + entropyLeft);

			double proportionalEntropyLeft = ((double)totalLeft / (double)totalCount) * entropyLeft;

			double entropyRight = this.getEntropy(outcomeCountsRight.values(), totalRight);

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
		LOG.trace("informationGainThreshold: " + informationGainThreshold);
		LOG.trace("originalEntropy: " + originalEntropy);
		int splitIndex = -1;
		
		if (this.stopConditionTest.equals(StopConditionTest.FAYYAD_IRANI)) {
			if (this.applyFayyadAndIraniMDLTest(featureValues, startIndex, endIndex, maxGainIndex)) {
				splitIndex = maxGainIndex;
			}
		} else {
			double normalisedThreshold = informationGainThreshold * entropy;
	
			LOG.trace("normalisedThreshold: " + normalisedThreshold);
			if (maxInformationGain >= normalisedThreshold) {
				splitIndex = maxGainIndex;
			}
		}
		
		if (splitIndex>=0) {
			LOG.trace("Adding split " + maxGainIndex);
			if (theSplits!=null) {
				Split leftSplit = new Split(startIndex, splitIndex);
				leftSplit.setEntropy(maxGainEntropyLeft);
				theSplits.add(leftSplit);
				Split rightSplit = new Split(splitIndex+1, endIndex);
				rightSplit.setEntropy(maxGainEntropyRight);
				theSplits.add(rightSplit);
			}
		} else {
			if (theSplits!=null) {
				Split split = new Split(startIndex, endIndex);
				split.setEntropy(entropy);
				theSplits.add(split);
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
	 * Information gain as a % of the total entropy for the collection of events
	 * - if max gain < this threshold, we won't bother splitting
	 */
	public double getInformationGainThreshold() {
		return informationGainThreshold;
	}

	public void setInformationGainThreshold(double informationGainThreshold) {
		this.informationGainThreshold = informationGainThreshold;
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

	public Map<Integer, Set<Split>> getSplitsByDepth() {
		return splitsByDepth;
	}

	boolean applyFayyadAndIraniMDLTest(List<NameValuePair> featureValues, int startIndex, int endIndex, int split) {
		LOG.trace("applyFayyadAndIraniMDLTest: " + startIndex + ", " + split + ", " + endIndex);
		double priorEntropy, entropy, gain; 
		double entropyLeft, entropyRight, delta;
		int numClassesTotal, numClassesRight, numClassesLeft;

		// get the outcome counts for the whole set & for left and right splits
		Map<String,Integer> outcomeCounts = new TreeMap<String, Integer>();
		Map<String,Integer> outcomeCountsLeft = new TreeMap<String, Integer>();
		Map<String,Integer> outcomeCountsRight = new TreeMap<String, Integer>();
		Set<String> outcomes = new TreeSet<String>();
		for (int i = startIndex; i <= endIndex; i++) {
			outcomes.add(featureValues.get(i).getName());
		}
		for (String outcome : outcomes) {
			outcomeCounts.put(outcome, 0);
			outcomeCountsLeft.put(outcome, 0);
			outcomeCountsRight.put(outcome, 0);
		}
		for (int i = startIndex; i <= endIndex; i++) {
			NameValuePair dataPoint = featureValues.get(i);
			int outcomeCount = outcomeCounts.get(dataPoint.getName());
			outcomeCounts.put(dataPoint.getName(), outcomeCount+1);
			if (i<=split) {
				int outcomeCountLeft = outcomeCountsLeft.get(dataPoint.getName());
				outcomeCountsLeft.put(dataPoint.getName(), outcomeCountLeft+1);
			} else {
				int outcomeCountRight = outcomeCountsRight.get(dataPoint.getName());
				outcomeCountsRight.put(dataPoint.getName(), outcomeCountRight+1);
			}
		}

		int totalCount = (endIndex-startIndex)+1;
		int leftCount = (split-startIndex)+1;
		int rightCount = endIndex-split;
		LOG.trace("totalCount: " + totalCount);
		LOG.trace("leftCount: " + leftCount);
		LOG.trace("rightCount: " + rightCount);

		// Compute entropy before split.
		priorEntropy = this.getEntropy(outcomeCounts.values(), totalCount);


		// Compute entropy after split.
		entropyLeft = this.getEntropy(outcomeCountsLeft.values(), leftCount);
		entropyRight = this.getEntropy(outcomeCountsRight.values(), rightCount);
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
		
		// Check if split is to be accepted
		return (gain > mdl);	
	}

	double log2(double value) {
		// for now we'll keep everything in base e
		return Math.log(value);
		//return Math.log(value) / LOG2;
	}
	
	/**
	 * Calculates the entropy given a bunch of counts.
	 * We could of course sum up the total in here, but since
	 * we have it in advance, it would make things less efficient.
	 * @param counts
	 * @param totalCount
	 * @return
	 */
	double getEntropy(Collection<Integer> counts, int totalCount) {
		double entropy = 0;

		for (int count : counts) {
			if (count > 0) {
				double proportion = ((double) count / (double) totalCount);
				if (proportion > 0)
					entropy -=  proportion * Math.log(proportion);
			}
		}
		// for now we'll keep entropy in base e
		// entropy = entropy / LOG2;
		return entropy;
	}

	public StopConditionTest getStopConditionTest() {
		return stopConditionTest;
	}

	public void setStopConditionTest(StopConditionTest stopConditionTest) {
		this.stopConditionTest = stopConditionTest;
	}


	
}
