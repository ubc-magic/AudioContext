package com.magic.audiocontextawareness;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import com.magic.audiocontextawareness.CONTEXT;
public class NearestNeighbourCalculator{
	CompPoint[] compPoints;
	
	public NearestNeighbourCalculator(Context context){
		try{
			File root = context.getFilesDir();
			File valsFile = new File(root, "vals.txt");
			File typesFile = new File(root, "types.txt");
			File versionFile = new File(root, "version.txt");
			File numFile = new File(root, "num.txt");
			updateCompPointDB(context, valsFile, typesFile, versionFile);
			BufferedReader numBR = new BufferedReader(new InputStreamReader(new FileInputStream(numFile)));
			BufferedReader typesBR = new BufferedReader(new InputStreamReader(new FileInputStream(typesFile)));
			DataInputStream dis = new DataInputStream(new FileInputStream(valsFile));
			double[] pointCoords = new double[CONST.NUMBER_OF_ENERGY_BANDS.val];
			CONTEXT pointContext = CONTEXT.UNSURE;
			String contextString;
			int numPoints = Integer.parseInt(numBR.readLine());
			compPoints = new CompPoint[numPoints];
			
			for(int i = 0; i < numPoints; ++i){
				for(int j = 0; j < CONST.NUMBER_OF_ENERGY_BANDS.val; ++j){
					pointCoords[j] = dis.readDouble();
				}
				contextString = typesBR.readLine();
				try{
					pointContext = CONTEXT.valueOf(contextString);
				} catch(Exception e){
					pointContext = CONTEXT.UNSURE;
				}
				
				compPoints[i] = new CompPoint(pointContext, pointCoords);
				
			}
		} catch (Exception e) { //if the comparison points don't initialize correctly, there's really nothing that can be done
			Log.e("NearestNeighbourCalculator initialization", e.getMessage());
		}
	}
	
	public CONTEXT getNearestContextType(double[] compPoint){
		double dist = 0;
		double minDist = compPoints[0].getManhattanDist(compPoint);

		CONTEXT minContextType = compPoints[0].contextType;
		for(int i = 1; i < compPoints.length; ++i){
			dist = compPoints[i].getManhattanDist(compPoint);
			if(dist < minDist){
				minDist = dist;
				minContextType = compPoints[i].contextType;
			}
		}

		return minContextType;
	}
	
	private void updateCompPointDB(Context context, File valsFile, File typesFile, File versionFile){
		boolean copyFiles = false;
		
		if(valsFile.exists() || typesFile.exists()){
			try{
				InputStream is = context.openFileInput("version.txt");
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader versionBR = new BufferedReader(isr);
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
			copyCompPointsDB(context, valsFile, typesFile, versionFile);
		}
			
	}
	
	private void copyCompPointsDB(Context context, File valsFile, File typesFile, File versionFile){
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
}

class CompPoint{
	CONTEXT contextType;
	double[] coords;
	
	public CompPoint(CONTEXT contextType, double[] coords){
		this.contextType = contextType;
		this.coords = coords;
		Log.d("CompPoint initialization", "Created " + contextType.name());
	}
	
	public double getManhattanDist(double[] compPoint){
		double dist = 0;
		for(int i = 0; i < coords.length; ++i){
			dist += Math.abs(coords[i] - compPoint[i]);
		}
		return dist;
	}
	
}