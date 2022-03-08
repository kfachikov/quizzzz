package client.scenes;

import client.utils.ServerUtils;
import com.google.inject.Inject;
import commons.QueueUser;
import commons.SingleUser;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Paint;
import javafx.stage.Modality;

public class HomeScreenCtrl {

    private static final int FORBIDDEN = 403;

    private final ServerUtils server;
    private final MainCtrl mainCtrl;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField serverURL;

    @FXML
    private Label invalidServer;

    /**
     * initializes HomeScreenCtrl by connecting it to backend and frontend mainCtrl.
     *
     * @param server   is the server variable
     * @param mainCtrl is the main controller varaiable
     */
    @Inject
    public HomeScreenCtrl(ServerUtils server, MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
        this.server = server;
    }

    /**
     * runs showHelp() function.
     */
    public void helpMe() {
        mainCtrl.showHelp();
    }

    /**
     * adds the user to the server and initialises the title and scene to Prepare.
     */
    public void playSolo() {
        try {
            ServerUtils.setCurrentServer(getServer());
            server.addUser(getSingleUser());
            mainCtrl.showPrep();
        } catch (WebApplicationException e) {
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        } catch (ProcessingException e) {
            serverInvalid();
        }

    }

    /**
     * @return a new SingleUser object that contains its username and score.
     */
    public SingleUser getSingleUser() {
        String user = usernameField.getText();
        return new SingleUser(user, 0);
    }

    /**
     * @return the server address entered in server address field.
     */
    public String getServer() {
        return serverURL.getText();
    }

    /**
     * Sends a POST request to the server, adding the user to the queue,
     * and then switches the scene to the queue.
     *
     * If a player tries to enter multiplayer queue without entering username, or using an already existing one,
     * WebApplicationException is thrown and handled accordingly.
     *
     * If the server entered is not a valid (running) one, then ProcessingException is thrown.
     */
    public void playMulti() {
        try {
            String username = usernameField.getText();
            ServerUtils.setCurrentServer(getServer());
            QueueUser user = server.addQueueUser(new QueueUser(username, -9999));
            mainCtrl.showQueue(user);
        } catch (WebApplicationException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initModality(Modality.APPLICATION_MODAL);

            switch (e.getResponse().getStatus()) {
            case FORBIDDEN: alert.setContentText("Username not unique!");
            break;
            default: alert.setContentText("Username not present!");
            }

            alert.showAndWait();
        } catch (ProcessingException e) {
            serverInvalid();
        }
    }

    /**
     * Reusable method to be executed once an invalid server is entered.
     */
    private void serverInvalid () {
        serverURL.setStyle("-fx-control-inner-background: #" + (Paint.valueOf("f2dede")).toString().substring(2));
        invalidServer.setVisible(true);
    }

}
