import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;



public class dbWrapper {

	/*Login parameters*/
	String username = "kevin";
	String password = "kevin";
	String databaseName = "wfp";
	String hostname = "localhost";
	//String hostname = "141.161.20.61:5432";
	/*Tracks the current sql Statement*/
	Statement currentStatement;
	String currentQuery = "";
	ResultSet currentResultSet;
	
	public static dbWrapper db;
	public static Connection populationDbConn;
	
	
	public dbWrapper() {
		Connection conn = null;
		try {
			Class.forName("org.postgresql.Driver"); //load the driver
            conn = DriverManager.getConnection("jdbc:postgresql://"+ hostname +":" +ExperimentConstants.portNum+ "/" + databaseName +"?user="+username+"&password="+password+"");
            currentStatement = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		}
		catch(ClassNotFoundException ce){
			ce.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void initGlobalWrappers() {
		db = new dbWrapper();
		try {
			populationDbConn = DriverManager.getConnection("jdbc:postgresql://localhost:"+ExperimentConstants.portNum+"/wfpbasic?user=kevin&password=kevin");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
	
	public void execute() {
		try {			
			if(currentQuery.startsWith("INSERT")){
				currentStatement.executeUpdate(currentQuery);
				return;
			}			
			currentStatement.executeQuery(currentQuery);
			if(currentStatement.getResultSet() != null){
				currentResultSet = currentStatement.getResultSet();
			}
			else {
				//debugPrint.print("Query returned null");
				//debugPrint.print("Query: " + currentQuery);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//debugPrint.print("Error when processing query: \n" + currentQuery);
		}		
	}

	public String queryNameMapping(String firstName, String lastName) {
		//Setup Query
		currentQuery = "SELECT name_id FROM name_mapping WHERE ";
		currentQuery += " LOWER(first_name) = LOWER('" + firstName + "')";
		currentQuery += " AND LOWER(last_name) = LOWER('" + lastName + "');";
		
		debugPrint.print(currentQuery, 1);
		//Execute the Query
		execute();
		String nameMappingId = "-1";
		try {
			if(currentResultSet.first()) {
				//debugPrint.print("result set is at first row");
			}
			nameMappingId = currentResultSet.getString("name_id");
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			//debugPrint.print("Error happened in Name Mapping lookup for person: " + firstName + " " + lastName);
			//System.exit(0);
			return "ERROR";
		}
		debugPrint.print("Found id: " + nameMappingId + " for person: " + firstName + " " + lastName, 1);
		debugPrint.print("Found id: " + nameMappingId + " for person: " + firstName + " " + lastName, 3);
		return nameMappingId;
	}

	public ArrayList<String> queryWebsiteId(String website, String nameMappingId) {
		// TODO Auto-generated method stub
		//debugPrint.print("Querying website: " + website + " for name id: " + nameMappingId);
		ArrayList<String> ids = new ArrayList<String>();
		currentQuery = "SELECT website_id FROM " + website + " WHERE name_id_source = " + nameMappingId + " ;";
		execute();
		HashSet<String> idsSeen = new HashSet<String>();
		try {
			while(currentResultSet.next()) {
				String curId = currentResultSet.getString("website_id");
				if(!idsSeen.contains(curId)) {
					idsSeen.add(curId);
					ids.add(curId);
				}
			}
		}
	    catch (SQLException e1) {
	    	e1.printStackTrace();
	    }
		return ids;
	}

	public ResultSet queryWebsiteAttr(String website, String profileId) {
		// TODO Auto-generated method stub
		currentQuery = "SELECT * FROM " + website + " WHERE website_id = '" + profileId + "' and ignore = FALSE ;";
		execute();
		return currentResultSet;
	}

	public ArrayList<Attribute> getInitialAttributes( String firstName, String lastName,
			ArrayList<String> initialAttrNames) {
		
		ArrayList<Attribute> returnAttr = new ArrayList<Attribute>();
		currentQuery = "SELECT * from gt_attributes where lower(first_name) = lower('" + firstName + "')";
		currentQuery += " and lower(last_name) =lower('" + lastName + "');";
		execute();
		
		try {
			while(currentResultSet.next()) {
				String attrName = currentResultSet.getString("attribute_name");
				String attrVal = currentResultSet.getString("attribute_value");
				if(initialAttrNames.contains(attrName)) {
					if(!attrName.equals("first_name") && !attrName.equals("last_name")){
						returnAttr.add(new Attribute(attrName, attrVal, 1.0, "Initial"));
					}
					
				}
			}
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		debugPrint.print("Added " + returnAttr.size() + " initial values", 3);
		return returnAttr;
	}
	
	public void postExperimentResults(Person person){
		
		//check if the experiment exists already
		boolean experimentExists = ExperimentConstants.experimentCreated;
		if(!experimentExists) {
			String experimentId = ExperimentConstants.experimentID;
			String websitesString = "";
			String websitesUsedString = "";
			String initialAttributesString = "";
			
			for(Attribute attr: person.initialAttributes) {
				initialAttributesString += "-" + attr.getName();
			}
			
			for(String curWebsite:person.websites.keySet()){
				websitesString += ", " + curWebsite;
				websitesUsedString += ", True";
			} 	
			
			currentQuery = "INSERT INTO experiment (experiment_id, initial_attributes, single_site, cross_site, population_engine"+websitesString+") VALUES (";
			currentQuery += experimentId + ",";
			currentQuery += "'" + initialAttributesString + "',";
			currentQuery += ExperimentConstants.websiteThreshold + ",";
			currentQuery += ExperimentConstants.crossSiteThreshold + ",";
			currentQuery += ExperimentConstants.populationThreshold;
			currentQuery += websitesUsedString;
			currentQuery += ");";
			debugPrint.print(currentQuery,4);
			execute();
			ExperimentConstants.experimentCreated = true;
		}
		
		//post inference results
		for(Attribute attr: person.coreAttributes.values()){
			currentQuery = "INSERT INTO experiment_attributes (experiment_id, first_name, last_name, gt_id, attribute_name, attribute_value, confidence, source) VALUES (";
			currentQuery += ExperimentConstants.experimentID + ",";
			currentQuery += "'" + person.firstName + "',";
			currentQuery += "'" + person.lastName + "',";
			currentQuery += person.gtId + ",";
			currentQuery += "'" + attr.getName() + "',";
			currentQuery += "'" + attr.getVal() + "',";
			currentQuery += attr.getConf() + ",";
			currentQuery += "'" + attr.getSource() + "'";
			
			currentQuery += ");";
			debugPrint.print(currentQuery,4);
			execute();
		}
		
		
	}

	public String getGtId(String firstName, String lastName) {
		
		String gtId = "";
		currentQuery = "SELECT gt_id FROM gt_person WHERE lower(first_name) = lower('" + firstName + "') AND lower(last_name) = lower('" + lastName + "');";		
		execute();
		
		try {
			currentResultSet.next();
			gtId = "" + currentResultSet.getInt("gt_id");
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		
		return gtId;
	}

	public ArrayList<Attribute> getAllAttributeForProfile(String websiteName,
			String value, double confidence) {
		// TODO Auto-generated method stub
		ArrayList<Attribute> retList = new ArrayList<Attribute>();
		currentQuery = "SELECT * FROM " + websiteName + " WHERE '" + websiteName + "_id' = '" + value + "' and ignore=FALSE;";
		execute();
		
		try{
			while(currentResultSet.next()) {
				String attrName = currentResultSet.getString("attribute_name");
				String attrValue = currentResultSet.getString("attribute_value");
				retList.add(new Attribute(attrName, attrValue, confidence, "profileId"));
			}
		}
		catch(SQLException e) {
			e.printStackTrace();
			return null;
		}
		
		return retList;
	}
}
