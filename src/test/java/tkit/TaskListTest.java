package tkit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskListTest {

    @Test
    void addAndGet_returnsSameTask() {
        TaskList list = new TaskList();
        Todo t = new Todo("restore sanity");
        list.add(t);
        assertEquals(t, list.get(0));
    }

    @Test
    void find_matchesCaseInsensitiveSubstring() {
        TaskList list = new TaskList();
        list.add(new Todo("Cry hard"));
        list.add(new Todo("mental breakdown"));

        List<Task> hits = list.find("cry");
        assertEquals(1, hits.size());
        assertEquals("Cry hard", ((Todo) hits.get(0)).toString().substring(7));
    }

    @Test
    void removeAt_deletesAndReturnsTask() {
        TaskList list = new TaskList();
        Todo t = new Todo("a");
        list.add(t);
        Task removed = list.removeAt(0);
        assertEquals(t, removed);
        assertTrue(list.isEmpty());
    }
}
