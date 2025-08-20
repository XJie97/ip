import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Tkit {

    private enum Command {
        ADD,
        LIST,
        BYE,
        UNKNOWN
    }

    public static void main(String[] args) {
        String identity = "not three kids in a trench coat";
        Scanner sc = new Scanner(System.in);

        List<String> lst = new ArrayList<>();

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
                        for (int i = 0; i < lst.size(); i++) {
                            System.out.println((i + 1) + ". " + lst.get(i));
                        }
                    }
                System.out.println("____________________________________________________________");
            }

            else{ 
                System.out.println("____________________________________________________________\n");
                System.out.println("added: " + input);
                lst.add(input);
                System.out.println("____________________________________________________________");
            }
        }

        sc.close();

    }
}
