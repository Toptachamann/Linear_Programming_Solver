import graphics.LPSolverFrame;

import javax.swing.JFrame;
import java.awt.EventQueue;

public class Main {
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                LPSolverFrame frame = new LPSolverFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }
        });
    }
}
