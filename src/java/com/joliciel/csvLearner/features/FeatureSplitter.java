//Copyright (C) 2011 Assaf Urieli
package com.joliciel.csvLearner.features;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.joliciel.csvLearner.NameValuePair;

public interface FeatureSplitter {
	public enum FeatureSplitterType {
		INFORMATION_GAIN_PERCENT,
		FAYYAD_IRANI,
		REGULAR_INTERVALS
	}

	/**
	 * Split a list multiple times, until no split is found with sufficient information gain.
	 * @param featureValues
	 * @return
	 */
	public abstract List<Integer> split(List<NameValuePair> featureValues);

	/**
	 * Returns the splits obtained at each level of splitting, where
	 * each level results in at most 2 splits for each single split at the previous level.
	 * Must be called immediately after split(List&lt;NameValuePair&gt;) to be reliable.
	 * @return
	 */
	public abstract Map<Integer, Set<Split>> getSplitsByDepth();

}