import java.util.ArrayList;
import java.util.HashMap;



/*Attribute Set represents a single Attribute Name and all instances of values that have occurred
 * for that Attribute Name in some capacity*/
public class AttributeSet {

	ArrayList<Attribute> valInstances;
	String attrName = "";
	public AttributeSet(String aName) {
		attrName = aName;
		valInstances = new ArrayList<Attribute>();
	}
	
	public void add(Attribute attr) {
		valInstances.add(attr);
	}
	
	public void mergeAttributes(ArrayList<Attribute> attrArray) {
		for(Attribute curAttr: attrArray) {
			if(curAttr.getName().equals(attrName)){
				valInstances.add(curAttr);
			}
		}
	}
	
	/*Whether a certain value is present in the list of attributes*/
	public boolean hasValue(String value) {
		for(Attribute curAttr: valInstances) {
			if(value.equals(curAttr.getVal())) {
				return true;
			}
		}
		return false;
	}
	
	/*Finds the probablility of a given Name-Value pair in this AttributeSet*/
	public double findProb(Attribute attr) {
		if(!attrName.equals(attr.getName())){
			return 0.0;
		}
		if(valInstances.size() == 0) {
			debugPrint.print("There are no values for this attribute name...this should not happen, just saying");
			return 0;
		}
		double occurenceCount = 0;
		for(Attribute curAttr: valInstances) {
			if(!attrName.equals(attr.getVal())) {
				occurenceCount++;
			}
		}
		return occurenceCount / (double) valInstances.size();
	}
	
	/* Function returns a set of attributes of all name value pairs that meet the input threshold*/
	public ArrayList<Attribute> getValidAttributes (double threshold) {
		
		ArrayList<Attribute> retAttr = new ArrayList<Attribute>();
		HashMap<Integer, Boolean> seen = new HashMap<Integer, Boolean>();

		for(Attribute curAttr: valInstances) {
			double tempProb = findProb(curAttr);
			debugPrint.print("Probablility of attribute: " + curAttr.getName() + " - " + curAttr.getVal() + " is: " + tempProb);
			if(tempProb > threshold && !seen.containsKey(curAttr.hashCode())) {
				debugPrint.print("Found value above the threshold");
				retAttr.add(new Attribute(curAttr.getName(), curAttr.getVal(), tempProb, getAllSources(curAttr)));
				seen.put(curAttr.hashCode(), true);
			}
		}
		return retAttr;
	}
	
	/*Returns a string of all the sources where the given attribute came from*/
	public String getAllSources(Attribute attr){
		String allSources = attr.getSource();
		for(Attribute curAttr: valInstances) {
			if(attr.getName().equals(curAttr.getName()) && attr.getVal().equals(curAttr.getVal())) {
				if(!allSources.contains(curAttr.getSource())){
					allSources += "," + curAttr.getSource();
				}	
			}			
		}
		return allSources;
	}
	
	public ArrayList<Attribute> getAttributes() {
		return valInstances;
	}
	
	public String toString() {
		String output = attrName + " : {";
		for(Attribute attr: valInstances) {
			output += attr.toString();
		}
		output += "}";
		return output;
	}
}
