package server.api;

import commons.misc.Activity;
import commons.misc.ActivityImage;
import commons.misc.ActivityImageMessage;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.database.ActivityImageRepository;
import server.database.ActivityRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Server-side controller for the activities stored in the database.
 */
@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityRepository repo;
    private final ActivityImageRepository imageRepo;

    /**
     * Constructor for the activity controller.
     *
     * @param repo      ActivityRepository instance.
     * @param imageRepo ActivityImageRepository instance.
     */
    public ActivityController(
            ActivityRepository repo,
            ActivityImageRepository imageRepo
    ) {
        this.repo = repo;
        this.imageRepo = imageRepo;
    }

    /**
     * POST mapping for adding an image to an activity.
     *
     * @param key     Key of the activity in the repository
     * @param message Base64 encoding of the image message
     * @return ResponseEntity indicating whether activity image was added.
     */
    @PostMapping("/images/{key}")
    public ResponseEntity<ActivityImageMessage> addActivityImage(
            @PathVariable("key") long key,
            @RequestBody ActivityImageMessage message) {
        Optional<Activity> optionalActivity = repo.findById(key);
        if (optionalActivity.isEmpty()) {
            return ResponseEntity.notFound().build();
        } else {
            String imageBase64 = message.getImageBase64();
            Activity activity = optionalActivity.get();
            byte[] decodedImage = Base64.decodeBase64(imageBase64);
            String id = activity.getId();
            ActivityImage activityImage = new ActivityImage(id, decodedImage);
            imageRepo.save(activityImage);
            System.out.println("Added image for " + activity.getId() + " activity");
            return ResponseEntity.ok(message);
        }
    }

    /**
     * GET mapping for retrieving the image of an activity.
     *
     * @param key Key of the activity in the repository.
     * @return Base64 encoding of the image message
     */
    @GetMapping("/images/{key}")
    public ResponseEntity<ActivityImageMessage> getActivityImage(@PathVariable("key") long key) {
        Optional<Activity> optionalActivity = repo.findById(key);
        if (optionalActivity.isEmpty()) {
            return ResponseEntity.badRequest().build();
        } else {
            Activity activity = optionalActivity.get();
            String id = activity.getId();
            Optional<ActivityImage> optionalActivityImage = imageRepo.findById(id);
            if (optionalActivityImage.isEmpty()) {
                return ResponseEntity.notFound().build();
            } else {
                ActivityImage activityImage = optionalActivityImage.get();
                String imageBase64 = Base64.encodeBase64String(activityImage.getImage());
                ActivityImageMessage message = new ActivityImageMessage(imageBase64, key);
                return ResponseEntity.ok(message);
            }
        }
    }

    /**
     * Lists all entries currently present in the repository.
     *
     * @return ResponseEntity consisting of a list of all activities.
     */
    @GetMapping("")
    public ResponseEntity<List<Activity>> getAllActivities() {
        return new ResponseEntity<>(repo.findAll(), HttpStatus.OK);
    }

    /**
     * Add an Activity entry into the ActivityRepository if a valid one is passed.
     *
     * @param activity An activity to be added - requested in a APPLICATION_JSON format.
     * @return Either a Bad Request or OK, depending on whether all fields of the entry are specified.
     */
    @PostMapping("")
    public ResponseEntity<Activity> addActivity(@RequestBody Activity activity) {
        if (activity == null ||
                isNullOrEmpty(activity.getId()) ||
                isNullOrEmpty(activity.getTitle()) ||
                isNullOrEmpty(activity.getSource()) ||
                isNullOrEmpty(activity.getImage()) ||
                activity.getConsumption() < 0) {
            return ResponseEntity.badRequest().build();
        }
        Activity saved = repo.save(activity);
        return ResponseEntity.ok(saved);
    }

    /**
     * Delete an activity (only if present) from the repository.
     *
     * @param key Primary-key attribute to search by.
     * @return ResponseEntity consisting of the deleted entry if present, or a Not Found error if not found.
     */
    @DeleteMapping("/delete/{key}")
    public ResponseEntity<Activity> removeActivity(@PathVariable("key") Long key) {
        Activity removed = repo.findById(key).orElse(null);
        if (removed != null) {
            repo.delete(removed);
            return ResponseEntity.ok(removed);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update an activity (only if present) in the repository.
     *
     * @param key             Primary-key attribute to search by.
     * @param activityDetails An activity with values as those to be updates as - requested in a APPLICATION_JSON format.
     * @return ResponseEntity consisting of the updated entry if present, or a Not Found error if not found.
     */
    @PutMapping("/{key}")
    public ResponseEntity<Activity> updateActivity(@PathVariable("key") Long key,
                                                   @RequestBody Activity activityDetails) {
        Activity activity = repo.findById(key).orElse(null);
        if (activity != null) {
            activity.setId(activityDetails.getId());
            activity.setTitle(activityDetails.getTitle());
            activity.setSource(activityDetails.getSource());
            activity.setImage(activityDetails.getImage());
            activity.setConsumption(activityDetails.getConsumption());

            if (isNullOrEmpty(activity.getId()) ||
                    isNullOrEmpty(activity.getTitle()) ||
                    isNullOrEmpty(activity.getSource()) ||
                    isNullOrEmpty(activity.getImage()) ||
                    activity.getConsumption() < 0) {
                return ResponseEntity.badRequest().build();
            }

            final Activity updatedActivity = repo.save(activity);
            return ResponseEntity.ok(updatedActivity);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Save the list of activities provided by the user to the ActivityRepository.
     * Corresponds to importActivities() in client/ServerUtils and is used in Administrator Panel.
     *
     * @param activities in a list, which is generated by JPA from the input file of user in Administrator Panel.
     *                   An activity must fulfill the criteria in lines 108 through 114 in order to be saved.
     *                   If there are no activities saved to the repository, BAD_REQUEST is sent.
     * @return ResponseEntity consisting of the list of activities that is saved to the ActivityRepository.
     */
    @PostMapping("/addToRepo")
    public ResponseEntity<List<Activity>> addActivities(@RequestBody List<Activity> activities) {
        ArrayList<Activity> savedActivities = new ArrayList<>();
        for (Activity activity : activities) {
            if (!isNullOrEmpty(activity.getId()) &&
                    !isNullOrEmpty(activity.getTitle()) &&
                    !isNullOrEmpty(activity.getSource()) &&
                    !isNullOrEmpty(activity.getImage()) &&
                    activity.getConsumption() > 0 &&
                    activity.getSource().length() < 240 &&
                    activity.getConsumption() < (long) Integer.MAX_VALUE
            ) {
                savedActivities.add(repo.save(activity));
            }
        }
        if (savedActivities.size() == 0) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(savedActivities);
    }

    /**
     * Checks whether a String is null or empty.
     *
     * @param s String to be checked.
     * @return Either true or false depending on whether the argument is a present one.
     */
    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }


}
