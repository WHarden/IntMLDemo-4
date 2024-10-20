import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import processing.core.PApplet;
import processing.sound.AudioIn;
import processing.sound.FFT;
import processing.sound.Sound;
import processing.sound.Waveform;

/* A class with the main function and Processing visualizations to run the demo */

public class ClassifyVibration extends PApplet {

	FFT fft;
	AudioIn in;
	Waveform waveform;
	int bands = 512;
	int nsamples = 1024; 
	float[] spectrum = new float[bands];
	float[] fftFeatures = new float[bands];
	String[] classNames = {"neutral", "drill", "hammer"};
	int classIndex = 0;
	int dataCount = 0;
	boolean recording = false;
	String message = "Press space to start recording";
	Set<String> classifySet = new HashSet<>();
	Map<String, Integer> dataCollectionMap = new HashMap<>();
	{{for (String className : classNames){
		dataCollectionMap.put(className, 0);
	}}}

	MLClassifier classifier;
	
	Map<String, List<DataInstance>> trainingData = new HashMap<>();
	{for (String className : classNames){
		trainingData.put(className, new ArrayList<DataInstance>());
	}}
	
	DataInstance captureInstance (String label){
		DataInstance res = new DataInstance();
		res.label = label;
		res.measurements = fftFeatures.clone();
		return res;
	}
	
	public static void main(String[] args) {
		PApplet.main("ClassifyVibration");
	}
	
	public void settings() {
		size(512, 400);
	}

	public void setup() {
		
		/* list all audio devices */
		Sound.list();
		Sound s = new Sound(this);
		  
		/* select microphone device */
		s.inputDevice(8);
		    
		/* create an Input stream which is routed into the FFT analyzer */
		fft = new FFT(this, bands);
		in = new AudioIn(this, 0);
		waveform = new Waveform(this, nsamples);
		waveform.input(in);
		
		/* start the Audio Input */
		in.start();
		  
		/* patch the AudioIn */
		fft.input(in);
	}

	public void draw() {
		background(0);
		fill(0);
		stroke(255);
		
		waveform.analyze();

		beginShape();
		  
		for(int i = 0; i < nsamples; i++)
		{
			vertex(
					map(i, 0, nsamples, 0, width),
					map(waveform.data[i], -1, 1, 0, height)
					);
		}
		
		endShape();

		fft.analyze(spectrum);

		for(int i = 0; i < bands; i++){

			/* the result of the FFT is normalized */
			/* draw the line for frequency band i scaling it up by 40 to get more amplitude */
			line( i, height, i, height - spectrum[i]*height*40);
			fftFeatures[i] = spectrum[i];
		} 

		fill(255);
		textSize(30);
		
		
		if(classifier != null) {
			text(message, 20, 30);
			if (recording) {
				// classifySet.clear();
				message = "Recording in progress";
				String guessedLabel = classifier.classify(captureInstance(null));
				classifySet.add(guessedLabel);
				int count = dataCollectionMap.get(guessedLabel);
				dataCollectionMap.put(guessedLabel, count += 1);
			} else {
				message = "Press space to start recording";
				// text("Classified as: " + classifySet, 20, 60);
			}
			// Step 1: Sort the HashMap by values in descending order
			List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(dataCollectionMap.entrySet());
			sortedEntries.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
	
			// Step 2: Retrieve the top two values
			List<Map.Entry<String, Integer>> topTwoEntries = sortedEntries.subList(0, Math.min(2, sortedEntries.size()));

			// if(topTwoEntries.get(0).getKey()== classNames[0] || topTwoEntries.get(1).getKey()== classNames[0]){
			// 	text()
			// }
			
			// Print the top two entries
			// for (Map.Entry<String, Integer> entry : topTwoEntries) {
			// 	System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
			// }
			text("Classified as: " + topTwoEntries, 20, 60);
			// Yang: add code to stabilize your classification results
			
			
		} else {
			text(classNames[classIndex], 20, 30);
			dataCount = trainingData.get(classNames[classIndex]).size();
			text("Data collected: " + dataCount, 20, 60);
		}
		
		
	}
	
	@SuppressWarnings (value="unchecked")
	public void keyPressed() {
		

		if (key == CODED && keyCode == DOWN) {
			classIndex = (classIndex + 1) % classNames.length;
		}
		
		else if (key == 't') {
			if(classifier == null) {
				println("Start training ...");
				int dCount = 0;
				for (List<DataInstance> d : trainingData.values()) {
					dCount += d.size();
				}
				if (dCount > 10) {
					classifier = new MLClassifier();
					classifier.train(trainingData);
				} else {
					System.out.println("There is not enough data for training");
				}
			}else {
				classifier = null;
			}
		}
		
		else if (key == 's') {
			// save classifier into a file csv, arff file, etc
			// Yang: add code to save your trained model for later use
			try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("trainingData5.ser"))) {
            	outputStream.writeObject(trainingData);
				System.out.println("Data has been saved to trainingData5.ser");
        	} catch (IOException e) {
            	e.printStackTrace();
        	}
		}
		
		else if (key == 'l') {
			// Yang: add code to load your previously trained model
			try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("trainingData5.ser"))) {
            	trainingData = (Map<String, List<DataInstance>>) inputStream.readObject();
            	System.out.println("Data has been loaded from trainingData5.ser");
            
        	} catch (IOException | ClassNotFoundException e) {
            	e.printStackTrace();
        	}
		} 

		else if (key == ' ') {
			if (classifier != null) {
				recording = !recording;
				if (recording) {
					classifySet.clear();
					
					for (String className : classNames){
						dataCollectionMap.put(className, 0);
					}

				} 
			}
			
		}
			
		else {
			if (classifier == null) {
				DataInstance currentCapturedInstance = captureInstance(classNames[classIndex]);
				// System.out.println(currentCapturedInstance.measurements.length);
				trainingData.get(classNames[classIndex]).add(currentCapturedInstance);
			}
		}
	}

}
