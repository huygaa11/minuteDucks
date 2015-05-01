package minuteDucks;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;



public class Input extends GLFWKeyCallback{

	public static boolean[] keys = new boolean[2000];
	
	@Override
	public void invoke(long window, int key, int scancode, int action, int mods) {
		// TODO Auto-generated method stub
		
		keys[key] = action != GLFW.GLFW_RELEASE;
	}
	
}
