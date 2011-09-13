//Copyright (C) 2011 Assaf Urieli
package com.joliciel.csvLearner.features;

import java.util.Collection;

public class EntropyCalculator {
	/**
	 * Calculates the entropy given a bunch of counts.
	 * This version sums up the total internally..
	 * @param counts
	 * @param totalCount
	 * @return
	 */
	static double getEntropy(Collection<Integer> counts) {
		int totalCount = 0;
		for (int count : counts)
			totalCount += count;
		return EntropyCalculator.getEntropy(counts, totalCount);
	}
	
	/**
	 * Calculates the entropy given a bunch of counts.
	 * We could of course sum up the total in here, but since
	 * we have it in advance, it would make things less efficient.
	 * @param counts
	 * @param totalCount
	 * @return
	 */
	static double getEntropy(Collection<Integer> counts, int totalCount) {
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
}
