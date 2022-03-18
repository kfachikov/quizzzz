package commons;

import java.util.List;
import java.util.Objects;

public class SinglePlayerState extends GameState {

    SinglePlayer player;

    /**
     * Constructor for the state of the solo game.
     * @param timeLeft the time left during a round.
     * @param roundNumber the round number of the game.
     * @param questionList the list of question for a game.
     * @param submittedAnswers the answers submitted by players during game.
     * @param activityList the list of activities used for the game.
     * @param state the status of the game.
     * @param player the player that is currently in the game.
     */
    public SinglePlayerState(double timeLeft, int roundNumber, List<AbstractQuestion> questionList, List<String> submittedAnswers, List<Activity> activityList, String state, SinglePlayer player) {
        super(timeLeft, roundNumber, questionList, submittedAnswers, activityList, state);
        this.player = player;
    }

    /**
     * Getter for the player.
     * @return a SinglePlayer representing the player that is currently in the game.
     */
    public SinglePlayer getPlayer() {
        return player;
    }

    /**
     * Setter for the player.
     * @param player the player that is currently in the game.
     */
    public void setPlayer(SinglePlayer player) {
        this.player = player;
    }

    /**
     * Compares the two entities.
     * @param o the instance that is compared to.
     * @return true if the two entities are equal, otherwise it will be returned false.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        SinglePlayerState that = (SinglePlayerState) o;
        return Objects.equals(player, that.player);
    }

    /**
     * Computes the object's hash code.
     * @return the object's hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), player);
    }

}
