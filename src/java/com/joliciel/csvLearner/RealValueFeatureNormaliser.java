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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Take a real-valued feature, and normalises it to a certain scale.
 * @author Assaf Urieli
 *
 */
public class RealValueFeatureNormaliser {
	public enum NormaliseMethod {
		/** Normalise based on the max value for a given feature = normalisedMax **/
		NORMALISE_BY_MAX,
		/** Normalise based on the mean value for a given feature = normalisedMax / 2 **/
		NORMALISE_BY_MEAN
	}
	private static final Log LOG = LogFactory.getLog(RealValueFeatureNormaliser.class);
	private CSVEventListReader eventListReader;
	private GenericEvents events;
	private float normalisedMax = 1;
	private NormaliseMethod normaliseMethod = NormaliseMethod.NORMALISE_BY_MAX;
	private Map<String, Float> featureToMaxMap = new TreeMap<String, Float>();
	private boolean normaliseToPreviousMaxValues = false;
	
	public RealValueFeatureNormaliser(CSVEventListReader eventListReader, GenericEvents events) {
		this.eventListReader = eventListReader;
		this.events = events;
	}
	
	public RealValueFeatureNormaliser(Map<String, Float> featureToMaxMap, GenericEvents events) {
		this.featureToMaxMap = featureToMaxMap;
		this.events = events;
		normaliseToPreviousMaxValues = true;
	}

	/**
	 * Peform the normalisation.
	 */
	public void normalise() {
		for (GenericEvent event : this.events) {
			if (LOG.isTraceEnabled())
				LOG.trace("Normalising " + event.getIdentifier());
			List<Float> newWeights = new Vector<Float>();
			for (int j=0;j<event.getFeatures().size();j++) {
				String featureName = event.getFeatures().get(j);
				boolean nominalFeature = featureName.contains(CSVLearner.NOMINAL_MARKER);
				float weight = event.getWeights().get(j);
				if (nominalFeature) {
					newWeights.add(weight);
				} else if (normaliseToPreviousMaxValues) {
					Float maxValueObj = this.featureToMaxMap.get(featureName);
					float maxValue = maxValueObj==null ? 0 : maxValueObj.floatValue();
					float newWeight = maxValue==0 ? weight : (weight/maxValue) * this.normalisedMax;
					newWeights.add(newWeight);
				} else if (this.normaliseMethod.equals(NormaliseMethod.NORMALISE_BY_MAX)) {
					float maxValue = this.eventListReader.getMax(featureName);
					float newWeight = (weight/maxValue) * this.normalisedMax;
					newWeights.add(newWeight);
					if (!featureToMaxMap.containsKey(featureName))
						featureToMaxMap.put(featureName, maxValue);
				} else {
					float meanValue = this.eventListReader.getMean(featureName);
					float newWeight = (weight/meanValue) * (this.normalisedMax / 2);
					newWeights.add(newWeight);
					if (!featureToMaxMap.containsKey(featureName))
						featureToMaxMap.put(featureName, meanValue*2.0f);
				}
			}
			event.getWeights().clear();
			event.getWeights().addAll(newWeights);
		}
	}

	public float getNormalisedMax() {
		return normalisedMax;
	}

	public void setNormalisedMax(float normalisedMax) {
		this.normalisedMax = normalisedMax;
	}

	/**
	 * How do we perform the normalisation (based on max or mean?)
	 * @return
	 */
	public NormaliseMethod getNormaliseMethod() {
		return normaliseMethod;
	}

	public void setNormaliseMethod(NormaliseMethod normaliseMethod) {
		this.normaliseMethod = normaliseMethod;
	}

	/**
	 * For each non-nominal feature, gives the value which has been mapped to 1.0
	 * (this is either the max, or 2.0 * mean, depending on the method).
	 * @return
	 */
	public Map<String, Float> getFeatureToMaxMap() {
		return featureToMaxMap;
	}
	
	
}
