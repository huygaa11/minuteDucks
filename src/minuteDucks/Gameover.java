package minuteDucks;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class Gameover {
	String creators = "Calvin Mei and Batkhuyag Batsaikhan";
	private JFrame frame;
	
	public Gameover(String finalscore, Environment game) {
		frame = new JFrame();
		
		String[] buttons = {"Restart", "Quit"};
		int returnValue = JOptionPane.showOptionDialog(null, "Your final score is: " + finalscore, "Asteroid Game",
		        JOptionPane.PLAIN_MESSAGE, 0, null, buttons, null);
		
		if(returnValue == 1)
			System.exit(0);
		else{
			game.restart();
		}
		
	}
}
