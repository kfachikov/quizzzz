package client.scenes.multi;

import client.scenes.misc.MainCtrl;
import client.scenes.multi.question.*;
import client.services.MultiplayerGameStatePollingService;
import client.utils.ActivityImageUtils;
import client.utils.ServerUtils;
import commons.misc.Activity;
import commons.misc.GameResponse;
import commons.multi.MultiPlayer;
import commons.multi.MultiPlayerState;
import commons.multi.Reaction;
import commons.question.*;
import commons.queue.QueueUser;
import jakarta.ws.rs.WebApplicationException;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import javafx.util.Pair;

import javax.inject.Inject;
import java.util.*;

/**
 * Class responsible for managing the multiplayer game for the client.
 * <p>
 * It keeps track of any logical changes on the server side, and updates the scenes and their controllers accordingly.
 */
public class MultiplayerCtrl {

    private final int forbidden = 403;
    private final int reactionsQuestion = 3;
    private final int reactionsLeaderboard = 6;

    private MultiGameConsumptionQuestionScreenCtrl consumptionQuestionScreenCtrl;
    private Scene consumptionQuestionScreen;

    private MultiGameGuessQuestionScreenCtrl guessQuestionScreenCtrl;
    private Scene guessQuestionScreen;

    private MultiGameInsteadQuestionScreenCtrl insteadQuestionScreenCtrl;
    private Scene insteadQuestionScreen;

    private MultiGameMoreExpensiveQuestionScreenCtrl moreExpensiveQuestionScreenCtrl;
    private Scene moreExpensiveQuestionScreen;

    private MultiGameMockScreenCtrl mockScreenCtrl;
    private Scene mockScreen;

    private LeaderboardScreenCtrl leaderboardCtrl;
    private Scene leaderboard;

    /*
    Abstract class instance. To be used for the answer-revealing process.
     */
    private MultiQuestionScreen currentScreenCtrl;


    /*
    Field to be used for the answer-correctness check. Even if a player clicks on one and
    the same answer, the field variable wouldn't change.
     */
    private String lastSubmittedAnswer;

    /*
    TimeLine instance to handle the visual effects of the progress bar used to "time" the rounds.
     */
    private Timeline timeline;

    private final MainCtrl mainCtrl;
    private final ServerUtils serverUtils;
    private final MultiplayerGameStatePollingService pollingService;
    private final ActivityImageUtils activityImageUtils;

    private long gameId;
    private String username;
    private boolean doublePoints;

    private Image surprised;
    private Image laughing;
    private Image angry;
    private Image crying;

    private final ChangeListener<MultiPlayerState> onPoll = (observable, oldValue, newValue) -> {
        // If state has changed, we probably have to switch scenes
        if (newValue != null && (oldValue == null || !newValue.getState().equals(oldValue.getState()))) {
            switchState(newValue);
        }
        // If state has changed, perhaps some new Reactions have been "registered".
        if (newValue != null) {
            List<Reaction> reactions = newValue.getReactionList();
            if (newValue.getState().equals(MultiPlayerState.QUESTION_STATE)) {
                updateReactionQuestion(reactions);
            } else if (newValue.getState().equals(MultiPlayerState.LEADERBOARD_STATE) ||
                newValue.getState().equals(MultiPlayerState.GAME_OVER_STATE)) {
                updateReactionLeaderboard(reactions);
            }
        }
    };

    /**
     * Constructor for Multiplayer controller.
     *
     * @param mainCtrl       Main controller
     * @param serverUtils    Server utilities
     * @param pollingService Multiplayer game state polling service
     * @param activityImageUtils    Activity Image utility
     */
    @Inject
    public MultiplayerCtrl(MainCtrl mainCtrl,
                           ServerUtils serverUtils,
                           MultiplayerGameStatePollingService pollingService,
                           ActivityImageUtils activityImageUtils
    ) {
        this.mainCtrl = mainCtrl;
        this.serverUtils = serverUtils;
        this.pollingService = pollingService;
        this.activityImageUtils = activityImageUtils;
    }

    /**
     * Initialization method for multiplayer controller.
     * <p>
     * Sets internal controller and scene references.
     * Sets up the associated polling service.
     * <p>
     * This method is called when application is starting up, similarly to MainCtrl:initialize.
     *
     * @param consumptionQuestionScreen     Mock question A screen pair
     * @param guessQuestionScreen           Mock question B screen pair
     * @param insteadQuestionScreen         Mock question C screen pair
     * @param moreExpensiveQuestionScreen   Mock question D screen pair
     * @param mockMulti                     Mock miscellaneous screen pair
     * @param leaderboard                   Final/intermediate leaderboard screen pair
     */
    public void initialize(
            Pair<MultiGameConsumptionQuestionScreenCtrl, Parent> consumptionQuestionScreen,
            Pair<MultiGameGuessQuestionScreenCtrl, Parent> guessQuestionScreen,
            Pair<MultiGameInsteadQuestionScreenCtrl, Parent> insteadQuestionScreen,
            Pair<MultiGameMoreExpensiveQuestionScreenCtrl, Parent> moreExpensiveQuestionScreen,
            Pair<MultiGameMockScreenCtrl, Parent> mockMulti,
            Pair<LeaderboardScreenCtrl, Parent> leaderboard) {
        this.consumptionQuestionScreenCtrl = consumptionQuestionScreen.getKey();
        this.consumptionQuestionScreen = new Scene(consumptionQuestionScreen.getValue());

        this.guessQuestionScreenCtrl = guessQuestionScreen.getKey();
        this.guessQuestionScreen = new Scene(guessQuestionScreen.getValue());

        this.insteadQuestionScreenCtrl = insteadQuestionScreen.getKey();
        this.insteadQuestionScreen = new Scene(insteadQuestionScreen.getValue());

        this.moreExpensiveQuestionScreenCtrl = moreExpensiveQuestionScreen.getKey();
        this.moreExpensiveQuestionScreen = new Scene(moreExpensiveQuestionScreen.getValue());

        this.mockScreenCtrl = mockMulti.getKey();
        this.mockScreen = new Scene(mockMulti.getValue());

        this.leaderboardCtrl = leaderboard.getKey();
        this.leaderboard = new Scene(leaderboard.getValue());

        pollingService.valueProperty().addListener(onPoll);

        setStylesheets();
        initializeImages();
    }

    /**
     * sets the stylesheets.
     */
    public void setStylesheets() {
        String CSSPath = "styling/GameStyle.css";

        consumptionQuestionScreen.getStylesheets().add(CSSPath);
        guessQuestionScreen.getStylesheets().add(CSSPath);
        insteadQuestionScreen.getStylesheets().add(CSSPath);
        moreExpensiveQuestionScreen.getStylesheets().add(CSSPath);
        mockScreen.getStylesheets().add(CSSPath);
        leaderboard.getStylesheets().add(CSSPath);
    }

    /**
     * Start a multiplayer session.
     * <p>
     * Will automatically switch scenes to multiplayer question screens, leaderboard etc.
     * <p>
     * Can be stopped using `stop()`
     *
     * @param gameId   Id of the multiplayer game
     * @param username Name of the player in the game
     */
    public void start(long gameId, String username) {
        this.gameId = gameId;
        this.username = username;

        serverUtils.addMultiPlayer(gameId, new MultiPlayer(username, 0, true, true, true));
        pollingService.start(gameId);

        insteadQuestionScreenCtrl.setJokers();
        guessQuestionScreenCtrl.setJokers();
        moreExpensiveQuestionScreenCtrl.setJokers();
        consumptionQuestionScreenCtrl.setJokers();

        switchState(pollingService.poll());
        mainCtrl.setOnCloseRequest(this::promptLeave);
    }

    /**
     * Stop the multiplayer session.
     * <p>
     * Resets the controller to a state where another multiplayer game can be played later.
     */
    public void stop() {
        pollingService.stop();
    }

    /**
     * Confirms if the user really wants to leave the game and allows them to
     * return to the home screen.
     *
     * @return whether the user selected "yes".
     */
    public boolean promptLeave() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Leave the game");
        alert.setContentText("Are you sure you want to leave the game?");
        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);

        alert.getButtonTypes().setAll(yesButton, noButton);

        Optional<ButtonType> confirmation = alert.showAndWait();
        if (confirmation.isPresent() && confirmation.get() == yesButton) {
            stop();
            mainCtrl.showHome();
            return true;
        }
        return false;
    }

    /**
     * Switch state of the game.
     * <p>
     * Switches to appropriate scene/animation based on the new state of the game.
     * <p>
     * This method is called whenever a transition happens on the serverside.
     *
     * @param game Up-to-date state of the game.
     */
    private void switchState(MultiPlayerState game) {
        switch (game.getState()) {
            case MultiPlayerState.QUESTION_STATE:
                switchToQuestion(game);
                break;
            case MultiPlayerState.TRANSITION_STATE:
                revealAnswerCorrectness(game);
                break;
            case MultiPlayerState.LEADERBOARD_STATE:
                showIntermediateGameOver(sortList(game), game.getState());
                break;
            case MultiPlayerState.GAME_OVER_STATE:
                showIntermediateGameOver(sortList(game), game.getState());
                break;
            default:
                switchToMock(game);
                break;
        }
    }

    /**
     * Updates the background of the current scene according to the correctness of the answer given.
     *
     * @param game  Multiplayer game state - to be used for answer comparison clieny-side.
     *              Used for changing the background during the transition phase.
     */
    public void revealAnswerCorrectness(MultiPlayerState game) {
        currentScreenCtrl.setScore(game.getPlayerByUsername(username).getScore());
        if (game.compareAnswerClient(lastSubmittedAnswer)) {
            currentScreenCtrl.getWindow()
                    .setStyle("-fx-background-color: #" + (Paint.valueOf("aedd94")).toString().substring(2));
        } else {
            currentScreenCtrl.getWindow()
                    .setStyle("-fx-background-color: #" + (Paint.valueOf("ff8a84")).toString().substring(2));
        }
        /*
        The correct answer is revealed for each type of question.
        */
        if (currentScreenCtrl instanceof MultiGameConsumptionQuestionScreenCtrl) {
            ((MultiGameConsumptionQuestionScreenCtrl) currentScreenCtrl).showCorrectAnswer();
        }
        if (currentScreenCtrl instanceof MultiGameInsteadQuestionScreenCtrl) {
            ((MultiGameInsteadQuestionScreenCtrl) currentScreenCtrl).showCorrectAnswer();
        }
        if (currentScreenCtrl instanceof MultiGameMoreExpensiveQuestionScreenCtrl) {
            ((MultiGameMoreExpensiveQuestionScreenCtrl) currentScreenCtrl).showCorrectAnswer();
        }
        if (currentScreenCtrl instanceof MultiGameGuessQuestionScreenCtrl) {
            ((MultiGameGuessQuestionScreenCtrl) currentScreenCtrl).setMessageCorrectAnswer((GuessQuestion)
                    game.getQuestionList().get(game.getRoundNumber()));
        }
    }

    /**
     * Used to change the color of the background of the current question scene to the initial blue color.
     * Also, updates the score the player have accumulated so far.
     *
     * @param game  Multiplayer game state to fetch the client's score from.
     */
    private void setDefault(MultiPlayerState game) {
        currentScreenCtrl.getWindow()
                .setStyle("-fx-background-color: #" + (Paint.valueOf("00236f")).toString().substring(2));
        currentScreenCtrl.setScore(game.getPlayerByUsername(username).getScore());
    }

    /**
     * Switch to a mock scene.
     *
     * @param game Current state of the game.
     */
    private void switchToMock(MultiPlayerState game) {
        mockScreenCtrl.setGameStateLabelText(game.toString());
        mainCtrl.getPrimaryStage().setScene(mockScreen);
    }

    /**
     * Switch a question scene.
     * <p>
     * Uses the current round number of the game and its question list to
     * determine the current question, then calls the appropriate method
     * to switch to the relevant question scene.
     *
     * @param game Current state of the game.
     */
    private void switchToQuestion(MultiPlayerState game) {
        int roundNumber = game.getRoundNumber();
        if (roundNumber < 0 || roundNumber >= 20) {
            System.err.println("Tried to switch to a question scene with invalid round number");
            return;
        }

        /*
        Setting the lastSubmittedAnswer field to something "default", so no errors occur if
        it is compared with the actual one, but the client hadn't submitted anything
        (and thus, not called `submitAnswer` method),
         */
        lastSubmittedAnswer = "";

        disableUsedRevealJoker();
        disableUsedDoubleJokers();
        disableUsedTimeJoker();

        AbstractQuestion question = game.getQuestionList().get(roundNumber);

        if (question instanceof ConsumptionQuestion) {
            ConsumptionQuestion consumptionQuestion = (ConsumptionQuestion) question;
            showConsumptionQuestion(game, consumptionQuestion);
        } else if (question instanceof GuessQuestion) {
            GuessQuestion guessQuestion = (GuessQuestion) question;
            showGuessQuestion(game, guessQuestion);
        } else if (question instanceof InsteadQuestion) {
            InsteadQuestion insteadQuestion = (InsteadQuestion) question;
            showInsteadQuestion(game, insteadQuestion);
        } else if (question instanceof MoreExpensiveQuestion) {
            MoreExpensiveQuestion moreExpensiveQuestion = (MoreExpensiveQuestion) question;
            showMoreExpensiveQuestion(game, moreExpensiveQuestion);
        }
    }

    /**
     * Show "Consumption" question screen.
     *
     * @param game      Multiplayer game to call setDefault with.
     * @param question "Consumption" question.
     */
    private void showConsumptionQuestion(MultiPlayerState game, ConsumptionQuestion question) {
        currentScreenCtrl = consumptionQuestionScreenCtrl;
        setDefault(game);
        consumptionQuestionScreenCtrl.setJokersStyle();
        consumptionQuestionScreenCtrl.setQuestion(question);
        consumptionQuestionScreenCtrl.getGameStateLabel().setText("Game ID: " + game.getId());
        consumptionQuestionScreenCtrl.prepareAnswerButton();
        consumptionQuestionScreenCtrl.setAnswers(question);
        consumptionQuestionScreenCtrl.setDescription(question);
        consumptionQuestionScreenCtrl.setImage(getActivityImage(question.getActivity()));
        centerImage(consumptionQuestionScreenCtrl.getImage());
        mainCtrl.getPrimaryStage().setScene(consumptionQuestionScreen);
        startTimer(game, consumptionQuestionScreenCtrl);
    }

    /**
     * Show "Guess" question screen.
     *
     * @param game      Multiplayer game to call setDefault with.
     * @param question "Guess" question.
     */
    private void showGuessQuestion(MultiPlayerState game, GuessQuestion question) {
        currentScreenCtrl = guessQuestionScreenCtrl;
        setDefault(game);
        guessQuestionScreenCtrl.setJokersStyle();
        guessQuestionScreenCtrl.getGameStateLabel().setText("Game ID: " + game.getId());
        guessQuestionScreenCtrl.inputFieldDefault();
        guessQuestionScreenCtrl.setDescription(question);
        guessQuestionScreenCtrl.setImage(getActivityImage(question.getActivity()));
        centerImage(guessQuestionScreenCtrl.getImage());
        mainCtrl.getPrimaryStage().setScene(guessQuestionScreen);
        startTimer(game, guessQuestionScreenCtrl);
    }

    /**
     * Show "Instead of" question screen.
     *
     * @param game      Multiplayer game to call setDefault with.
     * @param question "Instead of" question.
     */
    private void showInsteadQuestion(MultiPlayerState game, InsteadQuestion question) {
        currentScreenCtrl = insteadQuestionScreenCtrl;
        setDefault(game);
        insteadQuestionScreenCtrl.setJokersStyle();
        insteadQuestionScreenCtrl.setQuestion(question);
        insteadQuestionScreenCtrl.getGameStateLabel().setText("Game ID: " + game.getId());
        insteadQuestionScreenCtrl.prepareAnswerButton();
        insteadQuestionScreenCtrl.setDescription(question);
        insteadQuestionScreenCtrl.setAnswers(question);
        insteadQuestionScreenCtrl.setImage(getActivityImage(question.getActivity()));
        centerImage(insteadQuestionScreenCtrl.getImage());
        mainCtrl.getPrimaryStage().setScene(insteadQuestionScreen);
        startTimer(game, insteadQuestionScreenCtrl);

    }

    /**
     * Show "More Expensive" question screen.
     *
     * @param game      Multiplayer game to call setDefault with.
     * @param question "More Expensive" question.
     */
    private void showMoreExpensiveQuestion(MultiPlayerState game, MoreExpensiveQuestion question) {
        currentScreenCtrl = moreExpensiveQuestionScreenCtrl;
        setDefault(game);
        moreExpensiveQuestionScreenCtrl.setJokersStyle();
        moreExpensiveQuestionScreenCtrl.setQuestion(question);
        moreExpensiveQuestionScreenCtrl.prepareAnswerButton();
        moreExpensiveQuestionScreenCtrl.getGameStateLabel().setText("Game ID: " + game.getId());
        moreExpensiveQuestionScreenCtrl.setAnswerDescriptions(question);
        moreExpensiveQuestionScreenCtrl.setAnswerImages(getActivityImage(question.getAnswerChoices().get(0)),
                getActivityImage(question.getAnswerChoices().get(1)),
                getActivityImage(question.getAnswerChoices().get(2)));
        mainCtrl.getPrimaryStage().setScene(moreExpensiveQuestionScreen);
        startTimer(game, moreExpensiveQuestionScreenCtrl);
    }

    /**
     * Sets the scene as leaderboard/gameOver screen.
     * Calls LeaderBoardScreenCtrl.setScene() and passes
     * the sorted list of players and the state of the game, either LEADERBOARD or GAME_OVER
     *
     * @param players   the list of players in the game, in descending score order.
     * @param gameState the state of the game, is either LEADERBOARD or GAME_OVER.
     */
    public void showIntermediateGameOver(List<MultiPlayer> players, String gameState) {
        leaderboardCtrl.setScene(players, gameState);
        mainCtrl.getPrimaryStage().setScene(leaderboard);
    }

    /**
     * Sends a string to the server sa a chosen answer from the player.
     * The last two symbols from the string should be removed, as they
     * denote the "Wh" in the button text field.
     *
     * @param chosenAnswer String value of button clicked - answer chosen
     */
    public void submitAnswer(String chosenAnswer) {
        lastSubmittedAnswer = chosenAnswer.substring(0, chosenAnswer.length() - 2);
        serverUtils.postAnswerMultiplayer(new GameResponse(
                gameId,
                new Date().getTime(),
                (int) getRoundNumber(serverUtils.getMultiGameState(gameId)),
                username,
                lastSubmittedAnswer,
                doublePoints
        ));

        setDoublePoints(false);
    }

    /**
     * Setter for the doublePoints joker.
     *
     * @param doublePoints the boolean value for the joker regarding the points.
     */
    public void setDoublePoints(boolean doublePoints) {
        this.doublePoints = doublePoints;
    }

    /**
     * Returns to home screen.
     * Is assigned to the Return Home button in Game Over screen.
     */
    public void returnHome() {
        mainCtrl.showHome();
    }

    /**
     * Sorts the list of players in the game in descending score order.
     *
     * @param game The ongoing game, from which the <strong>unsorted</strong> List is retrieved
     * @return the retrieved List players, <strong>sorted</strong>.
     */
    public List<MultiPlayer> sortList(MultiPlayerState game) {
        List<MultiPlayer> players = game.getPlayers();
        Collections.sort(players, Comparator.comparing(player1 -> player1.getScore()));
        Collections.reverse(players);
        return players;
    }

    /**
     * Getter for the user username.
     *
     * @return the username of the user that joined the queue.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Getter for the game id.
     *
     * @return the id of the game.
     */
    public long getId() {
        return gameId;
    }

    /**
     * Getter for the game id.
     *
     * @return the id of the game.
     * @param game is a MultiPlayerState
     */
    public long getRoundNumber(MultiPlayerState game) {
        return game.getRoundNumber();
    }

    /**
     * Getter method for getting the image of an activity.
     *
     * @param activity Activity to get an image from.
     * @return JavaFX image of the activity.
     */
    public Image getActivityImage(Activity activity) {
        long key = activity.getKey();
        return activityImageUtils.getActivityImage(key);
    }

    /**
     * Centers an image inside any imageView passed as argument.
     *
     * @param imageView ImageView instance, which image should be centered.
     */
    public void centerImage(ImageView imageView) {
        Image img = imageView.getImage();
        if (img != null) {
            double ratioX = imageView.getFitWidth() / img.getWidth();
            double ratioY = imageView.getFitHeight() / img.getHeight();

            double reductionCoef = Math.min(ratioX, ratioY);

            double w = img.getWidth() * reductionCoef;
            double h = img.getHeight() * reductionCoef;

            imageView.setX((imageView.getFitWidth() - w) / 2);
            imageView.setY((imageView.getFitHeight() - h) / 2);
        }
    }

    /**
     * Initializes a new instance of TimeLine and starts it.
     * Used at the beginning of each "scene-showing" process.
     *
     * @param game                  Multiplayer game state instance to work with - needed for
     *                              next phase "fetching". The timer would go from now till
     *                              the next phase (Date).
     * @param multiQuestionScreen   Controller for the corresponding scene to be visualized.
     */
    private void startTimer(MultiPlayerState game, MultiQuestionScreen multiQuestionScreen) {
        ProgressBar time = multiQuestionScreen.getTime();
        time.setStyle("-fx-accent: #006e8c");

        long nextPhase = game.getNextPhase();
        long roundTime = nextPhase - new Date().getTime();

        timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(time.progressProperty(), 0)),
                new KeyFrame(Duration.millis(roundTime * 7 / 10), e -> {
                    time.setStyle("-fx-accent: red");
                }),
                new KeyFrame(Duration.millis(nextPhase - new Date().getTime()), e -> {
                    multiQuestionScreen.disableAnswerSubmission();
                }, new KeyValue(time.progressProperty(), 1))
        );
        timeline.play();
    }

    /**
     * Method for re-entering a queue. Bound with "Play Again" button on final
     * leaderboard.
     *
     * In case a player with such username already exists, the player is redirected to
     * the home screen.
     *
     * TODO Alert of non-unique username.
     */
    public void enterNewQueue() {
        try {
            QueueUser user = serverUtils.addQueueUser(new QueueUser(username));
            mainCtrl.showQueue(user, serverUtils.getCurrentServer());
        } catch (WebApplicationException e) {
            switch (e.getResponse().getStatus()) {
                case forbidden:
                    mainCtrl.showHome();
                    mainCtrl.getHomeCtrl().playMulti();
                    break;
            }
        }
    }



    /**
     * Disables the used jokers for all controllers.
     */
    public void disableUsedDoubleJokers() {
        if (consumptionQuestionScreenCtrl.getDoublePoints() ||
                moreExpensiveQuestionScreenCtrl.getDoublePoints() ||
                guessQuestionScreenCtrl.getDoublePoints() ||
                insteadQuestionScreenCtrl.getDoublePoints()) {
            consumptionQuestionScreenCtrl.disableDoublePoint();
            guessQuestionScreenCtrl.disableDoublePoint();
            moreExpensiveQuestionScreenCtrl.disableDoublePoint();
            insteadQuestionScreenCtrl.disableDoublePoint();
        }
    }

    /**
     * Disables the used jokers for all controllers.
     */
    public void disableUsedRevealJoker() {
        if (consumptionQuestionScreenCtrl.getReveal() ||
                moreExpensiveQuestionScreenCtrl.getReveal() ||
                guessQuestionScreenCtrl.getReveal() ||
                insteadQuestionScreenCtrl.getReveal()) {
            consumptionQuestionScreenCtrl.disableReveal();
            guessQuestionScreenCtrl.disableReveal();
            moreExpensiveQuestionScreenCtrl.disableReveal();
            insteadQuestionScreenCtrl.disableReveal();
        }
    }

    /**
     * Disables the used jokers for all controllers.
     */
    public void disableUsedTimeJoker() {
        if (consumptionQuestionScreenCtrl.getHalfTime() ||
                insteadQuestionScreenCtrl.getHalfTime() ||
                guessQuestionScreenCtrl.getHalfTime() ||
                moreExpensiveQuestionScreenCtrl.getHalfTime()) {
            consumptionQuestionScreenCtrl.disableShortenTime();
            guessQuestionScreenCtrl.disableShortenTime();
            moreExpensiveQuestionScreenCtrl.disableShortenTime();
            insteadQuestionScreenCtrl.disableShortenTime();
        }
    }


    /**
     * Initialized the actions happening once an emoji button is clicked.
     *
     * @param button1    Button to be bound with particular action.
     * @param button2    Button to be bound with particular action.
     * @param button3    Button to be bound with particular action.
     * @param button4    Button to be bound with particular action.
     */
    public void initializeEmojiButtons(Button button1, Button button2, Button button3, Button button4) {
        button1.setOnAction(e -> {
            postReaction("surprised");
        });
        button2.setOnAction(e -> {
            postReaction("laughing");
        });
        button3.setOnAction(e -> {
            postReaction("angry");
        });
        button4.setOnAction(e -> {
            postReaction("crying");
        });
    }

    /**
     * Posts a reaction object to the server.
     *
     * @param emoji     Emoji String to be used for "defining" the particular
     *                  emoji submitted.
     */
    private void postReaction(String emoji) {
        serverUtils.addReaction(gameId,
                new Reaction(username, emoji));
    }

    /**
     * Method to be called when a change in the reactions is registered during QUESTION_STATE.
     *
     * @param reactionList  List of Reaction instances to be used for the "chat".
     */
    private void updateReactionQuestion(List<Reaction> reactionList) {
        List<Node> reactionParts = currentScreenCtrl.getReactions().getChildren();
        updateReaction(reactionList, reactionParts, reactionsQuestion);
    }

    /**
     * Method to be called when a change in the reactions is registered during QUESTION_STATE.
     *
     * @param reactionList  List of Reaction instances to be used for the "chat".
     */
    private void updateReactionLeaderboard(List<Reaction> reactionList) {
        List<Node> reactionParts = leaderboardCtrl.getReactions().getChildren();
        updateReaction(reactionList, reactionParts, reactionsLeaderboard);
    }

    /**
     * In case a change occur in the game state, which is constantly being pulled,
     * the reaction section is being updated.
     *
     * @param reactionList      List of Reaction instances to be used for the "chat".
     * @param reactionParts     List of Node instances. Correspond to the particular list
     *                          of nodes of the desired screen.
     * @param reactionsNumber   The number of reactions to be shown. To be different between
     *                          question and leaderboard screen.
     */
    private void updateReaction(List<Reaction> reactionList, List<Node> reactionParts, int reactionsNumber) {
        ArrayList<Reaction> reactions = new ArrayList<>(reactionList);
        Collections.reverse(reactions);

        /*
        In the GridPane `reaction`, Labels and ImageViews are taking turns.
        Thus, the Labels would have even indices within the children of the pane.
        The ImageViews would have odd indices.
         */
        int currentReactionLabelIndex = 0;
        int currentReactionImageIndex = 1;
        for (Reaction reaction: reactions) {
            Label currentReactionLabel = (Label) reactionParts.get(currentReactionLabelIndex);
            ImageView currentReactionImage = (ImageView) reactionParts.get(currentReactionImageIndex);
            currentReactionLabel.setText(reaction.getUsername() + " reacts with ");
            switch (reaction.getEmoji()) {
                case "angry" -> currentReactionImage.setImage(angry);
                case "crying" -> currentReactionImage.setImage(crying);
                case "laughing" -> currentReactionImage.setImage(laughing);
                case "surprised" -> currentReactionImage.setImage(surprised);
            }

            currentReactionLabel.setVisible(true);
            currentReactionImage.setVisible(true);

            currentReactionLabelIndex = currentReactionLabelIndex + 2;
            currentReactionImageIndex = currentReactionImageIndex + 2;
            if (currentReactionLabelIndex >= 2 * reactionsNumber) {
                break;
            }
        }

        /*
        In case the total number of reactions is less than 3 - the size of our "chat",
        the later "lines", consisting of username and emoji submitted are made not visible.
         */
        for (int i = currentReactionLabelIndex; i < 2 * reactionsNumber; i++) {
            Node currentNode = reactionParts.get(i);
            currentNode.setVisible(false);
        }
    }

    /**
     * Initializes all image fields in the MultiplayerCtrl class.
     */
    private void initializeImages() {
        surprised = new Image(
                String.valueOf(this.getClass().getClassLoader().getResource("emoji/Surprised.png")));
        laughing = new Image(
                String.valueOf(this.getClass().getClassLoader().getResource("emoji/Laughing.png")));
        angry = new Image(
                String.valueOf(this.getClass().getClassLoader().getResource("emoji/Angry.png")));
        crying = new Image(
                String.valueOf(this.getClass().getClassLoader().getResource("emoji/Crying.png")));
    }
}
