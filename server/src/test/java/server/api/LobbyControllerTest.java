package server.api;

import commons.MultiUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

class LobbyControllerTest {

    private MockMultiUserRepository repo;
    private LobbyController userCtrl;

    private int nextId;

    @BeforeEach
    public void setup() {
        repo = new MockMultiUserRepository();
        userCtrl = new LobbyController(repo);
        nextId = 0;
    }

    private List<MultiUser> addMockUsers() {
        List<MultiUser> mockUser = new ArrayList<>();
        for (long i = 0; i < 3; i++) {
            mockUser.add(
                    new MultiUser("p" + nextId, 0)
            );
            mockUser.get((int) i).id = nextId++;
        }
        repo.multiUsers.addAll(mockUser);
        return mockUser;
    }

    @Test
    public void testGetAllUsers() {
        var expected = addMockUsers();
        var result = userCtrl.getAllUsers();
        assertEquals(expected, result);
    }

    @Test
    public void testMethodCall() {
        addMockUsers();
        userCtrl.getAllUsers();
        assertEquals(List.of("findAll"), repo.calledMethods);
    }

    @Test
    public void databaseIsUsed() {
        userCtrl.add(getUser("username"));
        assertEquals(List.of("save"), repo.calledMethods);
    }

    @Test
    public void cannotAddNullPlayer() {
        var actual = userCtrl.add(getUser(null));
        assertEquals(BAD_REQUEST, actual.getStatusCode());
    }



    private static MultiUser getUser(String username) {
        return new MultiUser(username, 0);
    }

    @Test
    public void testDelete() {
        MultiUser user = new MultiUser("ok" + 0, 0);
        MultiUser input = user;
        assertEquals(user, userCtrl.deleteUser(0).getBody());
    }
}