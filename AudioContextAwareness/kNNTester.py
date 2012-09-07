import numpy as np
import wave
import math
import struct
import glob
import sys
import os
from time import time

ENV = ['LAB', 'CAR', 'CARWITHMUSIC', 'STREET', 'BUS', 'UNSURE']

BAND_LIMITS = {0:65,
               1:130,
               2:260,
               3:500,
               4:1000,
               5:2000,
               6:4000,
               7:8000,
               8:16000}

WINDOW_SIZE = 1024

def add_fft_to_bands(fft_out, band_indexes, band_energy):
    total_energy = 0
    temp_band_energy = [0]*10
    for i in range(0,len(fft_out)):
        total_energy += fft_out[i]
        for j in range(0,10):
            if i < band_indexes[j]:
                temp_band_energy[j] += fft_out[i]
                break
    for i in range(0,len(band_energy)):
        band_energy[i] += temp_band_energy[i] / total_energy;
        
def normalize_bands(band_energy, windows_per_s):
    for i in range(0,10):
        band_energy[i] /= windows_per_s;
        
def get_band_indexes(fs):
    bin_size = fs/WINDOW_SIZE;
    band_indexes = {x:int((BAND_LIMITS[x]/bin_size)+1) for x in range(0, 9)} #10 bands
    band_indexes[9] = int(WINDOW_SIZE/2+2) #the last band includes the highest freq, WINDOW_SIZE/2+1
    return band_indexes
        
def get_fft(window):
    fft = np.fft.fft(window)
    fft_out = [abs(fft[0])/len(window)]
    for i in range(1,int(len(window)/2)+1):
        fft_out.append(abs(fft[i])*2/len(window))
    fft_out.append(abs(fft[len(window)/2+1])/len(window))
    return fft_out
    
    
def read_window(window, wave_file):
    temp = wave_file.readframes(1024)
    for i in range(0, WINDOW_SIZE):
        window[i] = struct.unpack('<h', temp[i*2:i*2+2])[0]/32768 #normalize audio samples to 1
    
if __name__ == '__main__':    
    window = [0]*1024
        
    files = glob.glob('C:\\Users\\mgrahamj\\Desktop\\Dropbox\\Summer 2012\\AudioContextAwareness\\testing clips' + '\\*.wav')
    for file in files:
        with open('C:\\Users\\mgrahamj\\Desktop\\Dropbox\\Summer 2012\\AudioContextAwareness\\testing clips\\' + os.path.splitext(os.path.basename(file))[0] + ".txt", 'w') as f_out:
            wave_file = wave.open(file, 'r')
            fs = wave_file.getframerate()
            windows_per_s = int(fs/WINDOW_SIZE)
            number_of_frames = wave_file.getnframes()
            total_s = int(number_of_frames/fs)
            if total_s > 600: total_s = 600 #limit to 10min per audio sample
            print(os.path.basename(file))
            f_out.write(os.path.basename(file)[0:2] + "\n")
            
            
            band_indexes = get_band_indexes(fs)
            for i in range(0, total_s):
                band_energy = [0]*10
                for j in range(0, windows_per_s):
                    read_window(window, wave_file)
                    fft_out = get_fft(window)
                    add_fft_to_bands(fft_out, band_indexes, band_energy)
                normalize_bands(band_energy, windows_per_s)
                out_string = str(band_energy[0])
                for j in range(1,10):
                    out_string += ", " + str(band_energy[j])
                f_out.write(out_string + "\n")
            wave_file.close()
    print("done")



















