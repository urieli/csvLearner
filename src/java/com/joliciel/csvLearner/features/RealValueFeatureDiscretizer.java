///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2011 Assaf Urieli
//
//This file is part of csvLearner.
//
//csvLearner is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//csvLearner is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with csvLearner.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.csvLearner.features;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.csvLearner.CSVLearner;
import com.joliciel.csvLearner.GenericEvent;
import com.joliciel.csvLearner.GenericEvents;
import com.joliciel.csvLearner.NameValuePair;

/**
 * Take a real-valued feature, and converts it to a set of (binary) classifying features
 * based on value thresholds giving sufficient information gain.
 * @author Assaf Urieli
 *
 */
public class RealValueFeatureDiscretizer {
	private static final Log LOG = LogFactory.getLog(RealValueFeatureDiscretizer.class);
	
	private FeatureSplitter featureSplitter = null;

	/**
	 * Transform a given real-valued feature into a set of discrete features, each with a separate class name.
	 * Will update the list of events to include the new discrete feature.
	 * @param events the list of events
	 * @param feature the feature to consider for splitting
	 * @param informationGainThreshold as a % of the total entropy - if max gain < this threshold, we won't bother splitting
	 * @param minNodeSize the min node size to consider for splitting
	 * @param maxDepth the max depth to consider for splitting - if maxDepth=n, we will have at most 2^n splits
	 * @param minErrorRate if a node has error rate < minErrorRate, don't bother splitting
	 * @return a set of upper-limit values for each discrete class
	 */
	public Set<Double> discretizeFeature(GenericEvents events, String feature) {
		LOG.debug("Discretizing feature: " + feature);
		Set<Double> splitValues = new TreeSet<Double>();
		// don't split features that are already nominal
		if (feature.contains(CSVLearner.NOMINAL_MARKER))
			return splitValues;
		splitValues = this.findFeatureSplitValues(events, feature);

		for (GenericEvent event : events) {
			int featureIndex = event.getFeatures().indexOf(feature);
			if (featureIndex>=0) {
				double weight = event.getWeights().get(featureIndex);
				event.getFeatures().remove(featureIndex);
				event.getWeights().remove(featureIndex);
				int i = 0;
				boolean foundCategory = false;
				for (double splitValue : splitValues) {
					if (weight <= splitValue) {
						foundCategory = true;
						event.addFeature(feature + CSVLearner.NOMINAL_MARKER + "c" + i);
						break;
					}
					i++;
				}
				if (!foundCategory)
					event.addFeature(feature + CSVLearner.NOMINAL_MARKER + "c" + i);
			}
		}

		return splitValues;
	}
	
	public Set<Double> findFeatureSplitValues(GenericEvents events, String feature) {
		Collection<NameValuePair> featureValues = new TreeSet<NameValuePair>();
		for (GenericEvent event : events) {
			if (!event.isTest()) {
				int featureIndex = event.getFeatures().indexOf(feature);
				if (featureIndex>=0)
					featureValues.add(new NameValuePair(event.getOutcome(), event.getWeights().get(featureIndex)));
			}
		}
		List<NameValuePair> featureValueList = new ArrayList<NameValuePair>(featureValues);

		List<Integer> splits = featureSplitter.split(featureValueList);
		Set<Double> splitValues = new TreeSet<Double>();
		for (int split : splits) {
			NameValuePair before = featureValueList.get(split);
			NameValuePair after = featureValueList.get(split+1);
			double splitValue = (before.getValue() + after.getValue()) / 2.0;
			splitValues.add(splitValue);
			LOG.debug("Split " + split + ", split value: " + splitValue);
		}
		return splitValues;
	}

	public FeatureSplitter getFeatureSplitter() {
		return featureSplitter;
	}

	public void setFeatureSplitter(FeatureSplitter featureSplitter) {
		this.featureSplitter = featureSplitter;
	}

	
	
}
