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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.csvLearner.CSVEventListReader;
import com.joliciel.csvLearner.GenericEvents;
import com.joliciel.csvLearner.utils.LogUtils;

import opennlp.maxent.GISTrainer;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;
import opennlp.model.AbstractModel;
import opennlp.model.DataIndexer;
import opennlp.model.MaxentModel;
import opennlp.model.OnePassRealValueDataIndexer;

/**
 * Given a set of events, trains a maxent model using the GIS algorithm.
 * @author Assaf Urieli
 *
 */
public class MaxentTrainer {
    private static final Log LOG = LogFactory.getLog(CSVEventListReader.class);
	private int iterations = 100;
	private int cutoff = 5;
	private double sigma = 0;
	private double smoothing = 0;
	private OutputStream outputStream = null;
	private File outputFile = null;

	public MaxentModel train(GenericEvents events) {
		try {
			GenericEventMapEventStream eventStream = new GenericEventMapEventStream(events);
			// Note: two-pass indexer does not maintain values
			//DataIndexer dataIndexer = new TwoPassDataIndexer(eventStream, cutoff);
			DataIndexer dataIndexer = new OnePassRealValueDataIndexer(eventStream, cutoff);
			// AbstractModel model = GIS.trainModel(iterations, dataIndexer);
			GISTrainer trainer = new GISTrainer(true);
			if (sigma>0)
				trainer.setGaussianSigma(sigma);
			
			if (smoothing>0) {
				trainer.setSmoothing(true);
				trainer.setSmoothingObservation(smoothing);
			}
			
			AbstractModel model =  trainer.trainModel(iterations, dataIndexer, cutoff);
	
			if (outputFile!=null) {
				new SuffixSensitiveGISModelWriter(model, outputFile).persist();
			} else if (outputStream!=null) {
				new MaxentModelWriter(model, outputStream).persist();
			}
			
			return model;
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

	
	/**
	 * An output file where the model should be written.
	 * @return
	 */
	public File getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	/**
	 * An outputStream for writing the binary model.
	 * Useful when writing directly into a zip file.
	 * @return
	 */
	public OutputStream getOutputStream() {
		return outputStream;
	}

	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	/**
	 * The number of iterations for Maxent training.
	 * @return
	 */
	public int getIterations() {
		return iterations;
	}


	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	/**
	 * Cutoff for maxent training - features must appear at least this many times to be included in the model.
	 * Note that for numeric features, any value > 0 counts as 1 time for cutoff purposes.
	 * @return
	 */
	public int getCutoff() {
		return cutoff;
	}

	public void setCutoff(int cutoff) {
		this.cutoff = cutoff;
	}

	/**
	 * Sigma for Gaussian smoothing on maxent training.
	 * @return
	 */
	public double getSigma() {
		return sigma;
	}

	public void setSigma(double sigma) {
		this.sigma = sigma;
	}

	/**
	 * Additive smoothing parameter during maxent training.
	 * @return
	 */
	public double getSmoothing() {
		return smoothing;
	}

	public void setSmoothing(double smoothing) {
		this.smoothing = smoothing;
	}

}
