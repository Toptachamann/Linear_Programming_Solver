import java.io.File;

/** Created by Timofey on 8/3/2017. */
public class Main {
  private static final File configFile =
      new File("C:\\Java_Projects\\LPSolver\\conf\\config.properties");

  public static void main(String[] args) {

    /*Properties properties = new Properties();

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
      LPStandardForm stForm = lpInputReader.readLP(new File(strInputFile));
      LPSolver solver = new LPSolver(stForm);
      solver.setOut(strOutputFile);
      solver.solve(10);
    } catch (SolutionException | IOException e) {
      e.printStackTrace();
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("utility-name", options);
      System.exit(1);
    }*/
  }
}
