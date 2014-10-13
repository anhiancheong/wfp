
public class debugPrint {
    public static boolean debugMode = true;	
	public static void print(String message) {
		if (debugMode) {
			System.out.println(message);
		}
	}
}
