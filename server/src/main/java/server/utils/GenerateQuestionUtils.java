package server.utils;

import commons.misc.Activity;
import commons.question.*;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import server.database.ActivityRepository;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
@ComponentScan(basePackageClasses = Random.class)
public class GenerateQuestionUtils {

    private final ActivityRepository repo;
    private final Random random;

    private int activityIndex;
    private List<Long> activityIds;


    public GenerateQuestionUtils(ActivityRepository repo, Random random) {
        this.repo = repo;
        this.random = random;
        this.activityIds = new ArrayList<>();
    }

    /**
     * Initialize GenerateQuestionUtils so it can generate unique activities.
     */
    public void initialize() {
        this.activityIds =
                repo.findAll().stream()
                        .mapToLong(Activity::getKey)
                        .boxed()
                        .collect(Collectors.toList());
        Collections.shuffle(activityIds, random);
        this.activityIndex = 0;
    }

    /**
     * Gets the next random unique activity from the list of activities.
     *
     * @return Next unique random activity.
     */
    public Activity getNextActivity() {
        long activityId = activityIds.get(activityIndex);
        Optional<Activity> optionalActivity = repo.findById(activityId);

        if (optionalActivity.isEmpty()) {
            throw new IllegalStateException("Activity IDs stored non-existent activity.");
        }
        Activity activity = optionalActivity.get();

        this.activityIndex = (this.activityIndex + 1) % activityIds.size();

        return activity;
    }

    /**
     * Utility function for constructing a predicate which can filter activities
     * that fall inside the given range.
     *
     * @param lower Lower bound of the range.
     * @param upper Upper bound of the range.
     * @return Predicate which returns true iff activity falls inside the range.
     */
    public Predicate<Activity> activityPredicate(long lower, long upper) {
        return activity -> activity.getConsumption() >= lower &&
                activity.getConsumption() <= upper;
    }

    /**
     * Find all activities within the given range.
     * <p>
     * The range is specified by its center, and the multiplier.
     * <p>
     * For example, if the center is 500 and multiplier is 2,
     * then the range will be between 250 and 1000.
     * <p>
     * Also, if fewer than 4 activities are found, the range is increased until enough
     * activities are found.
     *
     * @param center     Center of the range.
     * @param multiplier Multiplier to multiply/divide the center by to get the range.
     * @return All activities within multiple of the center.
     */
    public List<Activity> activitiesWithinRange(long center, long multiplier) {
        List<Activity> activities = repo.findAll();
        List<Activity> result = new ArrayList<>();


        // Reduce multiplier before loop, since it increases in each iteration of the loop.
        multiplier--;
        while (result.size() < 4) {
            multiplier++;
            result = activities.stream()
                    .filter(activityPredicate(center / multiplier,
                            center * multiplier))
                    .collect(Collectors.toList());
        }

        return result;
    }

    /**
     * Compare two activities by their consumption.
     *
     * @param a Activity to be compared against.
     * @param b Activity to compare.
     * @return compareTo result of activity consumptions.
     */
    public int compareActivities(Activity a, Activity b) {
        return a.getConsumption().compareTo(b.getConsumption());
    }

    public ConsumptionQuestion generateConsumptionQuestion(boolean difficult) {
        // Generate a random activity
        Activity activity = getNextActivity();

        // Make range of possible values smaller if question is "difficult"
        // Thus, players require more "precision" to answer correctly
        long multiplier = difficult ? 10 : 100;

        long center = activity.getConsumption();

        /*
        Get a list of candidate answers by:
        1. Taking all activities
        2. Filtering only those within the range for good answer choices
        3. Remove the true answer from the list
         */
        List<Long> candidateAnswers =
                activitiesWithinRange(center, multiplier).stream()
                        .mapToLong(Activity::getConsumption)
                        .distinct()
                        .filter(answer -> answer != center)
                        .boxed()
                        .collect(Collectors.toList());

        Collections.shuffle(candidateAnswers, random);

        List<Long> answerChoices = new ArrayList<>();
        // Add correct answer
        answerChoices.add(activity.getConsumption());
        // Add incorrect answers
        answerChoices.add(candidateAnswers.get(0));
        answerChoices.add(candidateAnswers.get(1));

        return new ConsumptionQuestion(activity, answerChoices);
    }

    public MoreExpensiveQuestion generateMoreExpensiveQuestion(boolean difficult) {
        // Pick a random center
        long center = getNextActivity().getConsumption();

        long multiplier = difficult ? 10 : 100;

        List<Activity> answerChoices = activitiesWithinRange(center, multiplier);
        // Limit to 3 answers
        Collections.shuffle(answerChoices, random);
        answerChoices = answerChoices.stream().limit(3).collect(Collectors.toList());

        Activity correct = answerChoices.stream().max(this::compareActivities).get();
        String correctAnswer = correct.getTitle();

        return new MoreExpensiveQuestion(correctAnswer, answerChoices);
    }

    public GuessQuestion generateGuessQuestion(boolean difficult) {
        Activity activity = getNextActivity();

        List<Activity> activities = repo.findAll().stream()
                .sorted(this::compareActivities)
                .collect(Collectors.toList());

        long median = activities.get(activities.size() / 2).getConsumption();

        // Activities with very high consumptions tend to be very difficult to guess
        // Thus, we restrict it to be below median
        while (!difficult && activity.getConsumption() > median) {
            activity = getNextActivity();
        }

        return new GuessQuestion(activity);
    }

    public InsteadQuestion generateInsteadQuestion(boolean difficult) {
        Activity activity = getNextActivity();

        long center = activity.getConsumption();
        long multiplier = difficult ? 10 : 100;

        // We look for the closest match
        List<Activity> correctAnswers = activitiesWithinRange(center, 1);
        // Much larger range
        List<Activity> incorrectAnswers = activitiesWithinRange(center, multiplier);
        // Make sure correct answers are not included in the incorrect answers
        incorrectAnswers.removeAll(correctAnswers);

        Collections.shuffle(correctAnswers, random);
        Collections.shuffle(incorrectAnswers, random);

        List<Activity> answerChoices = new ArrayList<>();
        answerChoices.add(correctAnswers.get(0));
        answerChoices.add(incorrectAnswers.get(0));
        answerChoices.add(incorrectAnswers.get(1));

        return new InsteadQuestion(activity, activity.getTitle(), answerChoices);
    }

    public AbstractQuestion generateQuestion(boolean difficult, int number) {
        return switch (number % 4) {
            case 0 -> generateInsteadQuestion(difficult);
            case 1 -> generateConsumptionQuestion(difficult);
            case 2 -> generateMoreExpensiveQuestion(difficult);
            case 3 -> generateGuessQuestion(difficult);
            default -> null;
        };
    }

    public List<AbstractQuestion> generate20Questions() {
        initialize();

        List<AbstractQuestion> easy = new ArrayList<>();
        List<AbstractQuestion> hard = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            if (i < 12) {
                easy.add(generateQuestion(false, i));
            } else {
                hard.add(generateQuestion(true, i));
            }
        }

        Collections.shuffle(easy, random);
        Collections.shuffle(hard, random);

        List<AbstractQuestion> result = new ArrayList<>();
        result.addAll(easy);
        result.addAll(hard);

        return result;
    }
}
