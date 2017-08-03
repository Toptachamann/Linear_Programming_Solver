
import java.io.File;
import java.io.FileNotFoundException;
import java.math.*;
import java.util.*;
import java.util.regex.*;

public class LPInputReader {
    private LPStandardForm stForm;
    private ArrayList<ArrayList<BigDecimal>> A;
    private ArrayList<BigDecimal> b, c;
    private HashMap<Integer, String> variables;
    private HashMap<String, Integer> coefficients;
    private int numOfVariables, numOfInequalities;

    public LPInputReader() {
        stForm = new LPStandardForm();
        A = new ArrayList<ArrayList<BigDecimal>>();
        b = new ArrayList<BigDecimal>();
        c = new ArrayList<BigDecimal>();
        variables = new HashMap<Integer, String>();
        coefficients = new HashMap<String, Integer>();
        numOfVariables = 0;
        this.numOfInequalities = 0;
    }

    public void readInput(String path) throws SolutionException {
        int counter = 0;
        try {
            File file = new File(path);
            if(!file.exists()){
                throw new SolutionException("No such file found");
            }
            Scanner in = new Scanner(file);
            String objective = in.nextLine();
            readObjective(objective);
            while (true) {
                String constraint = in.nextLine();
                if (constraint.compareTo("") != 0) {
                    this.readConstraint(constraint);
                    ++counter;
                } else if (counter > 0) {
                    break;
                }
            }
        } catch (NoSuchElementException e) {
            if(counter == 0) {
                e.printStackTrace();
                throw new SolutionException("No data in input file");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for (ArrayList<BigDecimal> row : A) {
            int size = row.size();
            for (int j = 0; j < numOfVariables - size; j++) {
                row.add(BigDecimal.ZERO);
            }
        }
        stForm.setA(A);
        stForm.setb(b);
        stForm.setc(c);
        stForm.setVariables(variables);
        stForm.setCoefficients(coefficients);
        stForm.setMinimize(true);
        stForm.setNumOfInequalities(this.numOfInequalities);
        stForm.setNumOfVariables(numOfVariables);
    }

    private void readObjective(String objective) {
        Pattern pat = Pattern.compile("(([+-]?\\s*\\d*\\.?\\d*)\\*?([a-zA-Z]+\\d*))");
        Matcher matcher = pat.matcher(objective);
        String t;
        for (int i = 0; matcher.find(); i++) {
            variables.put(i, matcher.group(3));
            coefficients.put(matcher.group(3), i);
            t = matcher.group(2);
            t = t.replaceAll("\\s", "");
            if (t.compareTo("") == 0 || t.compareTo("+") == 0) {
                t = "1";
            } else if (t.compareTo("-") == 0) {
                t = "-1";
            }
            c.add(new BigDecimal(t));
        }
        numOfVariables = variables.size();

    }

    @SuppressWarnings("unchecked")
    private boolean readConstraint(String constraint) throws SolutionException {
        ArrayList<BigDecimal> coefs = new ArrayList<BigDecimal>(numOfVariables + 1);
        BigDecimal bi = null;
        for (int i = 0; i < numOfVariables; i++) {
            coefs.add(BigDecimal.ZERO);
        }
        Pattern pat = Pattern.compile("(([+-]?\\s*\\d*\\.?\\d*)\\*?([a-zA-Z]+\\d*))");
        Matcher matcher = pat.matcher(constraint);
        while (matcher.find()) {
            String varName = matcher.group(3);
            if (!coefficients.containsKey(varName)) {
                variables.put(numOfVariables, varName);
                coefficients.put(varName, numOfVariables);
                numOfVariables += 1;
                coefs.add(null);
                c.add(BigDecimal.ZERO);
            }
            String t = matcher.group(2);
            t = t.replaceAll("\\s+", "");
            if (t.compareTo("") == 0 || t.compareTo("+") == 0) {
                t = "1";
            } else if (t.compareTo("-") == 0) {
                t = "-1";
            }
            coefs.set(coefficients.get(varName), new BigDecimal(t));
        }
        pat = Pattern.compile("(>=|<=|=|==)");
        matcher = pat.matcher(constraint);
        if (matcher.find()) {
            String ineqSign = matcher.group(1);
            pat = Pattern.compile("(>=|<=|=|==)\\s*(-?\\d+(\\.?\\d+)?)");
            matcher = pat.matcher(constraint);
            if (matcher.find()) {
                String t = matcher.group(2);
                bi = new BigDecimal(t);
            } else {
                throw new SolutionException("No numeric value after constraint sign");
            }
            if (ineqSign.compareTo(">=") == 0) {
                for (int i = 0; i < coefs.size(); i++) {
                    coefs.set(i, coefs.get(i).negate());
                }
                b.add(bi.negate());
                A.add(coefs);
                this.numOfInequalities += 1;
            } else if (ineqSign.compareTo("==") == 0 || ineqSign.compareTo("=") == 0) {
                ArrayList<BigDecimal> auxiliaryCoefs = (ArrayList<BigDecimal>) coefs.clone();
                for (int i = 0; i < coefs.size(); i++) {
                    auxiliaryCoefs.set(i, coefs.get(i).negate());
                }
                A.add(coefs);
                A.add(auxiliaryCoefs);
                b.add(bi);
                b.add(bi.negate());
                this.numOfInequalities += 2;
            } else {
                A.add(coefs);
                b.add(bi);
                this.numOfInequalities += 1;
            }
            return true;
        } else {
            return false;
        }
    }

    public LPStandardForm getLPStandardForm() {
        return this.stForm;
    }

}
