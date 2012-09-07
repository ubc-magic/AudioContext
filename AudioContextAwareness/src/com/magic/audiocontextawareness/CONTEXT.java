package com.magic.audiocontextawareness;

public enum CONTEXT {BUS(0), CAR(1), LAB(2), LABWITHFAN(3), INPOCKET(4), STREET(5), UNSURE(6);
		private final int index;
		
		CONTEXT(int index){
			this.index = index;
		}
		
		public int getIndex() { return index;}
		
		public static CONTEXT fromIndex(int in){
			for(CONTEXT context : values()){
				if(in == context.getIndex()){
					return context;
				}
			}
			return UNSURE; 
		}
		
		public static int getNumber(){ return 7;} //MANUALLY COUNTED. IGNORES UNSURE. KEEP UPDATED
	}; 