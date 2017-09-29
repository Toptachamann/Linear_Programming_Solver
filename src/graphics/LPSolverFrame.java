package graphics;

import graphics.utils.GBC;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;

public class LPSolverFrame extends JFrame {
    private static final String TITLE = "LPSolver";

    private JPanel mainPanel;
    public LPSolverFrame(){
        ininitalize();
        setView();
    }
    private void ininitalize(){
        setTitle(TITLE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;
        setSize(new Dimension(2*width/3, 2*height/3));
        Image image = new ImageIcon("images\\LPSolver_Icon.jpg").getImage();
        setIconImage(image);
        setLocationRelativeTo(null);
    }

    private void setView(){
        mainPanel = new JPanel(new GridBagLayout());
        JTextArea inputArea = new JTextArea();
        JTextArea outputArea = new JTextArea();
        JScrollPane inputScrollPane = new JScrollPane(inputArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane outputScrollPane = new JScrollPane(outputArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputScrollPane, outputScrollPane);
        splitPane.setResizeWeight(0.5);
        mainPanel.add(splitPane, new GBC(0, 0, 1, 1, 1, 1));
        this.getContentPane().add(mainPanel);
    }
}
