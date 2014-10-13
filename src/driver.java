import java.util.ArrayList;


public class driver {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		dbWrapper.initGlobalWrapper();

        driver.testNameFetch();
        debugPrint.print("Driver finished");
		
	}

	
	public static void testNameFetch() {
		
		String id = dbWrapper.db.queryNameMapping("anna", "genis");
		debugPrint.print("Name Mapping Id for Anna Genis is: " + id);
	}
}
