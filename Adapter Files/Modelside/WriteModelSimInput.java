package com.uppaal.chiporiginal;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.csvreader.CsvWriter;


public class WriteModelSimInput {
	
		public static void writeFile(int source, int destination) {
	    // int uInput = 0;
		String outputFile = "modelSim.csv";
		String simInput = null;

		// before we open the file check to see if it already exists
				boolean alreadyExists = new File(outputFile).exists();
					
				try {
					// use FileWriter constructor that specifies open for appending
					CsvWriter csvOutput = new CsvWriter(new FileWriter(outputFile, true), ',');
					
					// if the file didn't already exist then we need to write out the header line
					if (!alreadyExists)
					{
						csvOutput.write("Source");
						csvOutput.write("Destination");
//						csvOutput.write("Body");
//						csvOutput.write("Tail");
						csvOutput.endRecord();
					}
					// else assume that the file already has the correct header line
					
					// write out a few records
					csvOutput.write(String.valueOf(source));
					csvOutput.write(String.valueOf(destination));
					csvOutput.endRecord();
					
					
					csvOutput.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
	}
}
