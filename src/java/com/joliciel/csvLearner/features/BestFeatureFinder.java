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

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.csvLearner.GenericEvents;
import com.joliciel.csvLearner.NameValueDescendingComparator;
import com.joliciel.csvLearner.NameValuePair;
import com.joliciel.csvLearner.utils.CSVFormatter;

public class BestFeatureFinder {
	private static final Log LOG = LogFactory.getLog(BestFeatureFinder.class);
	private static final String TOTAL_ENTROPY = "### Total Entropy ###";
	private FeatureSplitter featureSplitter = null;
	
	public BestFeatureFinder(FeatureSplitter featureSplitter) {
		this.featureSplitter = featureSplitter;
	}
	
	public List<NameValuePair> getBestFeatures(GenericEvents events, String testOutcome, int featureCount) {
		LOG.debug("testOutcome: " + testOutcome);
		List<NameValuePair> bestFeatures = new ArrayList<NameValuePair>();

		RealValueFeatureEvaluator evaluator = new RealValueFeatureEvaluator();
		evaluator.setFeatureSplitter(featureSplitter);
		try {
			Set<String> features = events.getFeatures();

			PriorityQueue<NameValuePair> heap = new PriorityQueue<NameValuePair>(features.size(), new NameValueDescendingComparator());
			double eventSpaceEntropy = -1;
			for (String feature : features) {
				List<Double> featureEntropies = evaluator.evaluateFeature(events, feature, testOutcome);
				double informationGain = featureEntropies.get(0) - featureEntropies.get(featureEntropies.size()-1);
				if (eventSpaceEntropy<0)
					eventSpaceEntropy = featureEntropies.get(0);
				NameValuePair pair = new NameValuePair(feature, informationGain);
				heap.add(pair);
			}
			bestFeatures.add(new NameValuePair(TOTAL_ENTROPY, eventSpaceEntropy));
			for (int i = 0; i< featureCount; i++) {
				NameValuePair pair = heap.poll();
				if (pair==null)
					break;
				LOG.debug("feature: " + pair.getName() + ", " + pair.getValue());
	
				bestFeatures.add(pair);
			}
			heap = null;
		} finally {
			evaluator.logPerformance();
		}
		return bestFeatures;
	}

	public FeatureSplitter getFeatureSplitter() {
		return featureSplitter;
	}
	
	public void writeBestFeatures(Writer writer, Map<String,Collection<NameValuePair>> bestFeatures) {
		try {
			for (Entry<String,Collection<NameValuePair>> entry : bestFeatures.entrySet()) {
				writer.append(CSVFormatter.format(entry.getKey()) + ",");
				for (NameValuePair pair : entry.getValue()) {
					if (!pair.getName().equals(TOTAL_ENTROPY))
						writer.append(CSVFormatter.format(pair.getName()) + ",");
					writer.append(CSVFormatter.format(pair.getValue()) + ",");
				}
				writer.append("\n");
				writer.flush();
			}			
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	public void writeFirstLine(Writer writer, int numFeatures) {
		try {
			writer.append(CSVFormatter.format("outcome") + ",");
			writer.append(CSVFormatter.format("total entropy") + ",");
			for (int i = 1; i<=numFeatures; i++) {
				writer.append(CSVFormatter.format(i) + ",");
				writer.append("gain,");
			}
			writer.append("\n");
			writer.flush();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	public void writeBestFeatures(Writer writer, String outcome, Collection<NameValuePair> bestFeatures) {
		try {
			writer.append(CSVFormatter.format(outcome) + ",");
			for (NameValuePair pair : bestFeatures) {
				if (!pair.getName().equals(TOTAL_ENTROPY))
					writer.append(CSVFormatter.format(pair.getName()) + ",");
				writer.append(CSVFormatter.format(pair.getValue()) + ",");
			}
			writer.append("\n");
			writer.flush();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	public void writeFeatureList(Writer writer, Map<String,Collection<NameValuePair>> bestFeatureMap, int featureListSize) {
		try {
			Set<String> features = new TreeSet<String>();
			for (Collection<NameValuePair> bestFeatures : bestFeatureMap.values()) {
				int i = 0;
				for (NameValuePair pair : bestFeatures) {
					features.add(pair.getName());
					i++;
					if (i==featureListSize)
						break;
				}
			}
			for (String feature : features) {
				writer.append(feature);
				writer.append("\n");
			}
			writer.flush();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
}
