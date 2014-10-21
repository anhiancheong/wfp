import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;


public class driver {

	static ArrayList<String> websitesUsed = new ArrayList<String>();
	static ArrayList<String> initialAttributeNames = new ArrayList<String>();
	static String nameFile = "names.txt";
	static String configFile = "config.txt";
	static String logFile = "log.txt";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		parseParams(args);
		// TODO Auto-generated method stub
		dbWrapper.initGlobalWrappers();

        //debugPrint.print("Driver finished");
        //read in config file
        loadConfig(configFile);
        try {
        	
            FileReader fr = new FileReader(nameFile);
            BufferedReader br = new BufferedReader(fr);
            String nameStr = "";
            while((nameStr = br.readLine()) != null) {
            	
            	String[] nameArr = nameStr.split("\\|");
            	String firstName = nameArr[0];
            	String lastName = nameArr[1];
            	debugPrint.print("Experiment on " + firstName  + " " + lastName);
            	//initialize person
                Person experimentPerson = new Person(firstName, lastName, websitesUsed);
                //populate initial values of person
                if(!experimentPerson.populateInitialAttributes(initialAttributeNames)){
                	debugPrint.print("Could not the sufficient ground truth values for this person");
                	return;
                } 
                experimentPerson.outputStateToLog("After Initial Attributes Collected");
                //while inference is true, infer
                int round = 1;
                //debugPrint.print("Starting Inference");
                while(experimentPerson.infer()){
                	debugPrint.print("Infering.....round" + round,3);
                	experimentPerson.outputStateToLog("Inference Round " + round);
                	round++;
                }
                experimentPerson.outputStateToLog("After last Inference Round before population");
                //call population engine
                experimentPerson.populationInfer();
                experimentPerson.outputStateToLog("After last population inference engine");
                
                while(experimentPerson.infer()){
                	debugPrint.print("Infering.....round" + round,3);
                	experimentPerson.outputStateToLog("Inference Round post population" + round);
                	round++;
                }
                experimentPerson.outputStateToLog("After last Inference Round after population");
                
                //post results to database
        		experimentPerson.outputLogToFile();
        		dbWrapper.db.postExperimentResults(experimentPerson);       	
            }
        }
        catch (IOException e) {
        	e.printStackTrace();
        }   
	}

	
	public static void parseParams(String[] args) {
		if(args.length < 3) {
			System.out.println("Insufficent Parameters - Expected params file.jar <config.txt> <names.txt> <debug mode>");
			System.exit(0);
		}
		configFile = args[0];
		nameFile = args[1];
		debugPrint.printSet = Integer.parseInt(args[2]);
	}
	
	/** This method will read a json configured file and load the various run parameters
	 *  I just used Tavish's code for this part
	 * */
	public static void loadConfig(String filename) {
		try {
			Reader read = null;
			read = new FileReader(new File(filename));
			JSONTokener jsonReader = new JSONTokener(read);
			JSONObject params = new JSONObject(jsonReader);
			/*
			 * Reading in configuration information from the file, things like confidence, merging thresholds etc
			 * */
			System.out.println(params.toString(3));

			ExperimentConstants.populationThreshold = (float) params.getDouble("confidenceThreshold_forYifang");
			
			ExperimentConstants.crossSiteThreshold = (float) params.getDouble("aggregateThreshold");
			
			ExperimentConstants.websiteThreshold = (float) params.getDouble("individualWebsiteThreshold");
			
			ExperimentConstants.experimentID = "" + params.getInt("experimentId");

			//Getting which websites this experiment will seek to use
			JSONArray temp = params.getJSONArray("websites");

			for (int i = 0; i < temp.length(); i++) {
				websitesUsed.add(temp.getString(i));
			}

			//Gets the attirbutes for the initial core
			JSONArray tempInit = params.getJSONArray("initialCoreAttributes");

			for (int i = 0; i < tempInit.length(); i++) {
				if (tempInit.getString(i).equals("last_name") || tempInit.getString(i).equals("first_name"))
					continue;
				
				initialAttributeNames.add(tempInit.getString(i));
			}

			//debugPrint.print("Finished reading config file");
			
		}//end of try block
		catch(IOException ie){
			ie.printStackTrace();
		}
		catch(JSONException je){
			je.printStackTrace();
		}
	}
}
