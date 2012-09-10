package com.magic.audiocontextawareness;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.audio.analysis.FFT;

import com.magic.audiocontextawareness.CONTEXT;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class AudioContextAwarenessActivity extends Activity {

	
	  
	
	public static final int PREDICTION_MESSAGE = 0;
	public static final int PROG_MESSAGE = 1;
	public static final int CANCEL_MESSAGE = 2;
	

	private static final int[] BAND_INDEXES = {2, 4, 7, 12, 24, 47, 93, 186, 372, 514};
	
	ProgressBar progressBar = null;
	Button categorizeAudioButton = null;
	TextView resultsTextView = null;
	Spinner contextSpinner = null;
	Button addContextButton = null;
	
	String outText = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_context_awareness);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        categorizeAudioButton = (Button)findViewById(R.id.categorizeAudioButton);
        resultsTextView = (TextView)findViewById(R.id.resultsTextView);
        contextSpinner = (Spinner)findViewById(R.id.contextSpinner);
        addContextButton = (Button)findViewById(R.id.addContextButton);
        
        //create spinner with the CONTEXT enum values
        List<String> contextList = new ArrayList<String>();
        for(CONTEXT c : CONTEXT.values()){
        	if(!c.equals(CONTEXT.UNSURE)){
    			contextList.add(c.toString());
        	}
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, contextList);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        contextSpinner.setAdapter(dataAdapter);
        updateCompPointDB(this);
    }

    private void updateCompPointDB(Context context){
    	File root = new File(Environment.getExternalStorageDirectory(), "\\aca");//context.getFilesDir();
		File valsFile = new File(root, "vals.txt");
		File typesFile = new File(root, "types.txt");
		File versionFile = new File(root, "version.txt");
		File numFile = new File(root, "num.txt");
		boolean copyFiles = false;
		
		if(valsFile.exists() && typesFile.exists() && valsFile.exists()){
			try{
				BufferedReader versionBR = new BufferedReader(new InputStreamReader(context.openFileInput("version.txt")));
				int internalVersion = Integer.parseInt(versionBR.readLine());
				int apkVersion = context.getResources().getInteger(R.string.compPointVersion);
				if(apkVersion > internalVersion){
					copyFiles = true;
				}
			} catch(Exception e){
				copyFiles = true;
			}
		} else{
			copyFiles = true;
		}
		
		if(copyFiles){
			copyCompPointsDB(context, valsFile, typesFile, versionFile, numFile);
		}
			
	}
	
	private void copyCompPointsDB(Context context, File valsFile, File typesFile, File versionFile, File numFile){
		AssetManager am = context.getAssets();
		
		try{
			//write vals
			InputStream in = am.open("vals.txt");
			OutputStream out = new FileOutputStream(valsFile);
			copyFile(in, out);
			in.close();
			out.close();
			
			//write types
			in = am.open("types.txt");
			out = new FileOutputStream(typesFile);
			copyFile(in, out);
			in.close();
			out.close();
			
			//write number
			in = am.open("num.txt");
			out = new FileOutputStream(numFile);
			copyFile(in, out);
			in.close();
			out.close();
			
			//write version
			BufferedWriter bout = new BufferedWriter(new FileWriter(versionFile, false));
			bout.write(Integer.toString(context.getResources().getInteger(R.string.compPointVersion)));
			bout.close();
			
		} catch(Exception e){
			Log.e("UpdateCompPointDB", e.getMessage());
		}
	}
	
	private void copyFile(InputStream in, OutputStream out) throws IOException{
		byte[] buffer = new byte[1024];
		int read;
		while((read = in.read(buffer)) != -1){
			out.write(buffer, 0, read);
		}
	}
	
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_audio_context_awareness, menu);
        return true;
    }

    public void categorizeAudioButtonListener(View v){
    	disableButtons(); //only one thread running at once
    	//do audio analysis
    	AudioContextAsyncTask aCAsyncTask = new AudioContextAsyncTask(this);
    	aCAsyncTask.execute();
    }
    
    public void addNewLocationContextListener(View v){
    	disableButtons();
    	AddLocationPointAsyncTask aLPAsyncTask = new AddLocationPointAsyncTask(this, CONTEXT.valueOf((String)contextSpinner.getSelectedItem()));
    	aLPAsyncTask.execute();
    }
    
    private void disableButtons(){
    	categorizeAudioButton.setClickable(false);
    	addContextButton.setClickable(false);
    }
    
    private void enableButtons(){
    	categorizeAudioButton.setClickable(true);
    	addContextButton.setClickable(true);
    }
    
    public static void addEnergyBands(short[] data, double[] energyBands, double[] tempEnergyBands, float[] fftIn, float[] fftOut, FFT fft){
		//do FFT
		for(int i = 0; i < data.length; ++i){ //convert to floats
			fftIn[i] = (float)data[i];
		}
		fft.forward(fftIn); //do FFT
		fftOut = fft.getSpectrum(); //get FFT result
		int currentBand = 0;
		for(int i = 0; i < fftOut.length; ++i){ //add FFT results to the correct energy band
			if(i >= BAND_INDEXES[currentBand]){
				++currentBand;
			}
			tempEnergyBands[currentBand] += fftOut[i];
		}
		double totalEnergy = 0;
		for(int i = 0; i < energyBands.length; ++i){
			totalEnergy += tempEnergyBands[i];
		}
		for(int i = 0; i < energyBands.length; ++i){
			energyBands[i] += tempEnergyBands[i] / totalEnergy;
		}
		
	}
    
    public static int read1sAudio(double[] energyBands, int windowsPerSecond, int secRunning, AudioRecord recorder, short[] data, double[] tempEnergyBands, float[] fftIn, float[] fftOut, FFT fft){
    	for(int i = 0; i < energyBands.length; ++i){ //clear energy band array, which is summed over following the second
			energyBands[i] = 0;
		}
		for(int i = 0; i < windowsPerSecond; ++i){
			recorder.read(data, 0, CONST.WINDOW_SIZE.val); //read window
    		addEnergyBands(data, energyBands, tempEnergyBands, fftIn, fftOut, fft);
		}
		for(int i = 0; i < energyBands.length; ++i){ //normalize result
			energyBands[i] /= windowsPerSecond;
		}
		return secRunning + 1;
    }
    
    private class AddLocationPointAsyncTask extends AsyncTask<Void, Integer, double[]>{
    	AudioRecord recorder;
    	int windowsPerSecond;
    	FFT fft = new FFT(CONST.WINDOW_SIZE.val, CONST.Fs.val);
    	Context context;
    	CONTEXT locContext;
    	
    	AddLocationPointAsyncTask(Context context, CONTEXT locContext){
    		this.context = context;
    		this.locContext = locContext;
    	}
    	
    	@Override
		protected double[] doInBackground(Void... arg0){
    		Log.v("AudioContextAsyncTask", "started");
    		
    		windowsPerSecond = CONST.Fs.val/CONST.WINDOW_SIZE.val;
    		progressBar.setMax(CONST.SECONDS_TO_RUN_ADD.val*2);
    		recorder = new AudioRecord(AudioSource.MIC, CONST.Fs.val, AudioFormat.CHANNEL_CONFIGURATION_MONO, 
					AudioFormat.ENCODING_PCM_16BIT, CONST.WINDOW_SIZE.val*CONST.BUFFER_SIZE_MULT.val);
    		
    		short data[] = new short[CONST.WINDOW_SIZE.val];
    		double[][] energyBands = new double[CONST.SECONDS_TO_RUN_ADD.val][CONST.NUMBER_OF_ENERGY_BANDS.val];
    		double[] tempEnergyBands = new double[CONST.NUMBER_OF_ENERGY_BANDS.val];
    		float[] fftIn = new float[CONST.WINDOW_SIZE.val];
    		float[] fftOut = null;
    		int secNumber = 0;
    		recorder.startRecording();
    		for(int second = 0; second < CONST.SECONDS_TO_RUN_ADD.val; ++second){
    			secNumber = read1sAudio(energyBands[second], windowsPerSecond, secNumber, recorder, data, tempEnergyBands, fftIn, fftOut, fft);
    			publishProgress(secNumber);
    		}
    		recorder.stop();
    		//analysis turning the collected data into 1 representative point
    		for(int i = 0; i < CONST.SECONDS_TO_RUN_ADD.val/2; ++i){
    			removeFurthestPoint(energyBands);
    		}
    		
    		
    		
    		return calcAverage(energyBands);
    	}
    	
    	private void removeFurthestPoint(double[][] energyBands){
    		double[] average = calcAverage(energyBands);
    		int furthestIndex = -1;
    		double furthestDist = -1;
    		double tempDist;
    		
    		for(int i = 0; i < energyBands.length; ++i){
    			if(energyBands[i] != null){ //if null, this point has already been removed
					tempDist = getCityBlockDist(average, energyBands[i]);
					if(tempDist > furthestDist){
						furthestDist = tempDist;
						furthestIndex = i;
					}
    			}
    		}
    		energyBands[furthestIndex] = null;
    	}
    	
    	private double[] calcAverage(double[][] energyBands){
    		double[] average = new double[10];
    		int numPoints = 0;
    		
    		for(int i = 0; i < energyBands.length; ++i){
    			if(energyBands[i] != null){
	    			for(int j = 0; j < 10; ++j){
	    				average[j] += energyBands[i][j];
	    			}
	    			++numPoints;
    			}
    		}
    		for(int i = 0; i < 10; ++i){
    			average[i] /= numPoints;
    		}
    		return average;
    	}
    	
    	private double getCityBlockDist(double[] p1, double[] p2){
    		double dist = 0;
    		for(int i = 0; i < p1.length; ++i){
    			dist += Math.abs(p1[i]-p2[i]);
    		}
    		return dist;
    	}
    	
    	@Override
    	protected void onProgressUpdate(Integer... progress){
    		progressBar.setProgress(progress[0]);
    	}
    	
    	@Override
		protected void onPostExecute(double[] result) {
    		Log.v("AudioContextAsyncTask", "completed");
    		progressBar.setProgress(progressBar.getMax());
    		
    		//write new point
    		File root = new File(Environment.getExternalStorageDirectory(), "aca"); //context.getFilesDir();
			File valsFile = new File(root, "vals.txt");
			File typesFile = new File(root, "types.txt");
			File numFile = new File(root, "num.txt");
			try{
				BufferedWriter typesOut = new BufferedWriter(new FileWriter(typesFile, true));
				DataOutputStream valsOut = new DataOutputStream(new FileOutputStream(valsFile, true));
				BufferedReader numBR = new BufferedReader(new InputStreamReader(new FileInputStream(numFile)));
				int numPoints = Integer.parseInt(numBR.readLine()) + 1;
				numBR.close();
				BufferedWriter numOut = new BufferedWriter(new FileWriter(numFile, false));
				
				typesOut.write(locContext.toString());
				numOut.write(Integer.toString(numPoints));
				for(int i = 0; i < 10; ++i){
					valsOut.writeDouble(result[i]);
				}

				valsOut.close();
				typesOut.close();
				numOut.close();
				
				Toast.makeText(context, "Added new point. " + Integer.toString(numPoints) + " total.", Toast.LENGTH_SHORT).show();
			} catch(Exception e){
				e.printStackTrace();
			}
    		
    		enableButtons();
		}

    	
    }
    
    private class AudioContextAsyncTask extends AsyncTask<Void, Integer, CONTEXT>{
    	AudioRecord recorder;
    	int windowsPerSecond;
    	FFT fft = new FFT(CONST.WINDOW_SIZE.val, CONST.Fs.val);
    	NearestNeighbourCalculator nnc;
    	
    	AudioContextAsyncTask(Context context){
    		 nnc = new NearestNeighbourCalculator(context);
    	}
    	
    	@Override
    	protected CONTEXT doInBackground(Void... arg0) {
    		Log.v("AudioContextAsyncTask", "started");
    		windowsPerSecond = CONST.Fs.val/CONST.WINDOW_SIZE.val;
    		progressBar.setMax(CONST.SECONDS_TO_RUN_CAT.val);
    		recorder = new AudioRecord(AudioSource.MIC, CONST.Fs.val, AudioFormat.CHANNEL_CONFIGURATION_MONO, 
					AudioFormat.ENCODING_PCM_16BIT, CONST.WINDOW_SIZE.val*CONST.BUFFER_SIZE_MULT.val);
    		
    		short data[] = new short[CONST.WINDOW_SIZE.val];
    		double[] energyBands = new double[CONST.NUMBER_OF_ENERGY_BANDS.val];
    		double[] tempEnergyBands = new double[CONST.NUMBER_OF_ENERGY_BANDS.val];
    		CONTEXT[] predictions = new CONTEXT[CONST.SECONDS_TO_RUN_CAT.val];
    		float[] fftIn = new float[CONST.WINDOW_SIZE.val];
    		float[] fftOut = null;
    		int secRunning = 0;
    		recorder.startRecording();
    		for(int second = 0; second < CONST.SECONDS_TO_RUN_CAT.val; ++second){
    			secRunning = read1sAudio(energyBands, windowsPerSecond, secRunning, recorder, data, tempEnergyBands, fftIn, fftOut, fft);
    			publishProgress(secRunning);
	    		predictions[second] = getCONTEXTType(energyBands);
    		}
    		
    		CONTEXT prediction = makePrediction(predictions);
    		
    		recorder.release();
    		return prediction;
    	}
    	
    	
    	
    	private CONTEXT getCONTEXTType(double[] energyBands){
    		CONTEXT prediction = nnc.getNearestContextType(energyBands);
    		Log.v("getCONTEXTType", "Predicted " + prediction.toString());
    		return prediction;
    	}
    	
    	private CONTEXT makePrediction(CONTEXT[] predictions){
    		int[] resultCounter = new int[CONTEXT.getNumber()];
    		for(CONTEXT context : predictions) ++resultCounter[context.getIndex()];
    		//find index with most predictions
    		int index = 0;
    		int max = resultCounter[0];
    		for(int i = 1; i < resultCounter.length; ++i){
    			if(resultCounter[i] > max){
    				max = resultCounter[i];
    				index = i;
    			}
    		}
    		if(max > (predictions.length/2)){
    			return CONTEXT.fromIndex(index);
    		} else return CONTEXT.UNSURE;
    	}
    	
    	@Override
    	protected void onProgressUpdate(Integer... progress){
    		progressBar.setProgress(progress[0]);
    	}
    	
    	@Override
    	protected void onPostExecute(CONTEXT prediction){
    		Log.v("AudioContextAsyncTask", "completed");
    		progressBar.setProgress(progressBar.getMax());
    		if(outText == null)
				outText = prediction.toString();
			else
				outText = outText + "\n" + prediction.toString();
			resultsTextView.setText(outText);
			enableButtons();
    	}
    	
    }
}