package umm3601.todo;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.mongojack.JacksonMongoCollection;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import umm3601.Controller;
//import umm3601.user.User;
//import umm3601.user.UserByCompany;

public class TodoController implements Controller {
  private static final String API_TODO = "/api/todos";
  private static final String API_TODO_BY_ID = "/api/todos/{id}";
  static final String OWNER_KEY = "owner";
  static final String STATUS_KEY = "status";
  static final String BODY_KEY = "body";
  static final String CAT_KEY = "category";
  static final String SORT_ORDER_KEY = "sortorder";

  private final JacksonMongoCollection<Todo> todoCollection;

  public TodoController(MongoDatabase database) {
    todoCollection = JacksonMongoCollection.builder().build(
        database,
        "todos",
        Todo.class,
        UuidRepresentation.STANDARD);
  }

  public void getTodo(Context ctx) {
    String id = ctx.pathParam("id");
    Todo todo;

    try {
      todo = todoCollection.find(eq("_id", new ObjectId(id))).first();
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse("The requested user id wasn't a legal Mongo Object ID.");
    }
    if (todo == null) {
      throw new NotFoundResponse("The requested user was not found");
    } else {
      ctx.json(todo);
      ctx.status(HttpStatus.OK);
    }
  }


  public void getUsers(Context ctx) {
    Bson combinedFilter = constructFilter(ctx);
    Bson sortingOrder = constructSortingOrder(ctx);

    ArrayList<Todo> matchingUsers = todoCollection
      .find(combinedFilter)
      .sort(sortingOrder)
      .into(new ArrayList<>());

    ctx.json(matchingUsers);

    ctx.status(HttpStatus.OK);
  }

  private Bson constructFilter(Context ctx) {
    List<Bson> filters = new ArrayList<>(); // start with an empty list of filters

    if (ctx.queryParamMap().containsKey(CAT_KEY)) {
      Pattern pattern = Pattern.compile(Pattern.quote(ctx.queryParam(CAT_KEY)), Pattern.CASE_INSENSITIVE);
      filters.add(regex(CAT_KEY, pattern));
    }

    // Combine the list of filters into a single filtering document.
    Bson combinedFilter = filters.isEmpty() ? new Document() : and(filters);

    return combinedFilter;
  }

  private Bson constructSortingOrder(Context ctx) {
    String sortBy = Objects.requireNonNullElse(ctx.queryParam("sortby"), "name");
    String sortOrder = Objects.requireNonNullElse(ctx.queryParam("sortorder"), "asc");
    Bson sortingOrder = sortOrder.equals("desc") ?  Sorts.descending(sortBy) : Sorts.ascending(sortBy);
    return sortingOrder;
  }


  public void addNewTodo(Context ctx) {

    String body = ctx.body();
    Todo newTodo = ctx.bodyValidator(Todo.class)
      .check(usr -> usr.owner != null && usr.owner.length() > 0,
        "User must have a non-empty user name; body was " + body)
      .check(usr -> usr.body != null && usr.body.length() > 0,
        "User must have a non-empty user name; body was " + body)
      .check(usr -> usr.category != null && usr.category.length() > 0,
        "User must have a non-empty user name; body was " + body)
      .check(usr -> usr.status,
        "User must have a non-empty user name; body was " + body)
      .get();

    // Add the new user to the database
    todoCollection.insertOne(newTodo);

    // Set the JSON response to be the `_id` of the newly created user.
    // This gives the client the opportunity to know the ID of the new user,
    // which it can then use to perform further operations (e.g., a GET request
    // to get and display the details of the new user).
    ctx.json(Map.of("id", newTodo._id));
    // 201 (`HttpStatus.CREATED`) is the HTTP code for when we successfully
    // create a new resource (a user in this case).
    // See, e.g., https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
    // for a description of the various response codes.
    ctx.status(HttpStatus.CREATED);
  }

  /**
   * Delete the user specified by the `id` parameter in the request.
   *
   * @param ctx a Javalin HTTP context
   */
  public void deleteUser(Context ctx) {
    String id = ctx.pathParam("id");
    DeleteResult deleteResult = todoCollection.deleteOne(eq("_id", new ObjectId(id)));
    // We should have deleted 1 or 0 users, depending on whether `id` is a valid user ID.
    if (deleteResult.getDeletedCount() != 1) {
      ctx.status(HttpStatus.NOT_FOUND);
      throw new NotFoundResponse(
        "Was unable to delete ID "
          + id
          + "; perhaps illegal ID or an ID for an item not in the system?");
    }
    ctx.status(HttpStatus.OK);
  }

  /**
   * Utility function to generate an URI that points
   * at a unique avatar image based on a user's email.
   *
   * This uses the service provided by gravatar.com; there
   * are numerous other similar services that one could
   * use if one wished.
   *
   * YOU DON'T NEED TO USE THIS FUNCTION FOR THE TODOS.
   *
   * @param email the email to generate an avatar for
   * @return a URI pointing to an avatar image
   */
  String generateAvatar(String email) {
    String avatar;
    try {
      // generate unique md5 code for identicon
      avatar = "https://gravatar.com/avatar/" + md5(email) + "?d=identicon";
    } catch (NoSuchAlgorithmException ignored) {
      // set to mystery person
      avatar = "https://gravatar.com/avatar/?d=mp";
    }
    return avatar;
  }

  /**
   * Utility function to generate the md5 hash for a given string
   *
   * @param str the string to generate a md5 for
   */
  public String md5(String str) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] hashInBytes = md.digest(str.toLowerCase().getBytes(StandardCharsets.UTF_8));

    StringBuilder result = new StringBuilder();
    for (byte b : hashInBytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }

  /**
   * Sets up routes for the `user` collection endpoints.
   * A UserController instance handles the user endpoints,
   * and the addRoutes method adds the routes to this controller.
   *
   * These endpoints are:
   *   - `GET /api/users/:id`
   *       - Get the specified user
   *   - `GET /api/users?age=NUMBER&company=STRING&name=STRING`
   *      - List users, filtered using query parameters
   *      - `age`, `company`, and `name` are optional query parameters
   *   - `GET /api/usersByCompany`
   *     - Get user names and IDs, possibly filtered, grouped by company
   *   - `DELETE /api/users/:id`
   *      - Delete the specified user
   *   - `POST /api/users`
   *      - Create a new user
   *      - The user info is in the JSON body of the HTTP request
   *
   * GROUPS SHOULD CREATE THEIR OWN CONTROLLERS THAT IMPLEMENT THE
   * `Controller` INTERFACE FOR WHATEVER DATA THEY'RE WORKING WITH.
   * You'll then implement the `addRoutes` method for that controller,
   * which will set up the routes for that data. The `Server#setupRoutes`
   * method will then call `addRoutes` for each controller, which will
   * add the routes for that controller's data.
   *
   * @param server The Javalin server instance
   */
  @Override
  public void addRoutes(Javalin server) {
    // Get the specified user
    server.get(API_TODO_BY_ID, this::getTodo);

    // List users, filtered using query parameters
    server.get(API_TODO, this::getTodo);

    // Add new user with the user info being in the JSON body
    // of the HTTP request
    server.post(API_TODO, this::addNewTodo);

    // Delete the specified user
    server.delete(API_TODO_BY_ID, this::deleteUser);
  }

}
