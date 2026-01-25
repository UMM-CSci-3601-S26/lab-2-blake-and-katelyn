package umm3601.todo;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
// import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.mongojack.JacksonMongoCollection;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
// import com.mongodb.client.result.DeleteResult;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import umm3601.Controller;

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
      throw new BadRequestResponse("The requested todo id wasn't a legal Mongo Object ID.");
    }
    if (todo == null) {
      throw new NotFoundResponse("The requested todo was not found");
    } else {
      ctx.json(todo);
      ctx.status(HttpStatus.OK);
    }
  }


  public void getTodos(Context ctx) {
    // Build filters (status, contains, owner, category)
    Bson filter = constructFilter(ctx);

    // Parse Limit
    Integer limit = parseLimit(ctx);

    // Parse sorting order
    Bson sortingOrder = constructSortingOrder(ctx);

    // Build the MongoDB query
    FindIterable<Todo> results = todoCollection.find(filter);

    // Apply sorting if present
    if (sortingOrder != null) {
      results = results.sort(sortingOrder);
    }

    // Apply limit if present
    if (limit != null) {
      results = results.limit(limit);
    }

    // Materialize results
    ArrayList<Todo> matchingTodos = results.into(new ArrayList<>());

    // Return JSON
    ctx.json(matchingTodos);
    ctx.status(HttpStatus.OK);
  }

  private Integer parseLimit(Context ctx) {
    // If no limit, no limit
    if (!ctx.queryParamMap().containsKey("limit")) {
      return null;
    }

    String limitParam = ctx.queryParam("limit");

    try {
      int limit = Integer.parseInt(limitParam);
      if (limit < 1) {
        throw new BadRequestResponse("The limit must be a positive integer.");
      }
      return limit;
    } catch (NumberFormatException e) {
      throw new BadRequestResponse("The limit must be a number.");
    }

  }

  private Bson constructFilter(Context ctx) {
    List<Bson> filters = new ArrayList<>(); // start with an empty list of filters

    // Owner Filter

    // Category Filter

    // Status Filter

    // Contains Filter
    if (ctx.queryParamMap().containsKey("contains")) {
      Pattern pattern = Pattern.compile(Pattern.quote(ctx.queryParam("contains")));
      filters.add(regex(BODY_KEY, pattern));
    }

    Bson combinedFilter = filters.isEmpty() ? new Document() : and(filters);

    return combinedFilter;
  }

  private Bson constructSortingOrder(Context ctx) {
    String sortBy = Objects.requireNonNullElse(ctx.queryParam("sortby"), "name");
    String sortOrder = Objects.requireNonNullElse(ctx.queryParam("sortorder"), "asc");
    Bson sortingOrder = sortOrder.equals("desc") ?  Sorts.descending(sortBy) : Sorts.ascending(sortBy);
    return sortingOrder;
  }


  // public void addNewTodo(Context ctx) {

  //   String body = ctx.body();
  //   Todo newTodo = ctx.bodyValidator(Todo.class)
  //     .check(td -> td.owner != null && td.owner.length() > 0,
  //       "Todo must have a non-empty owner name; body was " + body)
  //     .check(td -> td.body != null && td.body.length() > 0,
  //       "Todo must have a non-empty body name; body was " + body)
  //     .check(td -> td.category != null && td.category.length() > 0,
  //       "Todo must have a non-empty category name; body was " + body)
  //     .check(td -> td.status, "Todo must have a status.")
  //     .get();

  //   todoCollection.insertOne(newTodo);

  //   // Set the JSON response to be the `_id` of the newly created user.
  //   // This gives the client the opportunity to know the ID of the new user,
  //   // which it can then use to perform further operations (e.g., a GET request
  //   // to get and display the details of the new user).
  //   ctx.json(Map.of("id", newTodo._id));
  //   // 201 (`HttpStatus.CREATED`) is the HTTP code for when we successfully
  //   // create a new resource (a user in this case).
  //   // See, e.g., https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
  //   // for a description of the various response codes.
  //   ctx.status(HttpStatus.CREATED);
  // }


  // public void deleteTodo(Context ctx) {
  //   String id = ctx.pathParam("id");
  //   DeleteResult deleteResult = todoCollection.deleteOne(eq("_id", new ObjectId(id)));

  //   if (deleteResult.getDeletedCount() != 1) {
  //     ctx.status(HttpStatus.NOT_FOUND);
  //     throw new NotFoundResponse(
  //       "Was unable to delete ID "
  //         + id
  //         + "; perhaps illegal ID or an ID for an item not in the system?");
  //   }
  //   ctx.status(HttpStatus.OK);
  // }

  // String generateAvatar(String email) {
  //   String avatar;
  //   try {
  //     // generate unique md5 code for identicon
  //     avatar = "https://gravatar.com/avatar/" + md5(email) + "?d=identicon";
  //   } catch (NoSuchAlgorithmException ignored) {
  //     // set to mystery person
  //     avatar = "https://gravatar.com/avatar/?d=mp";
  //   }
  //   return avatar;
  // }


  public String md5(String str) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] hashInBytes = md.digest(str.toLowerCase().getBytes(StandardCharsets.UTF_8));

    StringBuilder result = new StringBuilder();
    for (byte b : hashInBytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }

  @Override
  public void addRoutes(Javalin server) {

    server.get(API_TODO_BY_ID, this::getTodo);

    server.get(API_TODO, this::getTodos);

    // server.post(API_TODO, this::addNewTodo);

    // server.delete(API_TODO_BY_ID, this::deleteTodo);
  }

}
