package lpsolver;

import lpsolver.LPException;
import lpsolver.LPInputReader;
import lpsolver.LPSolver;
import lpsolver.SolutionException;

import java.io.IOException;

/**
 * Created by Timofey on 8/3/2017.
 */
public class Main {
    public static void main(String[] argc){
        final String defaultPathToInput = "io_files\\input.txt";
        final String defaultPathToOutput = "io_files\\output.txt";
        String pathToOutput, pathToInput;
        if(argc.length > 0){
            pathToInput = argc[0];
            if(argc.length > 1){
                pathToOutput = argc[1];
            }else{
                pathToOutput = defaultPathToOutput;
            }
        }else{
            pathToInput = defaultPathToInput;
            pathToOutput = defaultPathToOutput;
        }

        LPInputReader reader = new LPInputReader();
        try{
            reader.readInput(pathToInput);
            LPSolver solver = new LPSolver(reader.getLPStandardForm());
            solver.setOut(pathToOutput);
            solver.solve(10);
        }catch(LPException e){
            System.out.println(e.getMessage());
        }
        catch(SolutionException e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }

    }
}
