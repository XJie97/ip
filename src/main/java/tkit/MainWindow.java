package tkit;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class MainWindow {
    @FXML private ScrollPane scrollPane;
    @FXML private VBox dialogContainer;
    @FXML private TextField userInput;
    @FXML private Button sendButton;

    private final CommandProcessor core = new CommandProcessor();

    @FXML
    public void initialize() {
        // Initial bot message
        dialogContainer.getChildren().add(
                DialogBox.bot("Tkit ready. Commands: list | todo | deadline | event | mark | unmark | delete | on | find | bye"));
        scrollPane.vvalueProperty().bind(dialogContainer.heightProperty());
    }

    @FXML
    private void handleUserInput() {
        String input = userInput.getText();
        if (input == null || input.isBlank()) return;

        dialogContainer.getChildren().add(DialogBox.user(input));
        String response = core.handle(input);
        dialogContainer.getChildren().add(DialogBox.bot(response));
        userInput.clear();

        if (core.isExit(input)) {
            Platform.exit();
        }
    }
}
