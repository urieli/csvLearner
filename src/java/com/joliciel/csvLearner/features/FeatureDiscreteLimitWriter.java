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
package com.joliciel.csvLearner.features;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.joliciel.csvLearner.utils.CSVFormatter;

public class FeatureDiscreteLimitWriter {
	private Writer writer;
	
	public FeatureDiscreteLimitWriter(File file) {
		try {
			file.delete();
			file.createNewFile();
			this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false),"UTF8"));
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	public FeatureDiscreteLimitWriter(Writer writer) {
		this.writer = writer;
	}
	
	public void writeFile(Map<String,Set<Double>> discretizationLimits) {
		try {
			try {
				int maxLimitCount = 0;
				for (Set<Double> limits: discretizationLimits.values()) {
					if (limits.size()>maxLimitCount)
						maxLimitCount = limits.size();
				}
				writer.append("feature,");

				for (int i = 0;i<=maxLimitCount;i++) {
					writer.append("c"+ i +",");
				}
				
				writer.append("\n");
				writer.flush();
				
				for (Entry<String,Set<Double>> discretizationLimit: discretizationLimits.entrySet()) {
					writer.append(CSVFormatter.format(discretizationLimit.getKey())+",");
					for (double limit : discretizationLimit.getValue()) {
						writer.append(CSVFormatter.format(limit)+",");
					}
					writer.append("infinity,");
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
