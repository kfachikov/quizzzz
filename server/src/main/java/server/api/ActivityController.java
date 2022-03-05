package server.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.database.ActivityRepository;
import server.database.entities.Activity;

import java.util.List;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityRepository repo;

    public ActivityController(ActivityRepository repo) {
        this.repo = repo;
    }

    /**
     * Lists all entries currently present in the repository.
     * @return ResponseEntity consisting of a list of all activities.
     */
    @GetMapping("")
    public ResponseEntity<List<Activity>> getAllActivities() {
        return new ResponseEntity<>(repo.findAll(), HttpStatus.OK);
    }

    /**
     * Add an Activity entry into the ActivityRepository if a valid one is passed.
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
     * @param key Primary-key attribute to search by.
     * @return ResponseEntity consisting of the deleted entry if present, or a Not Found error if not found.
     */
    @DeleteMapping("/key={key}")
    public ResponseEntity<Activity> removeActivity(@PathVariable Long key) {
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
     * @param key Primary-key attribute to search by.
     * @param activityDetails An activity with values as those to be updates as - requested in a APPLICATION_JSON format.
     * @return ResponseEntity consisting of the updated entry if present, or a Not Found error if not found.
     */
    @PutMapping("/{key}")
    public ResponseEntity<Activity> updateActivity(@PathVariable(value = "key") Long key,
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
     * Checks whether a String is null or empty.
     * @param s String to be checked.
     * @return Either true or false depending on whether the argument is a present one.
     */
    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
