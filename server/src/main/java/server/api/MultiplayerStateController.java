package server.api;

import commons.multi.MultiPlayer;
import commons.multi.MultiPlayerState;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.utils.MultiPlayerStateUtils;


/**
 * Controller responsible for handling the client requests regarding any multiplayer game.
 */
@ComponentScan(basePackageClasses = MultiPlayerStateUtils.class)
@RestController
@RequestMapping("/api/multi")
public class MultiplayerStateController {

    private final MultiPlayerStateUtils multiUtils;

    /**
     * Constructor for multiplayer state controller.
     *
     * @param multiUtils Multiplayer state utilities.
     */
    public MultiplayerStateController(MultiPlayerStateUtils multiUtils) {
        this.multiUtils = multiUtils;
    }

    /**
     * GET mapping for multiplayer game state.
     * <p>
     * Internally, the state of the game will be updated to be complete up to date.
     * This means that is might switch to another state (e.g. TRANSITION to QUESTION),
     * and it might change the players' scores.
     * <p>
     * Expectation is that this endpoint will be called at least every 500 ms.
     *
     * @param id Id for the multiplayer game.
     * @return Up to date MultiPlayerState
     */
    @GetMapping("/{id}")
    public ResponseEntity<MultiPlayerState> getGameState(@PathVariable("id") long id) {
        if (id < 0) {
            return ResponseEntity.badRequest().build();
        } else {
            MultiPlayerState game = multiUtils.getGameState(id);
            if (game == null) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.ok(game);
            }
        }
    }

    /**
     * POST mapping for a player in a multiplayer game.
     * <p>
     * Adds the player to the game with the given id.
     *
     * @param id     Id of the multiplayer game.
     * @param player MultiPlayer that is added.
     * @return MultiPlayer that was added
     */
    @PostMapping("/players/{id}")
    public ResponseEntity<MultiPlayer> addMultiPlayer(@PathVariable("id") long id, @RequestBody MultiPlayer player) {
        if (id < 0) {
            return ResponseEntity.badRequest().build();
        } else {
            MultiPlayer multiPlayer = multiUtils.addPlayer(id, player);
            if (multiPlayer == null) {
                return ResponseEntity.badRequest().build();
            } else {
                return ResponseEntity.ok(multiPlayer);
            }
        }
    }
}