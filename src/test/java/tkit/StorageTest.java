package tkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link Storage} using an isolated temp file via reflection
 * to override the internal data path.
 */
class StorageTest {

    @TempDir
    Path tmp;

    private Storage storageAt(Path file) {
        Storage s = new Storage();
        try {
            Field f = Storage.class.getDeclaredField("dataFile");
            f.setAccessible(true);
            f.set(s, file);
        } catch (ReflectiveOperationException e) {
            fail("Failed to redirect Storage dataFile: " + e.getMessage());
        }
        return s;
    }

    @Test
    void loadAndSave_roundTrip_isolatedPath() throws Exception {
        Path data = tmp.resolve("Tkit.txt");
        Storage s = storageAt(data);

        List<Task> first = s.load();
        assertNotNull(first);
        assertTrue(first.isEmpty(), "Fresh temp file should load empty");

        Todo t1 = new Todo("read book");
        Deadline d1 = new Deadline("return book", LocalDateTime.of(2019, 12, 2, 18, 0));
        Event e1 = new Event("project meeting",
                LocalDateTime.of(2019, 12, 2, 14, 0),
                LocalDateTime.of(2019, 12, 2, 16, 0));

        s.save(List.of(t1, d1, e1));
        assertTrue(Files.exists(data));

        List<Task> loaded = s.load();
        assertEquals(3, loaded.size());
        assertTrue(loaded.get(0) instanceof Todo);
        assertTrue(loaded.get(1) instanceof Deadline);
        assertTrue(loaded.get(2) instanceof Event);
        assertTrue(loaded.get(0).toString().contains("read book"));
        assertTrue(loaded.get(1).toString().contains("Dec 2 2019 18:00"));
        assertTrue(loaded.get(2).toString().contains("Dec 2 2019 14:00"));
    }

    @Test
    void load_ignoresCorruptedLines_isolatedPath() throws Exception {
        Path data = tmp.resolve("Tkit.txt");
        Files.createDirectories(data.getParent());
        String content = String.join("\n",
                "# header",
                "T | 1 | fine task",
                "D | 0 | missing-date",
                "E | 0 | meeting | bad | 2019-12-02T16:00",
                "E | 0 | ok | 2019-12-02T14:00 | 2019-12-02T16:00");
        Files.writeString(data, content);

        Storage s = storageAt(data);
        List<Task> loaded = s.load();
        assertEquals(2, loaded.size());
        assertTrue(loaded.get(0) instanceof Todo);
        assertTrue(loaded.get(1) instanceof Event);
    }
}
