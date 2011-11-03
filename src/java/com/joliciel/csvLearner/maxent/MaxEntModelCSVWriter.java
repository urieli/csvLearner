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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.csvLearner.utils.CSVFormatter;
import com.joliciel.csvLearner.utils.LogUtils;
import com.joliciel.csvLearner.utils.StringComparatorIgnoreCase;

import opennlp.model.Context;
import opennlp.model.IndexHashTable;
import opennlp.model.MaxentModel;

/**
 * A class for writing a MaxEnt model to a CSV file.
 * @author Assaf Urieli
 *
 */
public class MaxEntModelCSVWriter {
    private static final Log LOG = LogFactory.getLog(MaxEntModelCSVWriter.class);
	MaxentModel model = null;
	String csvFilePath = null;
	boolean top100 = false;

	public void writeCSVFile() {
		try {
			Object[] dataStructures = model.getDataStructures();
			Context[] modelParameters = (Context[]) dataStructures[0];
			@SuppressWarnings("unchecked")
			IndexHashTable<String> predicateTable = (IndexHashTable<String>) dataStructures[1];
			String[] outcomeNames = (String[]) dataStructures[2];
			String[] predicates = new String[predicateTable.size()];
			predicateTable.toArray(predicates);
			
			Writer csvFileWriter = null;
			
			if (csvFilePath!=null&&csvFilePath.length()>0) {
				File csvFile = new File(csvFilePath);
				csvFile.delete();
				csvFile.createNewFile();
				csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
			}
			try {
				if (top100) {
					Map<String,Integer> outcomeMap = new TreeMap<String, Integer>();
					for (int i=0;i<outcomeNames.length;i++) {
						String outcomeName = outcomeNames[i];
						outcomeMap.put(outcomeName, i);
					}
					for (String outcome : outcomeMap.keySet()) {
						csvFileWriter.write(CSVFormatter.format(outcome) + ",,");
					}
					csvFileWriter.write("\n");
					
					Map<String,Set<MaxentParameter>> outcomePredicates = new HashMap<String, Set<MaxentParameter>>();
					for (int i=0;i<modelParameters.length;i++) {
						Context context = modelParameters[i];
						int[] outcomeIndexes = context.getOutcomes();
						double[] parameters = context.getParameters();
						for (int j=0;j<outcomeIndexes.length;j++) {
							int outcomeIndex = outcomeIndexes[j];
							String outcomeName = outcomeNames[outcomeIndex];
							double value = parameters[j];
							Set<MaxentParameter> outcomeParameters = outcomePredicates.get(outcomeName);
							if (outcomeParameters==null) {
								outcomeParameters = new TreeSet<MaxentParameter>();
								outcomePredicates.put(outcomeName, outcomeParameters);
							}
							MaxentParameter param = new MaxentParameter(predicates[i], value);
							outcomeParameters.add(param);
						}
					}
					
					for (int i=0;i<100;i++) {
						for (String outcome : outcomeMap.keySet()) {
							Set<MaxentParameter> outcomeParameters = outcomePredicates.get(outcome);
							if (outcomeParameters==null) {
								csvFileWriter.write(",,");
							} else {
								Iterator<MaxentParameter> iParams = outcomeParameters.iterator();
								MaxentParameter param = null;
								for (int j=0;j<=i;j++) {
									if (iParams.hasNext()) {
										param = iParams.next();
									} else {
										param = null;
										break;
									}
								}
								if (param==null)
									csvFileWriter.write(",,");
								else
									csvFileWriter.write("\"" + CSVFormatter.format(param.getPredicate()) + "\"," + CSVFormatter.format(param.getValue()) + ",");
							}
						}
						csvFileWriter.write("\n");
					}
				} else {
					Set<String> predicateSet = new TreeSet<String>(new StringComparatorIgnoreCase());
					for (String predicate : predicates)
						predicateSet.add(predicate);
					csvFileWriter.write("predicate,");
					for (String outcomeName : outcomeNames) {
						csvFileWriter.write(outcomeName + ",");
					}
					csvFileWriter.write("\n");
					
					for (String predicate : predicateSet) {
						csvFileWriter.write(CSVFormatter.format(predicate) + ",");
						int predicateIndex = predicateTable.get(predicate);
						Context context = modelParameters[predicateIndex];
						int[] outcomeIndexes = context.getOutcomes();
						double[] parameters = context.getParameters();
						for (int j=0;j<outcomeNames.length;j++) {
							int paramIndex = -1;
							for (int k=0;k<outcomeIndexes.length;k++) {
								if (outcomeIndexes[k]==j) {
									paramIndex = k;
									break;
								}
							}
							if (paramIndex>=0) {
								double value = parameters[paramIndex];
								csvFileWriter.write(CSVFormatter.format(Math.exp(value)) + ",");
							} else {
								csvFileWriter.write(CSVFormatter.format(0) + ",");
							}
						}
						csvFileWriter.write("\n");
					}
				}
			} finally {
				if (csvFileWriter!=null) {
					csvFileWriter.flush();
					csvFileWriter.close();
				}
			}
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	private static class MaxentParameter implements Comparable<MaxentParameter> {
		private String predicate;
		private double value;
		public MaxentParameter(String predicate, double value) {
			this.predicate = predicate;
			this.value = value;
		}
		public String getPredicate() {
			return predicate;
		}
		public double getValue() {
			return value;
		}
		@Override
		public int compareTo(MaxentParameter o) {
			if (this.getValue()==o.getValue()) {
				return this.getPredicate().compareTo(o.getPredicate());
			}
			if (o.getValue()>this.getValue())
				return 1;
			return -1;
		}	
		
	}

	
	public MaxentModel getModel() {
		return model;
	}

	public void setModel(MaxentModel model) {
		this.model = model;
	}

	public String getCsvFilePath() {
		return csvFilePath;
	}

	public void setCsvFilePath(String csvFilePath) {
		this.csvFilePath = csvFilePath;
	}

	public boolean isTop100() {
		return top100;
	}

	public void setTop100(boolean top100) {
		this.top100 = top100;
	}
	
	
}
