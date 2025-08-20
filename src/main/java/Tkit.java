import java.util.Scanner;

public class Tkit {
    public static void main(String[] args) {
        String identity = "not three kids in a trench coat";
        Scanner sc = new Scanner(System.in);
        System.out.println("____________________ \n\nHello from  " + identity);
        System.out.println("What do you mean we are three kids in a trench coat?");
        System.out.println("We are an adult person! \n ____________________\n");

        while(true) {
            String input = sc.nextLine().trim();
            
            if (input.equals("bye")) {
                System.out.println("____________________________________________________________\n");
                System.out.println("Goodbye fellow adult!");
                System.out.println("____________________________________________________________");
                break;
            }
            
            System.out.println("____________________________________________________________\n");
            System.out.println("" + input + " heehee");
            System.out.println("____________________________________________________________");
        }

        sc.close();

        
    }
}
