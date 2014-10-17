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
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		dbWrapper.initGlobalWrapper();

        driver.testNameFetch();
        //debugPrint.print("Driver finished");
        //read in config file
        loadConfig("testConfig.txt");
        //initialize person
        Person experimentPerson = new Person("kevin", "barrett", websitesUsed);
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
        experimentPerson.outputStateToLog("After last Inference Round");
        //call population engine
        
        
        //while inference is true
        
        //post results to database
		experimentPerson.outputLogToFile();
	}

	
	public static void testNameFetch() {
		
		String id = dbWrapper.db.queryNameMapping("anna", "genis");
		//debugPrint.print("Name Mapping Id for Anna Genis is: " + id);
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

			Constants.populationThreshold = (float) params.getDouble("confidenceThreshold_forYifang");
			
			Constants.crossSiteThreshold = (float) params.getDouble("aggregateThreshold");
			
			Constants.websiteThreshold = (float) params.getDouble("individualWebsiteThreshold");
			
			Constants.experimentID = "" + params.getInt("experimentId");

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
