package com.uppaal.chiporiginal;

	import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

	public class ReadModelSimOutput {
	
		public String[] output;
		public ArrayList<Integer> readModelSimOutput() {
			ArrayList<Integer> list = new ArrayList<Integer>();
	        String csvFile = "readOutput_ModelSim.csv";
	        BufferedReader br = null;
	        String line = "";
	        String cvsSplitBy = ",";

	        try {

	            br = new BufferedReader(new FileReader(csvFile));
	            while ((line = br.readLine()) != null) {

	                // use comma as separator
	               output = line.split(cvsSplitBy);	              	               
	               System.out.println("source= " + output[0] + " , destination=" + output[1]);

	               list.add(Integer.valueOf(output[0]));
	               list.add(Integer.valueOf(output[1]));
	               
	                

	            }

	        } catch (FileNotFoundException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        } finally {
	            if (br != null) {
	                try {
	                    br.close();
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	            }
	        }
			return list;

	    }

	}
