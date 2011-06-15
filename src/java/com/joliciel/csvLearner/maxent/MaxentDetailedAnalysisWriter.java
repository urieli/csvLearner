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
package com.joliciel.csvLearner.maxent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import opennlp.model.Context;
import opennlp.model.IndexHashTable;
import opennlp.model.MaxentModel;

import com.joliciel.csvLearner.GenericEvent;
import com.joliciel.csvLearner.NameValuePair;
import com.joliciel.csvLearner.utils.CSVFormatter;

/**
 * Writes a text file with a detailed analysis of what was calculated for each event.
 * @author Assaf Urieli
 *
 */
public class MaxentDetailedAnalysisWriter implements MaxentObserver {
	private Writer writer;
	private MaxentModel maxentModel;
	private List<String> outcomeList = new Vector<String>();
	private String[] predicates;
	private Context[] modelParameters;
	private String[] outcomeNames;
	private IndexHashTable<String> predicateTable;
	
	public MaxentDetailedAnalysisWriter(MaxentModel maxentModel, File file) {
		this.maxentModel = maxentModel;
		try {
			file.delete();
			file.createNewFile();
			this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false),"UTF8"));
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		this.initialise();
	}
	public MaxentDetailedAnalysisWriter(MaxentModel maxentModel, Writer outcomeFileWriter) {
		this.maxentModel = maxentModel;
		this.writer = outcomeFileWriter;
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
		NameValuePair bestOutcome = outcomes.iterator().next();
		
		try {
			Map<String, Double> outcomeTotals = new TreeMap<String, Double>();
			double uniformPrior = Math.log(1 / (double) outcomeList.size());
			
			for (String outcome : outcomeList)
				outcomeTotals.put(outcome, uniformPrior);
			
			writer.append("####### Event " + event.getIdentifier() +"\n");
			writer.append("Expected: " + event.getOutcome() +"\n");
			writer.append("Guessed: " + bestOutcome.getName() +"\n");
			
			writer.append("### Feature list:\n");
			for (int i=0; i<event.getFeatures().size(); i++ ) {
				String feature = event.getFeatures().get(i);
				double value = event.getWeights().get(i);
				writer.append("#" + feature + "\t");
				writer.append("value=" + CSVFormatter.format(value) + "\n");
				
				writer.append(String.format("%1$-30s", "outcome")  + String.format("%1$#15s","weight") +  String.format("%1$#15s","total") + "\n");
				int predicateIndex = predicateTable.get(feature);
				if (predicateIndex >=0) {
					Context context = modelParameters[predicateIndex];
					int[] outcomeIndexes = context.getOutcomes();
					double[] parameters = context.getParameters();
					for (String outcome : outcomeList) {
						int outcomeIndex = -1;
						for (int j=0;j<outcomeNames.length;j++) {
							if (outcomeNames[j].equals(outcome)) {
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
						
						double total = value * weight;
						writer.append(String.format("%1$-30s", outcome)  + String.format("%1$#15s",CSVFormatter.format(weight)) +  String.format("%1$#15s",CSVFormatter.format(total)) + "\n");
					
						double runningTotal = outcomeTotals.get(outcome);
						runningTotal += total;
						outcomeTotals.put(outcome, runningTotal);
					}
				}
				writer.append("\n");
			}
			
			writer.append("### Outcome totals:\n");
			writer.append("# Uniform prior: " + uniformPrior + " (=1/" + outcomeList.size() + ")\n");
			
			double grandTotal = 0;
			for (String outcome : outcomeList) {
				double total = outcomeTotals.get(outcome);
				double expTotal = Math.exp(total);
				grandTotal += expTotal;
			}
			writer.append(String.format("%1$-30s", "outcome") + String.format("%1$#15s","total(log)") + String.format("%1$#15s","total") + String.format("%1$#15s","normalised")+"\n");
			
			for (String outcome : outcomeList) {
				double total = outcomeTotals.get(outcome);
				double expTotal = Math.exp(total);
				writer.append(String.format("%1$-30s", outcome) + String.format("%1$#15s",CSVFormatter.format(total)) + String.format("%1$#15s",CSVFormatter.format(expTotal)) + String.format("%1$#15s",CSVFormatter.format(expTotal/grandTotal))+"\n");
			}
			writer.append("\n");
			
			Map<String,Double> outcomeWeights = new TreeMap<String, Double>();
			for (NameValuePair weightedOutcome : outcomes) {
				outcomeWeights.put(weightedOutcome.getName(), weightedOutcome.getValue());
			}
			
			writer.append("### Outcome list:\n");
			for (String outcome : outcomeList) {
				Double weightObj = outcomeWeights.get(outcome);
				double weight = (weightObj==null ? 0.0 : weightObj.doubleValue());
				writer.append(String.format("%1$-30s", outcome) + String.format("%1$#15s",CSVFormatter.format(weight))+"\n");
			}
			writer.append("\n");
			writer.flush();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	@Override
	public void onTerminate() {
		try {
			this.writer.flush();
			this.writer.close();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}			
	}
}
