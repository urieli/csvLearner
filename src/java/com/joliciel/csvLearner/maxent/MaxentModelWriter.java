package com.joliciel.csvLearner.maxent;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import opennlp.maxent.io.BinaryGISModelWriter;
import opennlp.maxent.io.GISModelWriter;
import opennlp.model.AbstractModel;

public class MaxentModelWriter extends GISModelWriter {
	private final GISModelWriter writer;
	private OutputStream outputStream;
	
	public MaxentModelWriter(AbstractModel model, OutputStream outputStream) {
		super(model);
		writer = new BinaryGISModelWriter(model,
	            new DataOutputStream(outputStream));
		this.outputStream = outputStream;
	}

	@Override
	public void writeUTF(String s) throws IOException {
		writer.writeUTF(s);
	}

	@Override
	public void writeInt(int i) throws IOException {
		writer.writeInt(i);
	}

	@Override
	public void writeDouble(double d) throws IOException {
		writer.writeDouble(d);
	}

	@Override
	public void close() throws IOException {
		outputStream.flush();
	}
}
