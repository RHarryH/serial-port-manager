package simulation.gui;

public class Simulation {

	private static void createAndShowGUI() {
        //Create and set up the window.
		new SimulationFrame("Simulation");
    }
	
	public static void main(String[] args) {

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
        		//r.setTarget(target);
                createAndShowGUI();
            }
        });
    }
}
