package com.joliciel.csvLearner.svm;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.joliciel.csvLearner.GenericEvent;
import com.joliciel.csvLearner.GenericEvents;
import com.rapidminer.operator.learner.functions.kernel.LibSVMModel;

public class SVMTrainerTest {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(SVMTrainerTest.class);

	@Test
	public void testTrain() throws Exception {
		GenericEvent event1 = new GenericEvent("a1");
		event1.addFeature("A", 1);
		event1.addFeature("B", 0.5f);
		event1.setOutcome("OutcomeA");
		event1.setTest(false);
		GenericEvent event2 = new GenericEvent("a2");
		event2.addFeature("A", 0.3f);
		event2.addFeature("B", 0.8f);
		event2.setOutcome("OutcomeB");
		event2.setTest(false);

		GenericEvent event3 = new GenericEvent("a3");
		event3.addFeature("A", 1);
		event3.addFeature("B", 1);
		event3.setOutcome("OutcomeA");
		event3.setTest(true);
		GenericEvent event4 = new GenericEvent("a4");
		event4.addFeature("A", 0.5f);
		event4.addFeature("B", 0.5f);
		event4.setOutcome("OutcomeC");
		event4.setTest(true);
		
		Collection<GenericEvent> eventList = new ArrayList<GenericEvent>();
		eventList.add(event1);
		eventList.add(event2);
		eventList.add(event3);
		eventList.add(event4);
		GenericEvents events = new GenericEvents(eventList);
		
		SVMTrainer trainer = new SVMTrainer();
		LibSVMModel model = trainer.train(events);
		assertNotNull(model);
		
		SVMPredicter predicter = new SVMPredicter(model);
		predicter.predict(events);

	}

}
