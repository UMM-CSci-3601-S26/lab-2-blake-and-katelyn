package umm3601.todos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.argThat;
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

import io.javalin.http.BadRequestResponse;
// import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
// import io.javalin.json.JavalinJackson;
import umm3601.todo.Todo;
import umm3601.todo.TodoController;


@SuppressWarnings({ "MagicNumber" })
public class TodoControllerSpec {

  private TodoController todoController;

  private ObjectId samsId;


  private static MongoClient mongoClient;
  private static MongoDatabase db;

  // Used to translate between JSON and POJOs.
  //private static JavalinJackson javalinJackson = new JavalinJackson();

  @Mock
  private Context ctx;

  @Captor
  private ArgumentCaptor<ArrayList<Todo>> todoArrayListCaptor;

  @Captor
  private ArgumentCaptor<Todo> todoCaptor;

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
        .append("owner", "Sam")
        .append("status", true)
        .append("body", "cillum commodo amet incididunt anim qui")
        .append("category", "School");

    todoDocuments.insertMany(testTodos);
    todoDocuments.insertOne(sam);

    todoController = new TodoController(db);
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

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    assertEquals(
        db.getCollection("todos").countDocuments(),
        todoArrayListCaptor.getValue().size());
  }

    @Test
  void getTodoWithExistentId() throws IOException {
    String id = samsId.toHexString();
    when(ctx.pathParam("id")).thenReturn(id);

    todoController.getTodo(ctx);

    verify(ctx).json(todoCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals("Sam", todoCaptor.getValue().owner);
    assertEquals(samsId.toHexString(), todoCaptor.getValue()._id);
  }

  @Test
  void getTodoWithBadId() throws IOException {
    when(ctx.pathParam("id")).thenReturn("bad");

    Throwable exception = assertThrows(BadRequestResponse.class, () -> {
      todoController.getTodo(ctx);
    });

    assertEquals("The requested todo id wasn't a legal Mongo Object ID.", exception.getMessage());
  }

  @Test
  void getTodoWithNonexistentId() throws IOException {
    String id = "588935f5c668650dc77df581";
    when(ctx.pathParam("id")).thenReturn(id);

    Throwable exception = assertThrows(NotFoundResponse.class, () -> {
      todoController.getTodo(ctx);
    });

    assertEquals("The requested todo was not found", exception.getMessage());
  }


  @Test
  void getTodosWithValidLimit() throws IOException {
    when(ctx.queryParamMap()).thenReturn(Map.of("limit", List.of("2")));
    when(ctx.queryParam("limit")).thenReturn("2");

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    assertEquals(2, todoArrayListCaptor.getValue().size());
  }

  @Test
  void getTodosLargeLimitReturnsAllTodos() throws IOException {
    when(ctx.queryParamMap()).thenReturn(Map.of("limit", List.of("100")));
    when(ctx.queryParam("limit")).thenReturn("100");

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    assertEquals(4, todoArrayListCaptor.getValue().size());
  }

  @Test
  void getTodosWithNonNumericLimitThrowsError() throws IOException {
    when(ctx.queryParamMap()).thenReturn(Map.of("limit", List.of("abc")));
    when(ctx.queryParam("limit")).thenReturn("abc");

    BadRequestResponse exception = assertThrows(
      BadRequestResponse.class,
      () -> todoController.getTodos(ctx));

    assertEquals("The limit must be a number.", exception.getMessage());
  }

  @Test
  void getTodosWithNegativeLimitThrowsError() throws IOException {
    when(ctx.queryParamMap()).thenReturn(Map.of("limit", List.of("-5")));
    when(ctx.queryParam("limit")).thenReturn("-5");

    BadRequestResponse exception = assertThrows(
      BadRequestResponse.class,
      () -> todoController.getTodos(ctx));

    assertEquals("The limit must be a positive integer.", exception.getMessage());
  }

  @Test
  void getTodosWithZeroLimitThrowsError() throws IOException {
    when(ctx.queryParamMap()).thenReturn(Map.of("limit", List.of("0")));
    when(ctx.queryParam("limit")).thenReturn("0");

    BadRequestResponse exception = assertThrows(
      BadRequestResponse.class,
      () -> todoController.getTodos(ctx));

    assertEquals("The limit must be a positive integer.", exception.getMessage());
  }
@Test
  void getTodosWithStatusComplete() throws IOException {
    when(ctx.queryParamMap()).thenReturn(Map.of("status", List.of("complete")));
    when(ctx.queryParam("status")).thenReturn("complete");

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    assertEquals(3, todoArrayListCaptor.getValue().size());
  }

  @Test
  void getTodosWithStatusIncomplete() throws IOException {
    when(ctx.queryParamMap()).thenReturn(Map.of("status", List.of("incomplete")));
    when(ctx.queryParam("status")).thenReturn("incomplete");

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    assertEquals(1, todoArrayListCaptor.getValue().size());
  }

  @Test
  void getTodosWithInvalidStatusThrowsError() throws IOException {
    when(ctx.queryParamMap()).thenReturn(Map.of("status", List.of("done")));
    when(ctx.queryParam("status")).thenReturn("done");

    BadRequestResponse exception = assertThrows(
      BadRequestResponse.class,
      () -> todoController.getTodos(ctx));

    assertEquals("Status must be 'complete' or 'incomplete'.", exception.getMessage());
  }
  
  @Test
  void getTodoWithOwner() throws IOException {
    when(ctx.queryParamMap()).thenReturn(Map.of("owner", List.of("Jack")));
    when(ctx.queryParam("owner")).thenReturn("Jack");

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    assertEquals(1, todoArrayListCaptor.getValue().size());
  }

}

