///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2011 Assaf Urieli
//
//This file is part of csvLearner.
//
//csvLearner is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//csvLearner is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with csvLearner.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.csvLearner.svm;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.csvLearner.GenericEvents;
import com.joliciel.csvLearner.export.RapidMinerExampleSetLoader;
import com.joliciel.csvLearner.utils.LogUtils;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.functions.kernel.LibSVMLearner;
import com.rapidminer.operator.learner.functions.kernel.LibSVMModel;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.tools.GroupTree;
import com.rapidminer.tools.documentation.OperatorDocBundle;

/**
 * An SVM trainer using RapidMiner LibSVM
 * @author Assaf Urieli
 *
 */
public class SVMTrainer {
	private static final Log LOG = LogFactory.getLog(SVMTrainer.class);

	public LibSVMModel train(GenericEvents events) {
		RapidMinerExampleSetLoader exampleSetLoader = new RapidMinerExampleSetLoader();
		ExampleSet trainingSet = exampleSetLoader.createExampleSet(events);
		return this.train(trainingSet);
	}
	
	public LibSVMModel train(ExampleSet exampleSet) {
		Parameters params = new Parameters();
		params.setParameter(LibSVMLearner.PARAMETER_SVM_TYPE, "" + LibSVMLearner.SVM_TYPE_C_SVC);
		int kernelType = 0;
		for (String kernelName : LibSVMLearner.KERNEL_TYPES) {
			if (kernelName.equals("linear"))
				break;
			kernelType++;
		}
		params.setParameter(LibSVMLearner.PARAMETER_KERNEL_TYPE, "" + kernelType);
		params.setParameter(LibSVMLearner.PARAMETER_DEGREE, "1");
		params.setParameter(LibSVMLearner.PARAMETER_GAMMA, "0");
		params.setParameter(LibSVMLearner.PARAMETER_COEF0, "0");
		params.setParameter(LibSVMLearner.PARAMETER_NU, "0");
		params.setParameter(LibSVMLearner.PARAMETER_CACHE_SIZE, "10");
		params.setParameter(LibSVMLearner.PARAMETER_C, "0");
		params.setParameter(LibSVMLearner.PARAMETER_EPSILON, "1");
		params.setParameter(LibSVMLearner.PARAMETER_P, "0");
		params.setParameter(LibSVMLearner.PARAMETER_SHRINKING, "false");
		params.setParameter(LibSVMLearner.PARAMETER_CALCULATE_CONFIDENCES, "false");

		OperatorDocBundle bundle = OperatorDocBundle.load(this.getClass().getClassLoader(), "operatorDoc");
		GroupTree groupTree = GroupTree.findGroup("JMySVMLearner", bundle);

		OperatorDescription description = new OperatorDescription("JMySVMLearner", LibSVMLearner.class, groupTree,this.getClass().getClassLoader(), "JMySVMLeanerIcon", null);
		LibSVMLearner learner = new LibSVMLearner(description);
		learner.setParameters(params);
		LibSVMModel model;
		try {
			model = (LibSVMModel) learner.learn(exampleSet);
		} catch (OperatorException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
		return model;
	}
}
