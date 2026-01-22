package umm3601.todos;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import umm3601.todo.Todo;

public class TodoSpec {
  private static final String FAKE_ID_STRING_1 = "fakeIdOne";
  private static final String FAKE_ID_STRING_2 = "fakeIdTwo";

  private Todo Todo1;
  private Todo Todo2;

  @BeforeEach
  void setupEach() {
    Todo1 = new Todo();
    Todo2 = new Todo();
  }

  @Test
  void TodosWithEqualIdAreEqual() {
    Todo1._id = FAKE_ID_STRING_1;
    Todo2._id = FAKE_ID_STRING_1;

    assertTrue(Todo1.equals(Todo2));
  }

  @Test
  void TodosWithDifferentIdAreNotEqual() {
    Todo1._id = FAKE_ID_STRING_1;
    Todo2._id = FAKE_ID_STRING_2;

    assertFalse(Todo1.equals(Todo2));
  }

  @Test
  void hashCodesAreBasedOnId() {
    Todo1._id = FAKE_ID_STRING_1;
    Todo2._id = FAKE_ID_STRING_1;

    assertTrue(Todo1.hashCode() == Todo2.hashCode());
  }

  @SuppressWarnings("unlikely-arg-type")
  @Test
  void TodosAreNotEqualToOtherKindsOfThings() {
    Todo1._id = FAKE_ID_STRING_1;
    // a Todo is not equal to its id even though id is used for checking equality
    assertFalse(Todo1.equals(FAKE_ID_STRING_1));
  }
}
