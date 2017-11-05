package lpsolver;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/** Created by Timofey on 8/3/2017. */
public class Main {
  private static final File configFile =
      new File("C:\\Java_Projects\\LPSolver\\conf\\config.properties");

  public static void main(String[] args) {
    Properties properties = new Properties();

    Options options = new Options();

    Option inputOption = new Option("i", "input file", true, "file containing linear program");
    inputOption.setRequired(false);
    options.addOption(inputOption);

    Option outputOption = new Option("o", "output file", true, "file to print results in");
    outputOption.setRequired(false);
    options.addOption(outputOption);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd;

    try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
      properties.load(reader);
      cmd = parser.parse(options, args);

      String strInputFile = cmd.getOptionValue("i");
      if (strInputFile == null) {
        strInputFile = properties.getProperty("input_file");
        if (strInputFile == null) {
          System.err.println("Input file is not specified in config file");
          System.exit(1);
          return;
        }
      }
      String strOutputFile = cmd.getOptionValue("o");
      if (strOutputFile == null) {
        strOutputFile = properties.getProperty("output_file");
        if (strOutputFile == null) {
          System.err.println("Output file is not specified in config file");
          System.exit(1);
          return;
        }
      }
      LPInputReader lpInputReader = new LPInputReader();
      lpInputReader.readInput(strInputFile);
      LPSolver solver = new LPSolver(lpInputReader.getLPStandardForm());
      solver.setOut(strOutputFile);
      solver.solve(10);
    } catch (SolutionException e) {
      e.printStackTrace();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("utility-name", options);
      System.exit(1);
      return;
    }
  }
}
