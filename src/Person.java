import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;



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
	
	public Person(String fname, String lname, ArrayList<String> websiteNames) {
		firstName = fname;
		lastName = lname;
		nameMappingId = dbWrapper.db.queryNameMapping(firstName, lastName); 
		initialAttributes = new ArrayList<Attribute>();
		websites = new HashMap<String, ProfileSet>();
		coreAttributes = new HashMap<Integer, Attribute>();
		
		for(String web: websiteNames) {
			websites.put(web, new ProfileSet(web));
		}
	}
	//Query database for the ground truth values, returns false if the initial values could not
	//be populated
	public boolean populateInitialAttributes(ArrayList<String> initialAttrNames) {
	
		//TODO database call
		
		for(Attribute attr: initialAttributes) {
			coreAttributes.put(attr.hashCode(), attr);
			debugPrint.print("Attribute was added to the initial starting set " + attr.getName() + " " + attr.getVal(), 1);
		}
		return false;
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
			ProfileSet ps = websites.get(website);
			ps.clear();
			//debugPrint.print("Getting profiles...");
			ps.getProfiles(firstName, lastName, website, nameMappingId);

			debugPrint.print("Filtering profiles....",3);
			ps.filterProfiles(filterList);
			//debugPrint.print("Calculating individual website attributes....");
			ps.calculateWebsiteAttributes();
			indivResult = updateCore(ps.getAttrAboveThreshold(Constants.websiteThreshold));
		}
		
		//debugPrint.print("Calculating cross site attributes");
		//handle cross-site inference
		HashMap<Integer, Attribute> crossSiteAttributes = new HashMap<Integer, Attribute>();
		for(String website: websites.keySet()) {
			//can assume probabilities already calculated and profiles filtered
			ProfileSet ps = websites.get(website);
			ArrayList<Attribute> crossSite = ps.getAttrAboveThreshold(Constants.crossSiteThreshold);
			for(Attribute attr: crossSite) {
				if(!crossSiteAttributes.containsKey(attr.hashCode())) {
					crossSiteAttributes.put(attr.hashCode(), attr);
				}
				crossSiteAttributes.get(attr.hashCode()).addConfToTotal(attr.confidence);
			}
		}//website for loop
		
		//Iterate across collection of attributes, find those with count > 1, add those to core
		ArrayList<Attribute> crossSiteList = new ArrayList<Attribute>();
		for(Integer i: crossSiteAttributes.keySet()) {
			Attribute curAttr = crossSiteAttributes.get(i);
			if(curAttr.getCount() > 1) {
				crossSiteList.add(new Attribute(curAttr.getName(), curAttr.getVal(), curAttr.getAvgConf(), curAttr.getSource() + ",cross-site"));
			}
		}
		crossResult = updateCore(crossSiteList);
		retVal = indivResult || crossResult;
		return retVal;
	}
	
	/*Method will check the core and insert new attributes, 
	 * update the probabilities of existing attributes, or do nothing
	 * and return a boolean indicating what it did*/
	public boolean updateCore(ArrayList<Attribute> newAttr) {
		boolean updated = false;
		
		for(Attribute curNewAttr: newAttr) {
			if(!coreAttributes.containsKey(curNewAttr.hashCode())){
				//debugPrint.print("New attribute added to core! I win!");
				//debugPrint.print(curNewAttr.toString());
				coreAttributes.put(curNewAttr.hashCode(), curNewAttr);
			}
			coreAttributes.get(curNewAttr.hashCode()).maybeUpdate(curNewAttr);
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
			fw = new FileWriter("Log" + Constants.experimentID + ".txt");
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(fileDump);
			bw.close();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
