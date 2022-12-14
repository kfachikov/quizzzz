package server.api;

import commons.single.SinglePlayerLeaderboardScore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.database.LeaderboardRepository;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/leaderboard/players")
public class LeaderboardController {

    private final LeaderboardRepository repo;

    /**
     * Constructor for the user controller.
     *
     * @param repo User repository final instance
     */
    public LeaderboardController(LeaderboardRepository repo) {
        this.repo = repo;
    }

    /**
     * Get endpoint that retrieves all users from the user repository with score descending.
     *
     * @return the users existing in the repository
     */
    @GetMapping("")
    public List<SinglePlayerLeaderboardScore> getAllUsers() {
        List<SinglePlayerLeaderboardScore> players = repo.findAll();

        //sorting players in ascending score order
        Collections.sort(players, Comparator.comparing(player1 -> player1.getScore()));

        //reversing the order to make it descending
        Collections.reverse(players);

        return players;
    }

    /**
     * @param entry the user to be added to the SingleUser repository
     * @return response
     */
    @PostMapping("")
    public ResponseEntity<SinglePlayerLeaderboardScore> add(@RequestBody SinglePlayerLeaderboardScore entry) {
        if (entry == null || isNullOrEmpty(entry.getUsername())) {
            return ResponseEntity.badRequest().build();
        }
        SinglePlayerLeaderboardScore saved = repo.save(entry);
        return ResponseEntity.ok(saved);
    }

    /**
     * Checks whether a string is empty or not.
     *
     * @param s String variable
     * @return true/false
     */
    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

}
