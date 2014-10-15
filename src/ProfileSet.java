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
	
	public ProfileSet () {
		profiles = new ArrayList<Profile>();
		setLevelAttributes = new HashMap<String, AttributeSet>();
		source = "";
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
		dbWrapper db = new dbWrapper();
		ArrayList<String> ids = dbWrapper.db.queryWebsiteId(website, nameMappingId);
		debugPrint.print(ids.toString());
		
		for(String id: ids) {
			// Add profile to the list
			//Construct a new profile object for each id
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
		for(Profile p: profiles) {
			if(p.maybeFilter(knownAttr)) {
				profiles.remove(p);
			}
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
		ArrayList<Attribute> retList = new ArrayList<Attribute>();
		for(String key: setLevelAttributes.keySet()) {
			retList.addAll(setLevelAttributes.get(key).getValidAttributes(threshold));
		}
		return retList;
	}
}