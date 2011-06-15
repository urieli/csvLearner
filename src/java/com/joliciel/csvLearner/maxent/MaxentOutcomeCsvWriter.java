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

import com.joliciel.csvLearner.GenericEvent;
import com.joliciel.csvLearner.NameValuePair;
import com.joliciel.csvLearner.utils.CSVFormatter;

import opennlp.model.MaxentModel;

/**
 * Writes a CSV file containing, in the columns, the list of outcomes,
 * in the rows, the list of events, and in the cells, the probability for each outcome.
 * The first two columns gived the expected and guessed outcome.
 * @author Assaf Urieli
 *
 */
public class MaxentOutcomeCsvWriter implements MaxentObserver {
	private Writer writer;
	private MaxentModel maxentModel;
	private List<String> outcomeList = new Vector<String>();
	private double minProbToConsider = 0.0;
	private String unknownOutcomeName = "";

	public MaxentOutcomeCsvWriter(MaxentModel maxentModel, File file) {
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
	public MaxentOutcomeCsvWriter(MaxentModel maxentModel, Writer outcomeFileWriter) {
		this.maxentModel = maxentModel;
		this.writer = outcomeFileWriter;
		this.initialise();
	}
	
	private void initialise() {
		try {
			writer.append("Event,Expected,Guessed,");
	
			Object[] dataStructures = maxentModel.getDataStructures();
			String[] outcomeNames = (String[]) dataStructures[2];
			TreeSet<String> outcomeSet = new TreeSet<String>();
			for (String outcome: outcomeNames)
				outcomeSet.add(outcome);
			outcomeList.addAll(outcomeSet);
			
			for (String outcome : outcomeList)
				writer.append(outcome + ",");
			writer.append("\n");
			writer.flush();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	@Override
	public void onAnalyse(GenericEvent event,
			Collection<NameValuePair> outcomes) {
		NameValuePair bestOutcome =  outcomes.iterator().next();
		String bestOutcomeName = bestOutcome.getName();
		if (bestOutcome.getValue() < minProbToConsider)
			bestOutcomeName = unknownOutcomeName;
		
		try {
			writer.append(event.getIdentifier() +",");
			writer.append(event.getOutcome() +",");
			writer.append(bestOutcomeName +",");
			Map<String,Double> outcomeWeights = new TreeMap<String, Double>();
			for (NameValuePair weightedOutcome : outcomes) {
				outcomeWeights.put(weightedOutcome.getName(), weightedOutcome.getValue());
			}
	
			for (String outcome : outcomeList) {
				Double weightObj = outcomeWeights.get(outcome);
				double weight = (weightObj==null ? 0.0 : weightObj.doubleValue());
				writer.append(CSVFormatter.format(weight)+",");
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
	public double getMinProbToConsider() {
		return minProbToConsider;
	}
	public void setMinProbToConsider(double minProbToConsider) {
		this.minProbToConsider = minProbToConsider;
	}
	public String getUnknownOutcomeName() {
		return unknownOutcomeName;
	}
	public void setUnknownOutcomeName(String unknownOutcomeName) {
		this.unknownOutcomeName = unknownOutcomeName;
	}

}
