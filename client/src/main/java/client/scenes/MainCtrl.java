/*
 * Copyright 2021 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package client.scenes;

import commons.MultiUser;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Pair;

public class MainCtrl {

    private Stage primaryStage;

    private HomeScreenCtrl homeCtrl;
    private Scene home;

    private PrepScreenCtrl prepCtrl;
    private Scene prep;

    private HelpScreenCtrl helpCtrl;
    private Scene help;
    private QueueScreenCtrl queueCtrl;
    private Scene queue;

    /**
     * @param primaryStage is the Stage representing the initial stage variable.
     * @param home         is the home screen pair variable
     * @param help         is the help screen pair variable
     * @param prep         is the prepare screen pair variable
     */
    public void initialize(Stage primaryStage,
                           Pair<HomeScreenCtrl, Parent> home,
                           Pair<HelpScreenCtrl, Parent> help,
                           Pair<PrepScreenCtrl, Parent> prep,
                           Pair<QueueScreenCtrl, Parent> queue
    ) {

        this.primaryStage = primaryStage;

        this.homeCtrl = home.getKey();
        this.home = new Scene(home.getValue());
        this.prepCtrl = prep.getKey();
        this.prep = new Scene(prep.getValue());
        this.helpCtrl = help.getKey();
        this.help = new Scene(help.getValue());
        this.queueCtrl = queue.getKey();
        this.queue = new Scene(queue.getValue());

        showHome();
        primaryStage.show();
    }

    /**
     * sets the title and the scene as home.
     */
    public void showHome() {
        primaryStage.setTitle("Quizzz: Home");
        primaryStage.setScene(home);
    }

    /**
     * sets the title and the scene as prep.
     */
    public void showPrep() {
        primaryStage.setTitle("Quizzz: Prepare");
        primaryStage.setScene(prep);
    }

    /**
     * sets the title and the scene as help.
     */
    public void showHelp() {
        primaryStage.setTitle("Quizzz: Help");
        primaryStage.setScene(help);
    }

    /**
     * Sets the current scene to the queue screen, starts the queue polling
     * service and initializes the queue scene controller with
     * the MultiUser instance of the person joining the queue.
     *
     * @param user MultiUser which is joining the queue
     */
    public void showQueue(MultiUser user) {
        primaryStage.setTitle("Quizzz: Queue");
        primaryStage.setScene(queue);
        queueCtrl.getPollingService().start();
        queueCtrl.setUser(user);
    }
}
