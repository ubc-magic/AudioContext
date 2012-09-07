package com.magic.audiocontextawareness;

public enum CONST {
	NUMBER_OF_ENERGY_BANDS(10), 
	Fs(44100), 
	BUFFER_SIZE_MULT(8), 
	SECONDS_TO_RUN_CAT(10), //for categorization
	SECONDS_TO_RUN_ADD(5), //for adding new points
	WINDOW_SIZE(1024);
	
	public final int val;
	CONST(int val){
		this.val = val;
	}
}