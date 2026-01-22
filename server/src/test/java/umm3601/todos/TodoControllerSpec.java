package umm3601.todos;

import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
// import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

// import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
// import io.javalin.json.JavalinJackson;
import umm3601.todo.Todo;
import umm3601.todo.TodoController;


@SuppressWarnings({ "MagicNumber" })
public class TodoControllerSpec {


  // An instance of the controller we're testing that is prepared in
  // `setupEach()`, and then exercised in the various tests below.
  private TodoController TodoController;

  // A Mongo object ID that is initialized in `setupEach()` and used
  // in a few of the tests. It isn't used all that often, though,
  // which suggests that maybe we should extract the tests that
  // care about it into their own spec file?
  private ObjectId samsId;

  // The client and database that will be used
  // for all the tests in this spec file.
  private static MongoClient mongoClient;
  private static MongoDatabase db;

  // Used to translate between JSON and POJOs.
  //private static JavalinJackson javalinJackson = new JavalinJackson();

  @Mock
  private Context ctx;

  @Captor
  private ArgumentCaptor<ArrayList<Todo>> TodoArrayListCaptor;

  @Captor
  private ArgumentCaptor<Todo> TodoCaptor;

  @Captor
  private ArgumentCaptor<Map<String, String>> mapCaptor;

  @BeforeAll
  static void setupAll() {
    String mongoAddr = System.getenv().getOrDefault("MONGO_ADDR", "localhost");

    mongoClient = MongoClients.create(
        MongoClientSettings.builder()
            .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(mongoAddr))))
            .build());
    db = mongoClient.getDatabase("test");
  }

  @AfterAll
  static void teardown() {
    db.drop();
    mongoClient.close();
  }

  @BeforeEach
  void setupEach() throws IOException {
    MockitoAnnotations.openMocks(this);

    MongoCollection<Document> todoDocuments = db.getCollection("todos");
    todoDocuments.drop();
    List<Document> testTodos = new ArrayList<>();
    testTodos.add(
        new Document()
            .append("owner", "Chris")
            .append("status", true)
            .append("body", "cillum commodo amet incididunt anim qui")
            .append("category", "Food"));
    testTodos.add(
        new Document()
            .append("owner", "Lynn")
            .append("status", false)
            .append("body", "cillum commodo amet incididunt anim qui")
            .append("category", "School"));
    testTodos.add(
        new Document()
            .append("owner", "Jack")
            .append("status", true)
            .append("body", "cillum commodo amet incididunt anim qui")
            .append("category", "Work"));

    samsId = new ObjectId();
    Document sam = new Document()
        .append("_id", samsId)
        .append("owner", "Blob")
        .append("status", true)
        .append("body", "cillum commodo amet incididunt anim qui")
        .append("category", "School");

    todoDocuments.insertMany(testTodos);
    todoDocuments.insertOne(sam);

    TodoController = new TodoController(db);
  }

  // @Test
  // void addsRoutes() {
  //   Javalin mockServer = mock(Javalin.class);
  //   TodoController.addRoutes(mockServer);
  //   verify(mockServer, Mockito.atLeast(3)).get(any(), any());
  //   verify(mockServer, Mockito.atLeastOnce()).post(any(), any());
  //   verify(mockServer, Mockito.atLeastOnce()).delete(any(), any());
  // }

  @Test
  void canGetAllTodos() throws IOException {

    when(ctx.queryParamMap()).thenReturn(Collections.emptyMap());

    TodoController.getTodos(ctx);

    verify(ctx).json(TodoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    assertEquals(
        db.getCollection("todos").countDocuments(),
        TodoArrayListCaptor.getValue().size());
  }

}
