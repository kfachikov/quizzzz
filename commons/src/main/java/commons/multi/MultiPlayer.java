package commons.multi;


import commons.misc.Player;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * The MultiPlayer class is used to represent the current player of the multiplayer game.
 */
public class MultiPlayer extends Player {

    private boolean timeJoker;
    private boolean incorrectAnswerJoker;
    private boolean pointsDoubledJoker;

    public MultiPlayer() {
        super();
    }

    /**
     * Constructor for the player who is playing multiplayer game.
     *
     * @param username             the player's username.
     * @param score                the player's score.
     * @param timeJoker            the joker for reducing the time
     * @param incorrectAnswerJoker the joker for eliminating an incorrect answer
     * @param pointsDoubledJoker   the joker for doubling the points.
     */
    public MultiPlayer(String username, int score, boolean timeJoker,
                       boolean incorrectAnswerJoker, boolean pointsDoubledJoker) {
        super(username, score);
        this.timeJoker = timeJoker;
        this.incorrectAnswerJoker = incorrectAnswerJoker;
        this.pointsDoubledJoker = pointsDoubledJoker;
    }

    /**
     * getter for the time joker.
     *
     * @return timeJoker
     */
    public boolean getTimeJoker() {
        return timeJoker;
    }

    /**
     * getter for the IncorrectAnswerJoker.
     *
     * @return IncorrectAnswerJoker
     */
    public boolean getIncorrectAnswerJoker() {
        return incorrectAnswerJoker;
    }

    /**
     * getter for the PointsDoubledJoker.
     *
     * @return PointsDoubledJoker
     */
    public boolean getPointsDoubledJoker() {
        return pointsDoubledJoker;
    }

    /**
     * setter for the TimeJoker.
     *
     * @param timeJoker the new TimeJoker
     */
    public void setTimeJoker(boolean timeJoker) {
        this.timeJoker = timeJoker;
    }

    /**
     * setter for the IncorrectAnswerJoker.
     *
     * @param incorrectAnswerJoker the new IncorrectAnswerJoker
     */
    public void setIncorrectAnswerJoker(boolean incorrectAnswerJoker) {
        this.incorrectAnswerJoker = incorrectAnswerJoker;
    }

    /**
     * setter for the PointsDoubledJoker.
     *
     * @param pointsDoubledJoker the new PointsDoubledJoker
     */
    public void setPointsDoubledJoker(boolean pointsDoubledJoker) {
        this.pointsDoubledJoker = pointsDoubledJoker;
    }

    /**
     * Checks wheter two instances are equal.
     *
     * @param o the object that needs to be checked for equality
     * @return true/false
     */
    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    /**
     * Generates hashcode for the instance.
     *
     * @return hashcode for the instance
     */
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}

