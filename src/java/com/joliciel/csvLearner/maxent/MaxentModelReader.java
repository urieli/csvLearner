package com.joliciel.csvLearner.maxent;

import java.io.IOException;
import java.io.InputStream;

import opennlp.maxent.io.GISModelReader;
import opennlp.model.AbstractModel;
import opennlp.model.AbstractModelReader;
import opennlp.model.BinaryFileDataReader;

public class MaxentModelReader extends AbstractModelReader {
	AbstractModelReader reader = null;
	
	public MaxentModelReader(InputStream inputStream) {
		super(new BinaryFileDataReader(inputStream));
		reader = new GISModelReader(super.dataReader);
	}

	@Override
	public void checkModelType() throws IOException {
		reader.checkModelType();
	}

	@Override
	public AbstractModel constructModel() throws IOException {
		return reader.constructModel();
	}

}
