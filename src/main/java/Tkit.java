import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class Task {
    protected String description;
    protected boolean isDone;

    public Task(String description) {
        this.description = description;
        this.isDone = false;
    }

    public void markAsDone() {
        this.isDone = true;
    }

    public void markAsUndone() {
        this.isDone = false;
    }

    public String getStatusIcon() {
        return isDone ? "X" : " ";
    }

    @Override
    public String toString() {
        return "[" + getStatusIcon() + "] " + description;
    }
}

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
                System.out.println("____________________________________________________________\n");
                System.out.println("Goodbye fellow adult!");
                System.out.println("____________________________________________________________");
                break;
            }

            if (input.equals("list")) {
                System.out.println("____________________________________________________________\n");
                if (lst.isEmpty()) {
                    System.out.println("No entries yet.");
                } else {
                    System.out.println("Here are the tasks in your list:");
                    for (int i = 0; i < lst.size(); i++) {
                        System.out.println((i + 1) + ". " + lst.get(i));
                    }
                }
                System.out.println("____________________________________________________________");
                continue;
            }

            if (input.startsWith("mark ")) {
                int idx = parseIndex(input.substring(5), lst.size());
                if (idx >= 0) {
                    Task t = lst.get(idx);
                    t.markAsDone();
                    System.out.println("____________________________________________________________\n");
                    System.out.println("Nice! I've marked this task as done:");
                    System.out.println("  " + t);
                    System.out.println("____________________________________________________________");
                }
                continue;
            }

            if (input.startsWith("unmark ")) {
                int idx = parseIndex(input.substring(7), lst.size());
                if (idx >= 0) {
                    Task t = lst.get(idx);
                    t.markAsUndone();
                    System.out.println("____________________________________________________________\n");
                    System.out.println("OK, I've marked this task as not done yet:");
                    System.out.println("  " + t);
                    System.out.println("____________________________________________________________");
                }
                continue;
            }

            System.out.println("____________________________________________________________\n");
            System.out.println("added: " + input);
            lst.add(new Task(input));
            System.out.println("____________________________________________________________");
        }

        sc.close();
    }

    private static int parseIndex(String s, int size) {
        try {
            int n = Integer.parseInt(s.trim());
            int idx = n - 1;
            if (idx >= 0 && idx < size) return idx;
        } catch (NumberFormatException ignored) { }
        System.out.println("____________________________________________________________\n");
        System.out.println("Invalid task number.");
        System.out.println("____________________________________________________________");
        return -1;
    }
}
