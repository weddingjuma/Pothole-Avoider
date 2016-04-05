/*
 * Encog(tm) Java Examples v3.3
 * http://www.heatonresearch.com/encog/
 * https://github.com/encog/encog-java-examples
 *
 * Copyright 2008-2014 Heaton Research, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *   
 * For more information on Heaton Research copyrights, licenses 
 * and trademarks visit:
 * http://www.heatonresearch.com/copyright
 */
package recurrentneuralnetwork.elman;

import java.io.IOException;

import org.encog.Encog;
import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.CalculateScore;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.train.MLTrain;
import org.encog.ml.train.strategy.Greedy;
import org.encog.ml.train.strategy.HybridStrategy;
import org.encog.ml.train.strategy.StopTrainingStrategy;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.TrainingSetScore;
import org.encog.neural.networks.training.anneal.NeuralSimulatedAnnealing;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.neural.pattern.ElmanPattern;

/**
 * Implement an Elman style neural network with Encog. This network attempts to
 * predict detect if a sequence (peak of a signal) has passed or not. The
 * internal state stored by an Elman neural network allows better performance.
 * Elman networks are typically used for temporal neural networks. An Elman
 * network has a single context layer connected to the hidden layer.
 * 
 * @author jeff
 * 
 */
public class ElmanSequence {

	static BasicNetwork createElmanNetwork(int numInputNeurons, int numOutputNeurons) {
		// construct an Elman type network
		ElmanPattern pattern = new ElmanPattern();
		pattern.setActivationFunction(new ActivationSigmoid());
		pattern.setInputNeurons(numInputNeurons);
		pattern.addHiddenLayer(6);
		pattern.setOutputNeurons(numOutputNeurons);
		return (BasicNetwork)pattern.generate();
	}
	
	
	public static void main(final String args[]) {
		FileHandler fileHandler = new FileHandler();
		int numInputNeurons = 1;
		int numOutputNeurons = 1;

		float[][] trainData = null;
		int[] trainDataLengthVector = new int[1];
		try {
			trainData = fileHandler.readAllInputFromDirectory("potholes"+"/"+"train", trainDataLengthVector,
					numInputNeurons+numOutputNeurons);
		} catch (IOException e) {
			e.printStackTrace();
		}
		int inputDataLength = trainDataLengthVector[0];

		final Sequence trainingSequence = new Sequence(trainData, inputDataLength, 
				numInputNeurons, numOutputNeurons);
		final MLDataSet trainingSet = trainingSequence.generate(100);

		final BasicNetwork elmanNetwork = ElmanSequence.createElmanNetwork(numInputNeurons, numOutputNeurons);

		final double elmanError = ElmanSequence.trainNetwork("Elman", elmanNetwork,
				trainingSet);	

		System.out.println("Best error rate with Elman Network: " + elmanError);
		
		System.out.println("\n\n");
		
		float[][] testData = null;
		int[] testDataLengthVector = new int[1];
		try {
			testData = fileHandler.readAllInputFromDirectory("potholes"+"/"+"test", testDataLengthVector,
					numInputNeurons+numOutputNeurons);
		} catch (IOException e) {
			e.printStackTrace();
		}
		int testDataLength = testDataLengthVector[0];

		final Sequence testSequence = new Sequence(testData, testDataLength, 
				numInputNeurons, numOutputNeurons);
		final MLDataSet testingSet = testSequence.generate(1);
		
		ElmanSequence.testNetwork(elmanNetwork, testingSet);		
		
		Encog.getInstance().shutdown();
	}

	private static void testNetwork(BasicNetwork elmanNetwork,
			MLDataSet testingSet) {
		
		for(int datasetIdx = 0; datasetIdx < testingSet.size(); datasetIdx++){
			MLDataPair mldataPair = testingSet.get(datasetIdx);
			MLData output = elmanNetwork.compute(mldataPair.getInput());
			System.out.println("Input: "+mldataPair.getInput()+"  Expected: "
					+mldataPair.getIdeal()+"  Output: "+output);
		}
	}


	public static double trainNetwork(final String what,
			final BasicNetwork network, final MLDataSet trainingSet) {
		// train the neural network
		CalculateScore score = new TrainingSetScore(trainingSet);
		final MLTrain trainAlt = new NeuralSimulatedAnnealing(
				network, score, 10, 2, 100);

		final MLTrain trainMain = new Backpropagation(network, trainingSet,0.000001, 0.0);

		final StopTrainingStrategy stop = new StopTrainingStrategy();
		trainMain.addStrategy(new Greedy());
		trainMain.addStrategy(new HybridStrategy(trainAlt));
		trainMain.addStrategy(stop);

		int epoch = 0;
		while (!stop.shouldStop()) {
			trainMain.iteration();
			System.out.println("Training " + what + ", Epoch #" + epoch
					+ " Error:" + trainMain.getError());
			epoch++;
		}
		return trainMain.getError();
	}
}