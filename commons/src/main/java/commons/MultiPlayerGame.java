package commons;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class MultiPlayerGame {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    public Long gameId;

    @OneToMany
    public List<AbstractQuestion> questions = new ArrayList<>();

    public MultiPlayerGame(Long gameId, List<AbstractQuestion> questions) {
        this.gameId = gameId;
        this.questions = questions;
    }

    public List<AbstractQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<AbstractQuestion> questions) {
        this.questions = questions;
    }

    public Long getId() {
        return gameId;
    }

    public void setId(Long id) {
        this.gameId = id;
    }
}
