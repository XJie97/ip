package tkit;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * JavaFX controller for the main chat window.
 * Wires UI controls to the command processor and renders dialogs.
 */
public class MainWindow {

    private static final String WELCOME =
            "Tkit ready."
        +    "Commands: list | todo | deadline | event | mark | unmark | delete | on | find | bye";

    @FXML private ScrollPane scrollPane;
    @FXML private VBox dialogContainer;
    @FXML private TextField userInput;
    @FXML private Button sendButton;

    private final CommandProcessor core = new CommandProcessor();

    /**
     * Called by the JavaFX framework after FXML fields are injected.
     * Adds the initial bot message and binds the scroll to content height.
     */
    @FXML
    public void initialize() {
        dialogContainer.getChildren().add(DialogBox.bot(WELCOME));
        scrollPane.vvalueProperty().bind(dialogContainer.heightProperty());
    }

    /**
     * Handles a user-entered line: echo, process, render response,
     * clear input, and exit if requested.
     */
    @FXML
    private void handleUserInput() {
        String input = userInput.getText();
        if (input == null || input.isBlank()) {
            return;
        }

        dialogContainer.getChildren().add(DialogBox.user(input));
        String response = core.handle(input);
        dialogContainer.getChildren().add(DialogBox.bot(response));
        userInput.clear();

        if (core.isExit(input)) {
            Platform.exit();
        }
    }
}
