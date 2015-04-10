import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import webfootprint.WebFootPrint2;
import webfootprint.engine.data.Constants;
import webfootprint.engine.data.Inference;
import webfootprint.engine.data.Predict;
import webfootprint.engine.data.WebUser;



/*
 * Person represents a hypothetical person that is the 'target' of the inference process
 * The objective of the inference process is to infer as many attributes about this person as possible
 * The only thing we are guarenteed to know about this person is their First and Last name
 * There are many social media profiles associated with this person and each profile has a set of attributes
 * These attributes are collected across multiple websites
 * */
public class Person {

	String firstName = "";
	String lastName = "";
	String nameMappingId = "";
	
	/*TODO
	 * Data structure for initial attributes
	 * Data structure to hold ground truth information for comparison? This is done at the database table level using Kevin's script
	 * Data structure to hold set of profiles for each website*/
	ArrayList<Attribute> initialAttributes;
	
	
	HashMap<String, ProfileSet> websites;
	
	HashMap<Integer, Attribute> coreAttributes;
	
	String fileDump = "";
	public String gtId;
	
	public Person(String fname, String lname, ArrayList<String> websiteNames) {
		firstName = fname;
		lastName = lname;
		nameMappingId = dbWrapper.db.queryNameMapping(firstName, lastName); 
		initialAttributes = new ArrayList<Attribute>();
		websites = new HashMap<String, ProfileSet>();
		coreAttributes = new HashMap<Integer, Attribute>();
		
		gtId = dbWrapper.db.getGtId(firstName, lastName);
		
		for(String web: websiteNames) {
			websites.put(web, new ProfileSet(web));
			debugPrint.print("Experiment using website: " + web,3);
		}
	}
	//Query database for the ground truth values, returns false if the initial values could not
	//be populated
	public boolean populateInitialAttributes(ArrayList<String> initialAttrNames) {
	
		//TODO database call
		debugPrint.print("Number of different attributes expected in initial set: " + initialAttrNames.size(), 3);
		ArrayList<Attribute> initialAttributesFromDB = dbWrapper.db.getInitialAttributes(firstName, lastName, initialAttrNames);
		
		//check if sufficient attributes are present
		
		HashSet<String> foundAttributes = new HashSet<String>();
		//Add these so that the count will match up with initial attr names
		
		
		for(Attribute attr: initialAttributesFromDB) {
			initialAttributes.add(attr);
			coreAttributes.put(attr.hashCode(), attr);
			foundAttributes.add(attr.getName());
			debugPrint.print("Attribute was added to the initial starting set " + attr.getName() + " " + attr.getVal(), 3);
		}
		if(foundAttributes.size() < initialAttrNames.size()){
			debugPrint.print("Insufficient number of ground truth values found; invalid profile",2);
			return false;
		}
		return true;
	}
	
	/*This method launches the primary inference process and returns true if values in its core have changed*/
	public boolean infer() {
		boolean retVal = false;
		boolean indivResult = false;
		boolean crossResult = false;
		
		//setup value filter list BEFORE the loop for websites
		//otherwise each website will affect the next
		ArrayList<Attribute> filterList = new ArrayList<Attribute>();
		for(Attribute attr: coreAttributes.values()){
			filterList.add(attr);
		}
		
		// Handle Individual Website inference
		for(String website: websites.keySet()) {
			debugPrint.print("Now infering from: " + website, 3);
			ProfileSet ps = websites.get(website);
			ps.clear();
			//debugPrint.print("Getting profiles...");
			ps.getProfiles(firstName, lastName, website, nameMappingId);

			debugPrint.print("Filtering profiles....",3);
			ps.filterProfiles(filterList);
			//debugPrint.print("Calculating individual website attributes....");
			ps.calculateWebsiteAttributes();
			indivResult = updateCore(ps.getAttrAboveThreshold(ExperimentConstants.websiteThreshold));
		}
		
		debugPrint.print("Calculating cross site attributes");
		//handle cross-site inference
		HashMap<Integer, Attribute> crossSiteAttributes = new HashMap<Integer, Attribute>();
		for(String website: websites.keySet()) {
			//can assume probabilities already calculated and profiles filtered
			ProfileSet ps = websites.get(website);
			ArrayList<Attribute> crossSite = ps.getAttrAboveThreshold(ExperimentConstants.crossSiteThreshold);
			for(Attribute attr: crossSite) {
				if(!crossSiteAttributes.containsKey(attr.hashCode())) {
					crossSiteAttributes.put(attr.hashCode(), attr);
				}
				crossSiteAttributes.get(attr.hashCode()).addConfToTotal(attr.confidence);
			}
		}//website for loop
		

		//Iterate across collection of attributes, find those with count > 1, add those to core
		ArrayList<Attribute> crossSiteList = new ArrayList<Attribute>();
		if(ExperimentConstants.doCrossSite) {
			for(Integer i: crossSiteAttributes.keySet()) {
				Attribute curAttr = crossSiteAttributes.get(i);
				//If the attribute occured on more than 1 website
				if(curAttr.getCount() > 1) {
					crossSiteList.add(new Attribute(curAttr.getName(), curAttr.getVal(), curAttr.getAvgConf(), curAttr.getSource() + ",cross-site"));
				    debugPrint.print("Found a cross-site attribute - " + curAttr.getVal(), 4);
				}
			}
			debugPrint.print("Number of possible cross-site values: " + crossSiteList.size(), 3);
		}

		//update the core if any of the cross site attributes are not yet present
		crossResult = updateCore(crossSiteList);
		retVal = indivResult || crossResult;
		debugPrint.print("Did any new values get added to the core or confidences updated? " + retVal, 3);
		return retVal;
	}
	
	/*Method will check the core and insert new attributes, 
	 * update the probabilities of existing attributes, or do nothing
	 * and return a boolean indicating what it did*/
	public boolean updateCore(ArrayList<Attribute> newAttr) {
		boolean updated = false;
		
		for(Attribute curNewAttr: newAttr) {
			if(!coreAttributes.containsKey(curNewAttr.hashCode())){
				debugPrint.print("New attribute added to core! I win!",2);
				debugPrint.print(curNewAttr.toString(),3);
				coreAttributes.put(curNewAttr.hashCode(), curNewAttr);
				updated = true;
			}
			//if the value is already in the core, check if the current attr has a higher confidence
			boolean maybeUpdated = coreAttributes.get(curNewAttr.hashCode()).maybeUpdate(curNewAttr);
			
			if(maybeUpdated)
				updated = true;
		}
		return updated;
	}
	
	/*If we find a high confidence profile ID in a cross site value, then we want to
	 * do a lookup on that profile ID and add all attributes from that profile with X confidence*/
	public void checkForProfileIds() {
		
	}
	
	public void outputStateToLog(String stateHeader){
		fileDump += "\n ========== " + stateHeader + "==========\n";
		for(Attribute attr: coreAttributes.values()) {
			fileDump += attr.toString() + "\n";
		}
	}
	
	public void outputLogToFile(){
		FileWriter fw;
		try {
			fw = new FileWriter("Log" + ExperimentConstants.experimentID + ".txt");
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(fileDump);
			bw.close();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void populationInfer() {
		// TODO Auto-generated method stub
	  debugPrint.print("Starting Population Inference Engine step", 5);
		/*Setup data strucutre to pass to Yifang's code*/
	  HashMap<String, HashMap<String, HashSet<String>>> data = new HashMap<String, HashMap<String, HashSet<String>>>();
	  //<User ID <Attribute Name, <Values>>
	  HashMap<String, HashSet<String>> valueSet = new HashMap<String, HashSet<String>>();
	  for(Attribute attr: coreAttributes.values()){
		  if(!valueSet.containsKey(attr.getName())) {
			  valueSet.put(attr.getName(), new HashSet<String>());
		  }
		  valueSet.get(attr.getName()).add(attr.getVal());
	  }
	  data.put("" + gtId, valueSet);
	
	WebFootPrint2 popInferenceEngine = new WebFootPrint2(dbWrapper.populationDbConn);
	ArrayList<WebUser> popResults = popInferenceEngine.getInferences(data);
	
	ArrayList<Attribute> populationInferenceAttributes = new ArrayList<Attribute>();
	
	for(int i = 0; i < popResults.size(); i++) {
		WebUser user = popResults.get(i);
		System.out.println("USER: " + user.getProfile().getUserId());
		Inference inference = user.getInference();
		for(int j = 0; j < inference.size(); j++) {
			String attribute = inference.getAttribute(j);
			ArrayList array = inference.getAttributeValue(attribute);
			for(int k = 0; k < array.size(); k++) {
				Predict predict = (Predict)array.get(k);
				String algorithm;
				String confidence;
				switch(predict.getAlgorithm()) {
				case(Constants.ASSOCIATION_RULE_MINING):
					algorithm = "APRIORI";
					confidence = "apriori_confidence";
					break;
				case(Constants.NAIVE_BAYES):
					algorithm = "NAIVE_BAYES";
					confidence = "naive_bayes_majority_confidence";
					break;
				case(Constants.LDA):
				default:
					algorithm = "LDA";		
					confidence = "lda_majority_vote_confidence";
					break;
				}
				System.out.println("ATTRIBUTE: " + attribute + "\tALGRORITHM: " + algorithm + "\tANSWER: " + predict.getAnswer() + "\tCONFIDENCE: " + (Double)predict.getUserData().getUserDatum(confidence));
			    double attrConf = (Double)predict.getUserData().getUserDatum(confidence);
				if(attrConf >= ExperimentConstants.populationThreshold) {
			    	populationInferenceAttributes.add(new Attribute(attribute, predict.getAnswer(), attrConf, "PopulationEngine-" + algorithm));
			    	debugPrint.print("Attribute added from population inference engine", 5);
			    }
			}
		}
	}
	debugPrint.print("Ending Population Inference Engine Step", 5);
	updateCore(populationInferenceAttributes);
	
	}
	/*Checks for website IDs that were found across several websites; if any are found, all attributes from that ID
	 * are added*/
	public void checkForCrossSiteProfiles() {
		// TODO Auto-generated method stub
		for(Attribute attr:coreAttributes.values()) {
			//name is the id from a website
			if(attr.getName().contains("_id")){
				//the source for that attribute was a cross site attribute
				if(attr.getSource().contains("cross")) {
					ArrayList<Attribute> allProfileAttributes = dbWrapper.db.getAllAttributeForProfile(attr.getName().replace("_id","") ,attr.getVal(), attr.confidence);
					if(allProfileAttributes != null)
					  updateCore(allProfileAttributes);
				}
			}
		}
	}
}
