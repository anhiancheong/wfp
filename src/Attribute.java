import org.apache.commons.lang3.builder.HashCodeBuilder;
/*
 * Attribute represents a single attribute about a person
 * This can be thought of a as key-value pair
 * The class tracks some metadata about the Attribute in question
 * ex: {location:Paris}
 * */
public class Attribute {


	String attrName = "";
	String attrVal = "";
	double confidence = 0.0;
	String source = "";
	
	public Attribute(String inptAttrName, String inptAttrVal) {
		// TODO Auto-generated constructor stub
		attrName = inptAttrName;
		attrVal = inptAttrVal;
	}
	
	public Attribute(String inptAttrName, String inptAttrVal, String inptSource) {
		attrName = inptAttrName;
		attrVal = inptAttrVal;
		source = inptSource;
	}
	
	public Attribute(String inptAttrName, String inptAttrVal, double prob,
			String inptSource) {
		attrName = inptAttrName;
		attrVal = inptAttrVal;
		confidence = prob;
		source = inptSource;
	}
	/*TODO
	 * Track the source of the attribute
	 * Track whether this attribute is a known/initial/ground attribute
	 * others?
	 * */
	public String getName() {
		return attrName;
	}
	
	public String getVal() {
		return attrVal;
	}
	
	public String getSource() {
		return source;
	}
	
	public int hashCode() {
		return new HashCodeBuilder(17,31).append(attrName).append(attrVal).toHashCode();
	}
}
