package application.ui;

/**
 * Provides the entry point of the application. When JavaFX is loaded from the class path,
 * as it is in the distribution, the main class must not extend {@link javafx.application.Application}.
 */
public class Launcher {

	/**
	 * Starts the application.
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		MainFx.main(args);
	}

}
