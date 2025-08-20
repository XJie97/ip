import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Tkit {
    public static void main(String[] args) {
        String identity = "not three kids in a trench coat";
        Scanner sc = new Scanner(System.in);

        List<Task> lst = new ArrayList<>();

        System.out.println("____________________ \n\nHello from  " + identity);
        System.out.println("What do you mean we are three kids in a trench coat?");
        System.out.println("We are an adult person! \n____________________\n");

        while (true) {
            String input = sc.nextLine().trim();

            if (input.equals("bye")) {
                System.out.println("____________________\n");
                System.out.println("Goodbye fellow adult!");
                System.out.println("____________________");
                break;
            }

            if (input.equals("list")) {
                System.out.println("____________________\n");
                if (lst.isEmpty()) {
                    System.out.println("No entries yet.");
                } else {
                    System.out.println("Here are the tasks in your list:");
                    for (int i = 0; i < lst.size(); i++) {
                        System.out.println((i + 1) + ". " + lst.get(i));
                    }
                }
                System.out.println("____________________");
                continue;
            }

            if (input.startsWith("mark ")) {
                int idx = parseIndex(input.substring(5), lst.size());
                if (idx >= 0) {
                    Task t = lst.get(idx);
                    t.markAsDone();
                    System.out.println("____________________\n");
                    System.out.println("Nice! I've marked this task as done:");
                    System.out.println("  " + t);
                    System.out.println("____________________");
                }
                continue;
            }

            if (input.startsWith("unmark ")) {
                int idx = parseIndex(input.substring(7), lst.size());
                if (idx >= 0) {
                    Task t = lst.get(idx);
                    t.markAsUndone();
                    System.out.println("____________________\n");
                    System.out.println("OK, I've marked this task as not done yet:");
                    System.out.println("  " + t);
                    System.out.println("____________________");
                }
                continue;
            }

            if (input.startsWith("todo ")) {
                String desc = input.substring(5).trim();
                Task t = new Todo(desc);
                lst.add(t);
                printAdded(t, lst.size());
                continue;
            }

            if (input.startsWith("deadline ")) {
                String body = input.substring(9).trim();
                String[] parts = body.split("\\s*/by\\s*", 2);
                if (parts.length < 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                    printInvalid("Invalid deadline format. Format: deadline <TASK> /by <WHEN>");
                    continue;
                }
                Task t = new Deadline(parts[0].trim(), parts[1].trim());
                lst.add(t);
                printAdded(t, lst.size());
                continue;
            }

            if (input.startsWith("event ")) {
                String body = input.substring(6).trim();
                String[] p1 = body.split("\\s*/from\\s*", 2);
                if (p1.length < 2 || p1[0].isEmpty()) {
                    printInvalid("Invalid event format. Format: event <EVENT> /from <START> /to <END>");
                    continue;
                }
                String[] p2 = p1[1].split("\\s*/to\\s*", 2);
                if (p2.length < 2 || p2[0].isEmpty() || p2[1].isEmpty()) {
                    printInvalid("Invalid event format. Use: event <EVENT> /from <START> /to <END>");
                    continue;
                }
                Task t = new Event(p1[0].trim(), p2[0].trim(), p2[1].trim());
                lst.add(t);
                printAdded(t, lst.size());
                continue;
            }

            System.out.println("____________________\n");
            System.out.println("added: " + input);
            lst.add(new Todo(input));
            System.out.println("____________________");
        }

        sc.close();
    }

    private static void printAdded(Task t, int count) {
        System.out.println("____________________\n");
        System.out.println("Got it. I've added this task:");
        System.out.println("  " + t);
        System.out.println("Now you have " + count + " tasks in the list.");
        System.out.println("____________________");
    }

    private static void printInvalid(String msg) {
        System.out.println("____________________\n");
        System.out.println(msg);
        System.out.println("____________________");
    }

    private static int parseIndex(String s, int size) {
        try {
            int n = Integer.parseInt(s.trim());
            int idx = n - 1;
            if (idx >= 0 && idx < size) return idx;
        } catch (NumberFormatException ignored) { }
        System.out.println("____________________\n");
        System.out.println("Invalid task number.");
        System.out.println("____________________");
        return -1;
    }
}

abstract class Task {
    protected String description;
    protected boolean isDone;

    protected Task(String description) {
        this.description = description;
        this.isDone = false;
    }

    public void markAsDone() {
        this.isDone = true;
    }

    public void markAsUndone() {
        this.isDone = false;
    }

    protected String getStatusIcon() {
        return isDone ? "X" : " ";
    }

    @Override
    public String toString() {
        return "[" + getStatusIcon() + "] " + description;
    }
}

class Todo extends Task {
    public Todo(String description) {
        super(description);
    }

    @Override
    public String toString() {
        return "[T]" + super.toString();
    }
}

class Deadline extends Task {
    protected String by;

    public Deadline(String description, String by) {
        super(description);
        this.by = by;
    }

    @Override
    public String toString() {
        return "[D]" + super.toString() + " (by: " + by + ")";
    }
}

class Event extends Task {
    protected String from;
    protected String to;

    public Event(String description, String from, String to) {
        super(description);
        this.from = from;
        this.to = to;
    }

    @Override
    public String toString() {
        return "[E]" + super.toString() + " (from: " + from + " to: " + to + ")";
    }
}
