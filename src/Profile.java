import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;


/*
 * Profile represents a single social media profile that was collected
 * */
public class Profile {

	/*The website this profile came from*/
	String website = "";
	/*The unique website id for this profile*/
	String profileId = "";
	/*The id of the profile in the database*/
	String databaseId = "";
	/*The id mapping of this profile's source name*/
	String nameMappingID = "";
	
	String firstName = "";
	String lastName = "";
	
	
	/*A list of all attributes for this person from the website
	 *A list is used to handle if a person has more than 1 value for any attribute*/
	HashMap<String, AttributeSet> attributeSets;
	
	public Profile(String fName, String lName, String inptWebsite, String inptProfileId) {
		// TODO Auto-generated constructor stub
		firstName = fName;
		lastName = lName;
		website = inptWebsite;
		profileId = inptProfileId;
		attributeSets = new HashMap<String, AttributeSet>();
	}

	public void queryAttributes() {
		// TODO Auto-generated method stub
		ResultSet rs = dbWrapper.db.queryWebsiteAttr(website, profileId);
		try {
			while(rs.next()) {
				String attrName = rs.getString("attribute_name");
				String attrVal = rs.getString("attribute_value");
				if(!attributeSets.containsKey(attrName)) {
					//debugPrint.print("Adding new attribute to person : " + profileId + " -- " + attrName + " - " + attrVal);
					attributeSets.put(attrName, new AttributeSet(attrName));
				}
				
				attributeSets.get(attrName).add(new Attribute(attrName, attrVal, website));
			}
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
	}

	/*Function will check if all of the value pairs in knownAttr are in this profile
	 * if they aren't the function returns false
	 * True - this profile should be filtered - ie it has contradictory values*/
	public boolean maybeFilter(ArrayList<Attribute> knownAttr) {
		//for each variable in known attr, if the value in this profile for that name
		//contradicts that value, return false
		for(Attribute attr: knownAttr) {
			if(attributeSets.containsKey(attr.getName())) {
				//one of the values must agree with the known value
				if(!attr.getVal().equals(attributeSets.get(attr.getName()).hasValue(attr.getVal()))) {
					//The known value is not in this profile
					debugPrint.print("Known value: " + attr.getVal()+ " is not in this profile (id="+ profileId +"), it will be filtered", 3);
					return true;
				}
			}
		}
		return false;
	}
	
}

//OLD CODE

// TODO Auto-generated method stub
//Check if all known attr are in this profile
/*
int totalKnown = knownAttr.size();
int knownFound = 0;
if( knownAttr.isEmpty()) {
	return false;
}

HashMap<String, String> knownVals = new HashMap();
for(Attribute attr: knownAttr) {
	knownVals.put(attr.getName(), attr.getVal());
}

for(String key: knownVals.keySet()) {
	if(attributeSets.get(key).hasValue(knownVals.get(key))) {
		knownFound++;
	}
}
// Case where the profile fails to meet the necessary number of attributes in the core
if (knownFound < totalKnown) {
	return true;
}
return false;
*/