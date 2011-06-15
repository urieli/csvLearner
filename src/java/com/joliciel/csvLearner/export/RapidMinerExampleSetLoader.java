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
package com.joliciel.csvLearner.export;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.joliciel.csvLearner.GenericEvent;
import com.joliciel.csvLearner.GenericEvents;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.tools.Ontology;
		
public class RapidMinerExampleSetLoader {

	public ExampleSet createExampleSet(GenericEvents events) {
		return this.createExampleSet(events, false, null);
	}
	
	public ExampleSet createExampleSet(GenericEvents events, boolean forPrediction, Set<String> outcomes) {
		// create attribute list
		List <Attribute> attributes = new LinkedList<Attribute>();
		for (String feature : events.getFeatures()) {
			attributes.add( AttributeFactory.createAttribute (feature,Ontology.REAL));
		}
		
		if (forPrediction) {
			Attribute predictedLabel = AttributeFactory.createAttribute("predictedLabel", Ontology.NOMINAL);
			attributes.add(predictedLabel);
			for (String outcome : outcomes) {
				predictedLabel.getMapping().mapString(outcome);
			}			
	
			for (String outcome : outcomes) {
				String name = "confidence_" + outcome;
				Attribute confidenceAttribute = AttributeFactory.createAttribute(name, Ontology.REAL);
				attributes.add(confidenceAttribute);
			}	

		}

		Attribute label = AttributeFactory.createAttribute ("label", Ontology.NOMINAL);
		attributes.add(label);
		
		if (forPrediction) {
			for (String outcome : outcomes) {
				label.getMapping().mapString(outcome);
			}			
		}

		//create table
		MemoryExampleTable table = new MemoryExampleTable(attributes);
		// fill table (here : only real values )

		for (GenericEvent event : events) {
			if ((!event.isTest()&&!forPrediction)||(event.isTest()&&forPrediction)) {
				double[] data = new double[attributes.size()];
				int a = 0;
				for (String feature : events.getFeatures()) {
					int featureIndex = event.getFeatureIndex(feature);
					//fill with proper data here
					if (featureIndex < 0)
						data[a] = 0;
					else
						data[a] = event.getWeights().get(featureIndex);
	
					a++;
				}
				// maps the nominal classification to a double value
				data[data.length - 1] = label.getMapping().mapString(event.getOutcome());
				//add data row
				table.addDataRow(new DoubleArrayDataRow(data));
			}
		}
		// create example set
		ExampleSet exampleSet = table.createExampleSet(label);
		if (forPrediction) {
			Attribute predictedLabel = exampleSet.getAttributes().get("predictedLabel");
			exampleSet.getAttributes().setPredictedLabel(predictedLabel);
			for (String outcome : outcomes) {
				String name = "confidence_" + outcome;
				Attribute confidenceAttribute = exampleSet.getAttributes().get(name);
				exampleSet.getAttributes().setSpecialAttribute(confidenceAttribute, name);
			}
		}
		return exampleSet;
	}

}