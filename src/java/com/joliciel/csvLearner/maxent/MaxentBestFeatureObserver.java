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
package com.joliciel.csvLearner.maxent;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import opennlp.model.Context;
import opennlp.model.IndexHashTable;
import opennlp.model.MaxentModel;

import com.joliciel.csvLearner.CSVLearner;
import com.joliciel.csvLearner.GenericEvent;
import com.joliciel.csvLearner.NameValueDescendingComparator;
import com.joliciel.csvLearner.NameValuePair;
import com.joliciel.csvLearner.utils.CSVFormatter;

/**
 * Calculate feature weight per outcome during analysis.
 * Note that this weight is fairly meaningless on an individual feature basis.
 * However, for an entire file, it can give an idea of the weight of that file in the overall calculation.
 * @author Assaf Urieli
 *
 */
public class MaxentBestFeatureObserver implements MaxentObserver {
	private static final Log LOG = LogFactory.getLog(MaxentBestFeatureObserver.class);
	private MaxentModel maxentModel;
	private int n = 0;
	private List<String> outcomeList = new ArrayList<String>();
	private String[] predicates;
	private Context[] modelParameters;
	private String[] outcomeNames;
	private IndexHashTable<String> predicateTable;
	
	private Map<String,Map<String,Double>> featureMap = new HashMap<String, Map<String,Double>>();
	private Map<String,List<NameValuePair>> bestFeaturesPerOutcome;
	private Map<String,Double> totalPerOutcome;
	private Map<String,Double> bestFeatureTotalPerOutcome;
	private Map<String,String> featureToFileMap;
	private Map<String,Map<String,Double>> filePercentagePerOutcome;
	private Set<String> fileNames;
	
	public MaxentBestFeatureObserver(MaxentModel maxentModel, int n, Map<String,String> featureToFileMap) {
		this.maxentModel = maxentModel;
		this.n = n;
		this.featureToFileMap = featureToFileMap;
		this.initialise();
	}
	
	@SuppressWarnings("unchecked")
	private void initialise() {
		Object[] dataStructures = maxentModel.getDataStructures();
		outcomeNames = (String[]) dataStructures[2];
		TreeSet<String> outcomeSet = new TreeSet<String>();
		for (String outcome: outcomeNames)
			outcomeSet.add(outcome);
		outcomeList.addAll(outcomeSet);
		this.predicateTable = (IndexHashTable<String>) dataStructures[1];
		predicates = new String[predicateTable.size()];
		predicateTable.toArray(predicates);
		modelParameters = (Context[]) dataStructures[0];
	}
	
	@Override
	public void onAnalyse(GenericEvent event,
			Collection<NameValuePair> outcomes) {

		Map<String,Double> featureTotals = featureMap.get(event.getOutcome());
		if (featureTotals == null) {
			featureTotals = new TreeMap<String, Double>();
			featureMap.put(event.getOutcome(), featureTotals);
		}
		
		for (int i=0; i<event.getFeatures().size(); i++ ) {
			String feature = event.getFeatures().get(i);
			double value = event.getWeights().get(i);
			
			double currentTotal = 0.0;
			Double currentTotalObj = featureTotals.get(feature);
			if (currentTotalObj!=null)
				currentTotal = currentTotalObj.doubleValue();

			int predicateIndex = predicateTable.get(feature);
			if (predicateIndex >=0) {
				Context context = modelParameters[predicateIndex];
				int[] outcomeIndexes = context.getOutcomes();
				double[] parameters = context.getParameters();

				int outcomeIndex = -1;
				for (int j=0;j<outcomeNames.length;j++) {
					if (outcomeNames[j].equals(event.getOutcome())) {
						outcomeIndex = j;
						break;
					}
				}
				int paramIndex = -1;
				for (int k=0;k<outcomeIndexes.length;k++) {
					if (outcomeIndexes[k]==outcomeIndex) {
						paramIndex = k;
						break;
					}
				}
				double weight = 0.0;
				if (paramIndex>=0)
					weight = parameters[paramIndex];
				
				double total = Math.exp(value * weight);
				currentTotal += total;
			}
			featureTotals.put(feature, currentTotal);
		}

	}
	
	@Override
	public void onTerminate() {
		bestFeaturesPerOutcome = new TreeMap<String, List<NameValuePair>>();
		totalPerOutcome = new TreeMap<String, Double>();
		bestFeatureTotalPerOutcome = new TreeMap<String, Double>();
		filePercentagePerOutcome = new TreeMap<String, Map<String,Double>>();
		fileNames = new TreeSet<String>();
		for (Entry<String, Map<String,Double>> entry : featureMap.entrySet()) {
			String outcome = entry.getKey();
			LOG.debug("outcome: " + outcome);
			Map<String,Double> featureTotals = entry.getValue();
			Map<String,Double> fileTotals = new TreeMap<String, Double>();
			PriorityQueue<NameValuePair> heap = new PriorityQueue<NameValuePair>(featureTotals.size(), new NameValueDescendingComparator());
			double grandTotal = 0.0;
			for (Entry<String,Double> featureTotal : featureTotals.entrySet()) {
				NameValuePair pair = new NameValuePair(featureTotal.getKey(), featureTotal.getValue());
				heap.add(pair);
				grandTotal += featureTotal.getValue();
				String featureKey = featureTotal.getKey();
				if (featureKey.contains(CSVLearner.NOMINAL_MARKER))
					featureKey = featureKey.substring(0, featureKey.indexOf(CSVLearner.NOMINAL_MARKER));
				String fileName = this.featureToFileMap.get(featureKey);
				Double fileTotalObj = fileTotals.get(fileName);
				double fileTotal = fileTotalObj==null ? 0 : fileTotalObj.doubleValue();
				fileTotals.put(fileName, fileTotal + featureTotal.getValue());
			}
			List<NameValuePair> bestFeatures = new ArrayList<NameValuePair>();
			double bestFeatureTotal = 0.0;
			for (int i=0;i<n;i++) {
				NameValuePair pair = heap.poll();
				if (pair==null)
					break;

				LOG.debug("Feature: " + pair.getName() + ", Total: " + pair.getValue());
				bestFeatures.add(pair);
				bestFeatureTotal += pair.getValue();
			}
			bestFeaturesPerOutcome.put(outcome, bestFeatures);
			totalPerOutcome.put(outcome, grandTotal);
			bestFeatureTotalPerOutcome.put(outcome, bestFeatureTotal);
			
			// convert the file totals to percentages
			for (Entry<String,Double> fileTotal : fileTotals.entrySet()) {
				double filePercentage = fileTotal.getValue() / grandTotal;
				fileTotal.setValue(filePercentage);
				fileNames.add(fileTotal.getKey());
			}
			filePercentagePerOutcome.put(outcome, fileTotals);
			
			featureTotals.clear();
		}
		featureMap.clear();
		featureMap = null;
	}

	public Map<String, List<NameValuePair>> getBestFeaturesPerOutcome() {
		return bestFeaturesPerOutcome;
	}
	
	public void writeFileTotalsToFile(Writer writer) {
		try {
			boolean firstOutcome = true;
			Map<String,Double> totalPerFile = new HashMap<String, Double>();
			for (String fileName : fileNames)
				totalPerFile.put(fileName, 0.0);
			
			for (String outcome : filePercentagePerOutcome.keySet()) {
				if (firstOutcome) {
					writer.append("outcome,");
					for (String fileName : fileNames)
						writer.append(CSVFormatter.format(fileName) + ",");
					writer.append("\n");
				}
				writer.append(CSVFormatter.format(outcome) + ",");
				for (String fileName : fileNames) {
					Double filePercentageObj = filePercentagePerOutcome.get(outcome).get(fileName);
					double filePercentage = filePercentageObj==null ? 0 : filePercentageObj.doubleValue();
					double currentTotal = totalPerFile.get(fileName);
					totalPerFile.put(fileName, currentTotal + filePercentage);
					writer.append(CSVFormatter.format(filePercentage) + ",");
				}
				writer.append("\n");
				firstOutcome = false;
			}
			
			writer.append(CSVFormatter.format("AVERAGE") + ",");
			for (String fileName : fileNames) {
				double total = totalPerFile.get(fileName);
				writer.append(CSVFormatter.format(total/filePercentagePerOutcome.size()) + ",");
			}			
			writer.append("\n");
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
}
