package com.uppaal.chip;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.csvreader.CsvWriter;

public class ModSimInputGen {

	public static void writeFile(int uInput) {
		// int uInput = 0;
		String outputFile = "users.csv";
		String simInput = null;

		// before we open the file check to see if it already exists
		boolean alreadyExists = new File(outputFile).exists();

		try {
			// use FileWriter constructor that specifies open for appending
			CsvWriter csvOutput = new CsvWriter(new FileWriter(outputFile, true), ',');

			// if the file didn't already exist then we need to write out the
			// header line
			if (!alreadyExists) {
				csvOutput.write("UPPAAL Input Type");
				csvOutput.write("Source");
				csvOutput.write("Destination");
				csvOutput.write("Body");
				csvOutput.write("Tail");
				csvOutput.endRecord();
			}
			// else assume that the file already has the correct header line

			// write out a few records
			// Generate dynamic data for model SIM input based on input
			// receiving !
			csvOutput.write(String.valueOf(uInput));
			csvOutput.write("2"); // Source
			csvOutput.write("4"); // Destination
			csvOutput.write("10101010101010101010101010101010"); // Body
			csvOutput.write("10101010101010101010101010101100"); // Tail
			csvOutput.endRecord();

			csvOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
