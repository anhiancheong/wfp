
public class debugPrint {
    public static boolean debugMode = true;
    public static int printSet = 4;
    //print set 1 is database related queries
    //print set 2 is for data population debugging
    //print set 3 is for inference debugging
	public static void print(String message) {
		if (debugMode) {
			System.out.println(message);
		}
	}
	
	public static void print(String message, int set) {
		if(debugMode && set == printSet) {
			System.out.println(message);
		}
	}
}
