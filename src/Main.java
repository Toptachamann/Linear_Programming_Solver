import java.io.IOException;

/**
 * Created by Timofey on 8/3/2017.
 */
public class Main {
    public static void main(String[] argc){
        LPInputReader reader = new LPInputReader();
        try{
            reader.readInput("D:\\Java_Projects\\LPSolver\\input.txt");
            LPSolver solver = new LPSolver(reader.getLPStandardForm(), true);
            solver.setOut("D:\\Java_Projects\\LPSolver\\output.txt");
            solver.solve(true, 10);
        }catch(SolutionException e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }

    }
}
