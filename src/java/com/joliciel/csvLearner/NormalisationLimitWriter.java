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
import java.util.Map;
import java.util.Map.Entry;
import com.joliciel.csvLearner.utils.CSVFormatter;

public class NormalisationLimitWriter {
	private File file;
	private Writer writer;
	
	public NormalisationLimitWriter(File file) {
		this.file = file;
	}

	public NormalisationLimitWriter(Writer writer) {
		this.writer = writer;
	}
	
	public void writeFile(Map<String,Float> normalisationLimits) {
		try {
			if (this.file!=null) {
				file.delete();
				file.createNewFile();
				this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false),"UTF8"));
			}

			try {
				writer.append("feature,max,");
				
				writer.append("\n");
				writer.flush();
				
				for (Entry<String,Float> normalisationLimit: normalisationLimits.entrySet()) {
					writer.append(CSVFormatter.format(normalisationLimit.getKey())+",");
					writer.append(CSVFormatter.format(normalisationLimit.getValue())+",");
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
