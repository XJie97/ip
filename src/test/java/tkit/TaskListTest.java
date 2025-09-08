package tkit;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TaskListTest {

    @Test
    void addAndGet_returnsSameTask() {
        TaskList list = new TaskList();
        Todo t = new Todo("restore sanity");
        list.add(t);
        Assertions.assertEquals(t, list.get(0));
    }

    @Test
    void find_matchesCaseInsensitiveSubstring() {
        TaskList list = new TaskList();
        list.add(new Todo("Cry hard"));
        list.add(new Todo("mental breakdown"));

        List<Task> hits = list.find("cry");
        Assertions.assertEquals(1, hits.size());
        Assertions.assertEquals("Cry hard", ((Todo) hits.get(0)).toString().substring(7));
    }

    @Test
    void removeAt_deletesAndReturnsTask() {
        TaskList list = new TaskList();
        Todo t = new Todo("a");
        list.add(t);
        Task removed = list.removeAt(0);
        Assertions.assertEquals(t, removed);
        Assertions.assertTrue(list.isEmpty());
    }
}
