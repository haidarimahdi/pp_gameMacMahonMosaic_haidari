package gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main class for the user interface.
 *
 * @author mjo, cei
 */
public class UserInterfaceController implements Initializable {

    /**
     * Label that displays the welcome text. Should be deleted when creating an
     * actual project.
     */
    @FXML
    private Label welcomeText;

    /**
     * This is where you need to add code that should happen during
     * initialization and then change the java doc comment.
     *
     * @param location  probably not used
     * @param resources probably not used
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //stuff should probably happen here
    }

    /**
     * Called when the button on the gui is clicked. Triggers display of welcome
     * text in the label. <br> Should be deleted when creating an actual
     * project.
     */
    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }


}