import java.util.ArrayList;
import java.util.HashMap;


/* ProfileSet represents a collection of profiles about a person
 * This may be used to collect all profiles of a target person on a specific website
 * Or can be used as an aggregated set of profiles across multiple websites*/
public class ProfileSet {

	/*Set of profiles*/
	ArrayList<Profile> profiles;
	
	HashMap<String, AttributeSet> setLevelAttributes;
	
	String source;
	
	public ProfileSet (String src) {
		profiles = new ArrayList<Profile>();
		setLevelAttributes = new HashMap<String, AttributeSet>();
		source = src;
	}
	
	/*TODO
	 * Track any and all metadata
	 * implement aggregating functions*/
	
	public void getProfiles(String firstName, String lastName, String website, String nameMappingId) {
		/* Algorithm overview
		 * - Query name mapping id for person with the given name
		 * - Query the specified website table for profile ids of the relevant profiles
		 * - For each profile, create a profile object, have profile object collect their attributes
		 * */
		dbWrapper db = dbWrapper.db;
		ArrayList<String> ids = dbWrapper.db.queryWebsiteId(website, nameMappingId);
		//debugPrint.print(ids.toString());
		debugPrint.print("Website " + website + " had " + ids.size() + " profiles for people with name: " + firstName + " " + "last_name",3);
		for(String id: ids) {
			// Add profile to the list
			//Construct a new profile object for each id
			//debugPrint.print("Making new profile object for id: " + id);
			Profile p = new Profile(firstName, lastName, website, id);
			//Populate the attributes for this profile
			p.queryAttributes();
			
			profiles.add(p);
		}

	}
	
	public void clear() {
		profiles = new ArrayList<Profile>();
		setLevelAttributes = new HashMap<String, AttributeSet>();
	}
	
	/*
	 * Function to remove all profiles that don't have the attribute values
	 * This will handle only analyzing profile that meet the requirements of attribute set given
	 * */
	public void filterProfiles(ArrayList<Attribute> knownAttr) {
		ArrayList<Profile> removeList =  new ArrayList<Profile>();
		for(Profile p: profiles) {
			if(p.maybeFilter(knownAttr)) {
				debugPrint.print("Profile being filtered", 3);
				removeList.add(p);
				//profiles.remove(p);
			}
		}
		
		for(Profile p: removeList) {
			profiles.remove(p);
		}
	}
	
	public void calculateWebsiteAttributes() {
		/* Algorithm Overview
		 * - Iterate across all profiles
		 * - Count the occurences of a given attribute name-value pair
		 * - Calculate the probability of that attribute in this profile set*/
		HashMap<String, AttributeSet> profileSetAttributes = new HashMap<String, AttributeSet>();
		for(Profile p: profiles) {
			for(String key: p.attributeSets.keySet()){
			  if(!profileSetAttributes.containsKey(key)){
				  profileSetAttributes.put(key, new AttributeSet(key));
			  }	
			  profileSetAttributes.get(key).mergeAttributes(p.attributeSets.get(key).getAttributes());
			}
		}
		setLevelAttributes = profileSetAttributes;
	}//end method
	
	public ArrayList<Attribute> getAttrAboveThreshold(double threshold){
		//debugPrint.print("Determining which attributes are above threshold");
		ArrayList<Attribute> retList = new ArrayList<Attribute>();
		//For each attribute set, get all attributes above the threshold
		for(String key: setLevelAttributes.keySet()) {
			retList.addAll(setLevelAttributes.get(key).getValidAttributes(threshold));
		}
		return retList;
	}
}
