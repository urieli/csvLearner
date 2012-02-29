//Copyright (C) 2011 Assaf Urieli
package com.joliciel.csvLearner.features;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.ArrayList;

import com.joliciel.csvLearner.NameValuePair;

/**
 * Split a list at regular value intervals, where the intervals are
 * determined by maxValue / 2 ^ maxDepth.
 * @author Assaf Urieli
 *
 */
public class RegularIntervalSplitter implements FeatureSplitter {
	int maxDepth = 1;
	private Map<Integer,Set<Split>> splitsByDepth = null;
	
	@Override
	public List<Integer> split(List<NameValuePair> featureValues) {
		splitsByDepth = new TreeMap<Integer, Set<Split>>();
		double maxValue = featureValues.get(featureValues.size()-1).getValue();
		
		double interval = maxValue;
		for (int depth = 1; depth<=maxDepth; depth++) {
			interval = interval / 2;
			double upperLimit = interval;
			Set<Split> currentLevelSplits = new TreeSet<Split>();
			this.splitsByDepth.put(depth, currentLevelSplits);
			int i = 0;
			int lastSplit = 0;
			for (NameValuePair pair : featureValues) {
				if (pair.getValue()>upperLimit) {
					if (i>0)
						currentLevelSplits.add(new Split(featureValues, lastSplit, i-1));
					lastSplit = i;
					while (pair.getValue()>upperLimit)
						upperLimit += interval;
					if (upperLimit>=maxValue)
						break;
				}
				i++;
			}
			if (lastSplit<featureValues.size()-1)
				currentLevelSplits.add(new Split(featureValues, lastSplit, featureValues.size()-1));
		}
		
		List<Integer> splits = new ArrayList<Integer>();
		for (Split split : splitsByDepth.get(maxDepth))
			splits.add(split.getEndIndex());
		return splits;
	}

	@Override
	public Map<Integer, Set<Split>> getSplitsByDepth() {
		return this.splitsByDepth;
	}

	public int getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

}
