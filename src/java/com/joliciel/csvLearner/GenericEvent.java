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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

/**
 * A single event, corresponding to a uniquely identified row in the CSV files.
 * @author Assaf Urieli
 *
 */
public class GenericEvent {
	private List<String> features = new Vector<String>();
	private List<Float> weights = new Vector<Float>();
	private Map<String,Integer> featureIndex = new TreeMap<String, Integer>();
	private int currentIndex = 0;
	
	private String outcome = "";
	private String identifier = "";
	private boolean test;
	
	public GenericEvent(String identifier) {
		this.identifier = identifier;
	}
	public int getFeatureIndex(String feature) {
		Integer featureIndexObj = this.featureIndex.get(feature);
		int i = featureIndexObj==null ? -1 : featureIndexObj.intValue();
		return i;
	}
	
	public void addFeature(String feature, float weight) {
		this.features.add(feature);
		// add the feature's base name if it's a nominal feature
		int nominalStartIndex = feature.indexOf(CSVLearner.NOMINAL_MARKER);
		if (nominalStartIndex>=0)
			this.featureIndex.put(feature.substring(0, nominalStartIndex), currentIndex);
		this.featureIndex.put(feature, currentIndex++);
		this.weights.add(weight);
	}
	
	public void addFeature(String feature) {
		this.addFeature(feature, 1);
	}
	
	public List<String> getFeatures() {
		return features;
	}
	public void setFeatures(List<String> features) {
		this.features = features;
	}
	public List<Float> getWeights() {
		return weights;
	}
	public void setWeights(List<Float> weights) {
		this.weights = weights;
	}
	public String getOutcome() {
		return outcome;
	}
	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}
	public boolean isTest() {
		return test;
	}
	public void setTest(boolean test) {
		this.test = test;
	}
	public String getIdentifier() {
		return identifier;
	}
	
	
}
