package tkit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TodoTest {

    @Test
    void toString_showsTypeAndDescription() {
        Todo todo = new Todo("go sleep");
        String rendered = todo.toString();
        Assertions.assertTrue(rendered.contains("go sleep"));
        Assertions.assertTrue(rendered.startsWith("[T][ ]"));
    }

    @Test
    void markAndUnmark_changesStatusIcon() {
        Todo todo = new Todo("PLEASE SLEEP");
        todo.markAsDone();
        Assertions.assertTrue(todo.toString().contains("[X]"));
        todo.markAsUndone();
        Assertions.assertTrue(todo.toString().contains("[ ]"));
    }
}
