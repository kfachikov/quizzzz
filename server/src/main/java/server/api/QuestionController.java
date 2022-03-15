package server.api;

import commons.AbstractQuestion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.database.QuestionRepository;
import server.utils.GenerateQuestionUtils;

import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final GenerateQuestionUtils randomUtils;
    private final QuestionRepository repo;

    public QuestionController(GenerateQuestionUtils randomUtils, QuestionRepository repo) {
        this.repo = repo;
        this.randomUtils = randomUtils;
    }

    /**
     * @param id the question id
     * @return response
     */
    @GetMapping("/id/{id}")
    public ResponseEntity<AbstractQuestion> getById(@PathVariable("id") long id) {
        if (id < 0 || !repo.existsById(id)) {
            return ResponseEntity.badRequest().build();
        } else {
            return ResponseEntity.ok(repo.getById(id));
        }
    }

    /**
     * @param gameId the game id
     * @param questionNo the question number
     * @return response
     */
    @GetMapping("/game/{gameId}/{questionNo}")
    public ResponseEntity<AbstractQuestion> getByGameRound(
            @PathVariable("gameId") long gameId,
            @PathVariable("questionNo") long questionNo
    ) {
        if (gameId < 0 || questionNo < 0 || questionNo >= 20) {
            return ResponseEntity.badRequest().build();
        } else if (repo.count() < 20) {
            return ResponseEntity.notFound().build();
        } else {
            Random random = new Random(gameId);
            List<Long> questionIds =
                    randomUtils.distinctList(random, 20, repo.count());
            long questionId = questionIds.get((int) questionNo);
            return ResponseEntity.ok(repo.getById(questionId));
        }
    }
}
