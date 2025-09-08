package tkit;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

final class DialogBox {

    private DialogBox() { }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private static StackPane bubble(String text, String styleClass) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.getStyleClass().addAll("bubble-base", styleClass);

        StackPane p = new StackPane(l);
        p.setMaxWidth(520);
        StackPane.setMargin(l, new Insets(0));
        return p;
    }

    /** Round image avatar loaded from resources. */
    private static StackPane avatar() {
        Image img = new Image(
                MainApp.class.getResource("/tkit/gemini_tkit.png").toExternalForm(),
                36, 36, true, true);
        ImageView iv = new ImageView(img);
        iv.setFitWidth(36);
        iv.setFitHeight(36);

        Circle clip = new Circle(18, 18, 18);
        iv.setClip(clip);

        StackPane a = new StackPane(iv);
        a.setMinSize(36, 36);
        a.setPrefSize(36, 36);
        a.setMaxSize(36, 36);
        return a;
    }

    /** Left-aligned bot message with avatar. */
    static Node bot(String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.TOP_LEFT);
        row.getChildren().addAll(avatar(), bubble(text, "bot-bubble"));
        row.setFillHeight(false);
        return row;
    }

    /** Right-aligned user message (no avatar). */
    static Node user(String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.TOP_RIGHT);
        row.getChildren().addAll(spacer(), bubble(text, "user-bubble"));
        row.setFillHeight(false);
        return row;
    }
}
