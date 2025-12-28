package simulation.gui;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.navigation.GPSData;

import simulation.RobotMock;

public class SimulationFrame extends JFrame {

	public SimulationFrame(String string) {
		super(string);
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setSize(800, 600);
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		this.setContentPane(panel);

        // add label
        JLabel label = new JLabel("Simulation started");
        panel.add(label);
        
        Canvas canvas = new Canvas(true);
        canvas.setFocusable(true);
        canvas.requestFocusInWindow();
        panel.add(canvas);

        // display the window
        this.setVisible(true);
        
        // zainicjuj wszystko
        new RobotMock(new GPSData(50.06714, 19.92271), canvas);
        //new RobotMock(new GPSData(50.865449, 20.716749), canvas);
	}
}
