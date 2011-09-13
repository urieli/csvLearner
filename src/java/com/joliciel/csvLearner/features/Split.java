//Copyright (C) 2011 Assaf Urieli
package com.joliciel.csvLearner.features;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.joliciel.csvLearner.NameValuePair;

class Split implements Comparable<Split> {
	private List<NameValuePair> featureValues;
	private int startIndex;
	private int endIndex;
	private double entropy;
	private boolean entropyCalculated = false;
	private Map<String,Integer> outcomeCounts = null;
	private int maxOutcomeCount = 0;
	
	public Split(List<NameValuePair> featureValues, int startIndex, int endIndex) {
		this.featureValues = featureValues;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
	}

	public List<NameValuePair> getFeatureValues() {
		return featureValues;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public int getSize() {
		return (endIndex-startIndex)+1;
	}
	
	public Map<String, Integer> getOutcomeCounts() {
		if (outcomeCounts==null) {
			outcomeCounts = new TreeMap<String, Integer>();
			for (int i = startIndex; i <= endIndex; i++) {
				NameValuePair dataPoint = featureValues.get(i);
				Integer outcomeCountObj = outcomeCounts.get(dataPoint.getName());
				int outcomeCount = outcomeCountObj==null ? 0 : outcomeCountObj.intValue();
				outcomeCount++;
				if (outcomeCount>maxOutcomeCount)
					maxOutcomeCount = outcomeCount;
				outcomeCounts.put(dataPoint.getName(), outcomeCount);
			}
		}
		return outcomeCounts;
	}

	public double getEntropy() {
		if (!entropyCalculated) {
			entropy = EntropyCalculator.getEntropy(this.getOutcomeCounts().values(), (endIndex-startIndex)+1);
			entropyCalculated = true;
		}
		return entropy;
	}

	public void setEntropy(double entropy) {
		this.entropy = entropy;
		this.entropyCalculated = true;
	}

	public int getMaxOutcomeCount() {
		return maxOutcomeCount;
	}
	
	@Override
	public int compareTo(Split o) {
		if (this.getStartIndex()!=o.getStartIndex())
			return this.getStartIndex() - o.getStartIndex();
		return this.getEndIndex() - o.getEndIndex();
	}
	
}
