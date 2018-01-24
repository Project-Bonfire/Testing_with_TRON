package com.uppaal.chiporiginal;

public class OpenCommandToCallModelSim {
	
	public void testRun(){
	 try
     { 
      // We are running "dir" and "ping" command on cmd
     Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"dir && ping localhost\"");
    //. Process p = Runtime.getRuntime().exec(new String[]{"Konsole --workdir /home/apneet/git/Testing_with_Tron/tmp/simul_temp -e'/bin/tcsh', ModSimCallFile.tcl"});
   //  System.out.println(p);	
     }
     catch (Exception e)
     {
         System.out.println("HEY Buddy ! U r Doing Something Wrong ");
         e.printStackTrace();
     }
	}
	public static void main (String args[]){
		
		OpenCommandToCallModelSim test = new OpenCommandToCallModelSim();
		test.testRun();
	}
	
	
}
