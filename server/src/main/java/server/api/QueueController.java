package server.api;


import commons.QueueState;
import commons.QueueUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.database.QueueUserRepository;

import java.util.Date;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    private final QueueUserRepository repo;

    private boolean gameStarting;
    private long startTimeInMs;
    private long upcomingGameId;

    public QueueController(QueueUserRepository repo) {
        this.repo = repo;
    }

    /**
     * Get the current state of the queue.
     * The state of the queue contains the following items:
     * - List of users in the queue
     *
     * @return the current state of the queue
     */
    @GetMapping("")
    public ResponseEntity<QueueState> getQueueState() {
        return ResponseEntity.ok(getCurrentQueue());
    }

    private QueueState getCurrentQueue() {
        return new QueueState(repo.findAll(), gameStarting, startTimeInMs - new Date().getTime(), upcomingGameId);
    }

    /**
     * First if handles the case when the username entered is empty.
     * Second one corresponds to username already in database case (not unique per multiplayer queue).
     *
     * @param user the user to be added to the QueueUser repository
     * @return response
     */
    @PostMapping("")
    public ResponseEntity<QueueUser> add(@RequestBody QueueUser user) {
        if (user == null || isNullOrEmpty(user.getUsername())) {
            return ResponseEntity.badRequest().build();
        } else if (repo.existsQueueUserByUsername(user.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        QueueUser saved = repo.save(user);
        gameStarting = false;
        return ResponseEntity.ok(saved);
    }

    /**
     * Starts the multiplayer game with the users inside the queue.
     * <p>
     * The start of the game is set to be 3 seconds after this endpoint is called.
     *
     * @return The updated state of the game.
     */
    @PostMapping("/start")
    public ResponseEntity<QueueState> startGame() {
        if (gameStarting) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } else {
            gameStarting = true;
            startTimeInMs = new Date().getTime() + 3000;
            return ResponseEntity.ok(getCurrentQueue());
        }
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }


    /**
     * Delete a user if present from the repository.
     *
     * @param id Primary-key attribute to search with
     * @return returns a ResponseEntity consisting of the deleted user if present or a Not Found error if not found.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<QueueUser> deleteUser(@PathVariable("id") long id) {
        QueueUser removed = repo.findById(id).orElse(null);
        if (removed != null) {
            repo.delete(removed);
            gameStarting = false;
            return ResponseEntity.ok(removed);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

}
