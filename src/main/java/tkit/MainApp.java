package tkit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application entry point for Tkit.
 * Loads the main window FXML and attaches the stylesheet.
 */
public class MainApp extends Application {

    /**
     * Initializes and shows the primary stage.
     *
     * @param stage the primary JavaFX stage
     * @throws Exception if FXML loading fails
     */
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(
                MainApp.class.getResource("/tkit/MainWindow.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        scene.getStylesheets().add(
                MainApp.class.getResource("/tkit/chat.css").toExternalForm());
        stage.setTitle("Tkit");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Standard Java entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
