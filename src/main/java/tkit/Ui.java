package tkit;

import java.time.LocalDate;
import java.util.List;

/**
 * Handles all user-facing console interactions.
 * Responsibilities:
 *   Printing banners, lists, confirmation messages, and errors
 *   Rendering filtered task results for search/date queries
 * This class is stateless and side-effect free except for standard output.
 */
final class Ui {

    /**
     * Prints the startup banner.
     *
     * @param identity application identity string to display
     */
    void banner(String identity) {
        System.out.println("____________________ \n\n Hello from  " + identity);
        System.out.println("What do you mean we are three kids in a trench coat?");
        System.out.println("We are an adult person! \n____________________\n");
    }

    /** Prints the termination banner. */
    void exit() {
        System.out.println("____________________\n");
        System.out.println("Goodbye, fellow adult!");
        System.out.println("____________________\n");
    }

    /**
     * Renders the current task list.
     *
     * @param tasks ordered view of all tasks
     */
    void list(List<Task> tasks) {
        System.out.println("____________________\n");
        if (tasks.isEmpty()) {
            System.out.println("There are no entries yet.");
        } else {
            System.out.println("Here are the tasks in your list:");
            for (int i = 0; i < tasks.size(); i++) {
                System.out.println((i + 1) + ". " + tasks.get(i));
            }
        }
        System.out.println("____________________\n");
    }

    /**
     * Prints the "added" confirmation block.
     *
     * @param t the task that was added
     * @param totalCount new total number of tasks
     */
    void added(Task t, int totalCount) {
        System.out.println("____________________\n");
        System.out.println("Got it. I've added this task:");
        System.out.println("  " + t);
        System.out.println("Now you have " + totalCount + " tasks in the list.");
        System.out.println("____________________\n");
    }

    /**
     * Prints the "marked done" confirmation block.
     *
     * @param t the task that was marked
     */
    void marked(Task t) {
        System.out.println("____________________\n");
        System.out.println("Nice! I've marked this task as done:");
        System.out.println("  " + t);
        System.out.println("____________________\n");
    }

    /**
     * Prints the "unmarked" confirmation block.
     *
     * @param t the task that was unmarked
     */
    void unmarked(Task t) {
        System.out.println("____________________\n");
        System.out.println("OK, I've marked this task as not done yet:");
        System.out.println("  " + t);
        System.out.println("____________________\n");
    }

    /**
     * Prints the "removed" confirmation block.
     *
     * @param t the task removed
     * @param newSize the new size of the task list
     */
    void removed(Task t, int newSize) {
        System.out.println("____________________\n");
        System.out.println("Noted. I've removed this task:");
        System.out.println("  " + t);
        System.out.println("Now you have " + newSize + " tasks in the list.");
        System.out.println("____________________\n");
    }

    /**
     * Renders the results of a keyword search.
     *
     * @param hits ordered list of tasks that matched
     */
    void matches(List<Task> hits) {
        System.out.println("____________________\n");
        if (hits.isEmpty()) {
            System.out.println("No matching tasks found.");
        } else {
            System.out.println("Here are the matching tasks in your list:");
            for (int i = 0; i < hits.size(); i++) {
                System.out.println((i + 1) + ". " + hits.get(i));
            }
        }
        System.out.println("____________________\n");
    }

    /**
     * Renders the results for tasks that occur on a specific date.
     *
     * @param date calendar date queried
     * @param hits ordered list of tasks that occur on the date
     */
    void onDate(LocalDate date, List<Task> hits) {
        System.out.println("____________________\n");
        if (hits.isEmpty()) {
            System.out.println("No deadlines/events on " + DateTimeUtil.pretty(date) + ".");
        } else {
            System.out.println("Deadlines/events on " + DateTimeUtil.pretty(date) + ":");
            for (int i = 0; i < hits.size(); i++) {
                System.out.println((i + 1) + ". " + hits.get(i));
            }
        }
        System.out.println("____________________\n");
    }

    /**
     * Prints an error block with the provided message.
     *
     * @param msg error message to display
     */
    void error(String msg) {
        System.out.println("____________________\n");
        System.out.println(msg);
        System.out.println("____________________\n");
    }
}
