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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.joliciel.csvLearner.utils.CSVFormatter;

public class FeatureEntropyWriter {
	private Writer writer;
	
	public FeatureEntropyWriter(File file) {
		try {
			file.delete();
			file.createNewFile();
			this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false),"UTF8"));
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	public FeatureEntropyWriter(Writer writer) {
		this.writer = writer;
	}
	
	public void writeFile(Map<String,List<Double>> featureEntropies) {
		try {
			try {
				int maxLimitCount = 0;
				for (List<Double> entropies: featureEntropies.values()) {
					if (entropies.size()>maxLimitCount)
						maxLimitCount = entropies.size();
				}
				writer.append("feature,");

				for (int i = 0;i<maxLimitCount;i++) {
					writer.append("level "+ i +",");
				}
				
				writer.append("\n");
				writer.flush();
				
				for (Entry<String,List<Double>> featureEntropyEntry: featureEntropies.entrySet()) {
					writer.append(CSVFormatter.format(featureEntropyEntry.getKey())+",");
					for (double entropy : featureEntropyEntry.getValue()) {
						writer.append(CSVFormatter.format(entropy)+",");
					}
					writer.append("\n");
					writer.flush();
				}
			} finally {
				writer.flush();
				writer.close();
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
}
