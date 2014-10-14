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
		ArrayList<Attribute> allAttr = new ArrayList<Attribute>();
		
		//Combine all attributes from all profiles
		for(Profile p: profiles) {
			allAttr.addAll(p.attributes);
		}
		//Tracks the number of times a certain hash
		HashMap<String, ArrayList<Attribute>> attrHashMap = new HashMap<String, ArrayList<Attribute>>();
		
		//Populate the attrHashMap, collect all attributes of the same attribute name
		for(Attribute attr: allAttr) {
			if(!attrHashMap.containsKey(attr.getName())){
				attrHashMap.put(attr.getName(), new ArrayList<Attribute>());
			}
			attrHashMap.get(attr.getName()).add(attr);
		}
		//iterate through the arrayList for each attribute Name, sum up occurrences of each value
		//see if any meet the threshold and add them to a set level set of attributes
		for(String attrName: attrHashMap.keySet()) {
			HashMap<String, Integer> valueCount = new HashMap<String, Integer>();
			ArrayList<Attribute> attrVals = attrHashMap.get(attrName);
			int numVals = attrVals.size();
			for(Attribute attr: attrVals) {
				if(valueCount.containsKey(attr.getVal())) {
					valueCount.put(attr.getVal(), 0);
				}
				valueCount.put(attr.getVal(), valueCount.get(attr.getVal()) + 1);
			}
			
			//Setup the profile set level attributes
			for(String attrVal: valueCount.keySet()) {
				double prob = valueCount.get(attrVal) / numVals;
				if (prob >= Constants.websiteThreshold) {
					setAttributes.add(new Attribute(attrName, attrVal, prob, source));
				}
			}
		}
	}
}
