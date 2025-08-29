package tkit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TodoTest {

    @Test
    void toString_showsTypeAndDescription() {
        Todo todo = new Todo("go sleep");
        String rendered = todo.toString();
        assertTrue(rendered.contains("go sleep"));
        assertTrue(rendered.startsWith("[T][ ]"));
    }

    @Test
    void markAndUnmark_changesStatusIcon() {
        Todo todo = new Todo("PLEASE SLEEP");
        todo.markAsDone();
        assertTrue(todo.toString().contains("[X]"));
        todo.markAsUndone();
        assertTrue(todo.toString().contains("[ ]"));
    }
}

