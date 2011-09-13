//Copyright (C) 2011 Assaf Urieli
package com.joliciel.csvLearner.features;

/**
 * Given a training sample as a set of (x,y) where x is a real number and y is a class,
 * finds the value xsplit such that,
 * if the training sample was split into two subsets, one for all items
 * where x<=xsplit, and one for all items where x>xsplit
 * this split would provide the maximum information gain.
 * The information gain is measured in terms of information entropy.
 * @author Assaf Urieli
 *
 */public class InformationGainSplitter extends AbstractFeatureSplitter {
	private double informationGainThreshold = 0;

	@Override
	public boolean checkStopCondition(Split subset, int proposedSplit, double informationGain) {
		double normalisedThreshold = informationGainThreshold * subset.getEntropy();
		return (informationGain < normalisedThreshold);
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

}
