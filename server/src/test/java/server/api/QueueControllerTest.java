package server.api;

import commons.QueueState;
import commons.QueueUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

class QueueControllerTest {

    private QueueUserRepository repo;
    private QueueController lobbyCtrl;

    private int nextId;

    @BeforeEach
    public void setup() {
        repo = new QueueUserRepository();
        lobbyCtrl = new QueueController(repo);
        nextId = 0;
    }

    private List<QueueUser> mockUsers() {
        List<QueueUser> mockUser = new ArrayList<>();
        for (long i = 0; i < 3; i++) {
            mockUser.add(
                    new QueueUser("p" + nextId)
            );
            mockUser.get((int) i).id = nextId++;
        }
        repo.queueUsers.addAll(mockUser);
        return mockUser;
    }

    private static QueueUser getUser(String username) {
        return new QueueUser(username);
    }

    @Test
    public void testGetQueueState() {
        var expected = new QueueState(mockUsers());
        var response = lobbyCtrl.getQueueState();
        var result = response.getBody();
        assertEquals(expected, result);
    }

    @Test
    public void testMethodCall() {
        mockUsers();
        lobbyCtrl.getQueueState();
        assertEquals(List.of("findAll"), repo.calledMethods);
    }

    @Test
    public void databaseIsUsed() {
        lobbyCtrl.add(getUser("username"));
        assertEquals(List.of("save"), repo.calledMethods);
    }

    @Test
    public void cannotAddNullPlayer() {
        var actual = lobbyCtrl.add(getUser(null));
        assertEquals(BAD_REQUEST, actual.getStatusCode());
    }

    @Test
    public void testNotUniqueUsername() {
        mockUsers();
        var actual = lobbyCtrl.add(getUser("p0"));
        assertEquals(FORBIDDEN, actual.getStatusCode());
    }

    @Test
    public void testBadRequest() {
        var response = lobbyCtrl.deleteUser(1);
        assertEquals(BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testNotFound() {
        QueueUser user = new QueueUser("ok" + -1);
        repo.save(user);
        var response = lobbyCtrl.deleteUser(-1);
        assertEquals(BAD_REQUEST, response.getStatusCode());
    }
}