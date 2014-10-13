import java.util.ArrayList;
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
	 * Data structure to hold ground truth information for comparison? This is done at the database table leve using Kevin's script
	 * Data structure to hold set of profiles for each website*/
	ArrayList<Attribute> initial_attributes;
	

	
	HashSet<ProfileSet> websites;
	
	
	public Person(String fname, String lname) {
		firstName = fname;
		lastName = lname;
		nameMappingId = dbWrapper.db.queryNameMapping(firstName, lastName); 
	}
	
}
