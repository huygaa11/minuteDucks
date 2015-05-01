package minuteDucks;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*; // NULL

import java.nio.ByteBuffer;

import org.lwjgl.glfw.GLFWvidmode;
import org.lwjgl.opengl.GLContext;

public class Main implements Runnable{
	
	private Thread thread;
	public boolean running = true;
	
	public Long window;
	public int width = 300;
	public int height = 200;
	
	public static void main(String args[]){
		new Main().start();
	}
	
	public void start(){
		running = true;
		this.run();
//		thread = new Thread(this, "Game");
//		thread.start();
	}
	
	public void init(){
		if(glfwInit() != GL_TRUE){
			System.out.println("Failed to initialize glfw");
		}
		
		glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);
		
		window = glfwCreateWindow(width, height, "Game", NULL, NULL);
		
		if(window == NULL){
			System.out.println("No Window");
		}
		
		ByteBuffer vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		
		glfwSetWindowPos(window, 0, 0);
		
		glfwMakeContextCurrent(window);
		glfwShowWindow(window);
		glfwSetKeyCallback(window, new Input());
		
		
		// openGL
		GLContext.createFromCurrent();
		glClearColor(0.56f, 0.258f, 0.425f, 1.0f);
		glEnable(GL_DEPTH_TEST);
		System.out.println("OpenGL: " + glGetString(GL_VERSION));
		
	}
	
	public void update(){
		glfwPollEvents();
		if(Input.keys[GLFW_KEY_SPACE]){
			System.out.println("space");
		}
	}
	
	public void render(){
		glfwSwapBuffers(window);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	}
	
	@Override
	public void run(){
		init();
		while(running){
			update();
			render();
			
			if(glfwWindowShouldClose(window) == GL_TRUE){
				running = false;
			}
		}
	}
	
}