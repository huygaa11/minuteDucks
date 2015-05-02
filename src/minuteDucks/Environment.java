package minuteDucks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.FPSAnimator;

import java.util.ArrayList;

class Environment extends JFrame implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, ActionListener {

	/* This defines the objModel class, which takes care
	 * of loading a triangular mesh from an obj file,
	 * estimating per vertex average normal,
	 * and displaying the mesh.
	 */
	
//	class Player extends objModel{
//
//		public Player(String filename) {
//			super(filename);
//			
//		}
//		
//		private moveUp(){
//			
//		}
//	}
	
	class Player extends Obstacle{

		public Player(objModel obj, float x, float y, float z) {
			super(obj, x, y, z);
		}
		
		public boolean checkCollision(){
			
			return false;
		}
	}
	
	class objModel {
		public FloatBuffer vertexBuffer;
		public IntBuffer faceBuffer;
		public FloatBuffer normalBuffer;
		public Point3f center;
		public int num_verts;		// number of vertices
		public int num_faces;		// number of triangle faces
		public float radius = .5f;		// radius
		
		public void Draw() {
			vertexBuffer.rewind();
			normalBuffer.rewind();
			faceBuffer.rewind();
			gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
			gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
			
			gl.glVertexPointer(3, GL.GL_FLOAT, 0, vertexBuffer);
			gl.glNormalPointer(GL.GL_FLOAT, 0, normalBuffer);
			
			gl.glDrawElements(GL.GL_TRIANGLES, num_faces*3, GL.GL_UNSIGNED_INT, faceBuffer);
			
			gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
		}
		
		public objModel(String filename) {
			/* load a triangular mesh model from a .obj file */
			BufferedReader in = null;
			try {
				in = new BufferedReader(new FileReader(filename));
			} catch (IOException e) {
				System.out.println("Error reading from file " + filename);
				System.exit(0);
			}

			center = new Point3f();			
			float x, y, z;
			int v1, v2, v3;
			float minx, miny, minz;
			float maxx, maxy, maxz;
			float bbx, bby, bbz;
			minx = miny = minz = 10000.f;
			maxx = maxy = maxz = -10000.f;
			
			String line;
			String[] tokens;
			ArrayList<Point3f> input_verts = new ArrayList<Point3f> ();
			ArrayList<Integer> input_faces = new ArrayList<Integer> ();
			ArrayList<Vector3f> input_norms = new ArrayList<Vector3f> ();
			try {
			while ((line = in.readLine()) != null) {
				if (line.length() == 0)
					continue;
				switch(line.charAt(0)) {
				case 'v':
					tokens = line.split("[ ]+");
					x = Float.valueOf(tokens[1]);
					y = Float.valueOf(tokens[2]);
					z = Float.valueOf(tokens[3]);
					minx = Math.min(minx, x);
					miny = Math.min(miny, y);
					minz = Math.min(minz, z);
					maxx = Math.max(maxx, x);
					maxy = Math.max(maxy, y);
					maxz = Math.max(maxz, z);
					input_verts.add(new Point3f(x, y, z));
					center.add(new Point3f(x, y, z));
					break;
				case 'f':
					tokens = line.split("[ ]+");
					String s1 = tokens[1];
					if(s1.contains("/"))
						s1 = s1.split("/")[0];
					
					if(s1.charAt(0) == '-')
						s1 = s1.substring(1);
					
					v1 = Integer.valueOf(s1)-1;
					
					String s2 = tokens[2];
					if(s2.contains("/"))
						s2 = s2.split("/")[0];
					
					if(s2.charAt(0) == '-')
						s2 = s2.substring(1);
					
					v2 = Integer.valueOf(s2)-1;
					
					String s3 = tokens[3];
					if(s3.contains("/"))
						s3 = s3.split("/")[0];
					
					if(s3.charAt(0) == '-')
						s3 = s3.substring(1);
					
					v3 = Integer.valueOf(s3)-1;
//					v2 = Integer.valueOf(tokens[2])-1;
//					v3 = Integer.valueOf(tokens[3])-1;
					input_faces.add(v1);
					input_faces.add(v2);
					input_faces.add(v3);				
					break;
				default:
					continue;
				}
			}
			in.close();	
			} catch(IOException e) {
				System.out.println("Unhandled error while reading input file.");
			}

			System.out.println("Read " + input_verts.size() +
						   	" vertices and " + input_faces.size() + " faces.");
			
			center.scale(1.f / (float) input_verts.size());
			
			bbx = maxx - minx;
			bby = maxy - miny;
			bbz = maxz - minz;
			float bbmax = Math.max(bbx, Math.max(bby, bbz));
			
			for (Point3f p : input_verts) {
				
				p.x = (p.x - center.x) / bbmax;
				p.y = (p.y - center.y) / bbmax;
				p.z = (p.z - center.z) / bbmax;
			}
			center.x = center.y = center.z = 0.f;
			
			/* estimate per vertex average normal */
			int i;
			for (i = 0; i < input_verts.size(); i ++) {
				input_norms.add(new Vector3f());
			}
			
			Vector3f e1 = new Vector3f();
			Vector3f e2 = new Vector3f();
			Vector3f tn = new Vector3f();
			for (i = 0; i < input_faces.size(); i += 3) {
				v1 = input_faces.get(i+0);
				v2 = input_faces.get(i+1);
				v3 = input_faces.get(i+2);
				
				e1.sub(input_verts.get(v2), input_verts.get(v1));
				e2.sub(input_verts.get(v3), input_verts.get(v1));
				tn.cross(e1, e2);
				input_norms.get(v1).add(tn);
				
				e1.sub(input_verts.get(v3), input_verts.get(v2));
				e2.sub(input_verts.get(v1), input_verts.get(v2));
				tn.cross(e1, e2);
				input_norms.get(v2).add(tn);
				
				e1.sub(input_verts.get(v1), input_verts.get(v3));
				e2.sub(input_verts.get(v2), input_verts.get(v3));
				tn.cross(e1, e2);
				input_norms.get(v3).add(tn);			
			}

			/* convert to buffers to improve display speed */
			for (i = 0; i < input_verts.size(); i ++) {
				input_norms.get(i).normalize();
			}
			
			vertexBuffer = BufferUtil.newFloatBuffer(input_verts.size()*3);
			normalBuffer = BufferUtil.newFloatBuffer(input_verts.size()*3);
			faceBuffer = BufferUtil.newIntBuffer(input_faces.size());
			
			for (i = 0; i < input_verts.size(); i ++) {
				vertexBuffer.put(input_verts.get(i).x);
				vertexBuffer.put(input_verts.get(i).y);
				vertexBuffer.put(input_verts.get(i).z);
				normalBuffer.put(input_norms.get(i).x);
				normalBuffer.put(input_norms.get(i).y);
				normalBuffer.put(input_norms.get(i).z);			
			}
			
			for (i = 0; i < input_faces.size(); i ++) {
				faceBuffer.put(input_faces.get(i));	
			}			
			num_verts = input_verts.size();
			num_faces = input_faces.size()/3;
		}		
	}

	
	class Obstacle{
		objModel obj;
		public float x, y, z, speed;
		public float r = 0.5f;
		public float radius;

		public Obstacle(objModel obj, float x, float y, float z) {
			this.obj = obj;
			this.x = x;
			this.y = y;
			this.z = z;
			this.radius = r * ((float)Math.random() + 1) / 2;
		}
		public Obstacle() {
			obj = null;
			x = 0;
			y = 0;
			z = 0;
			this.radius = r * ((float)Math.random() + 1) / 2;
		}
	}
	
	public float cameraYSpeed = 0;
	public float cameraXSpeed = 0;
	public float dif = 0.2f;
	public float rot = 1f;
	
	public void keyPressed(KeyEvent e) {

		switch(e.getKeyCode()) {
		case KeyEvent.VK_SPACE:
//			System.out.println("pressed");
			break;
		case KeyEvent.VK_RIGHT:
//			System.out.println("right");
			cameraXSpeed = -2*rot;
			xdif = dif;
			break;
		case KeyEvent.VK_LEFT:
//			System.out.println("left");
			cameraXSpeed = +2*rot;
			xdif= -dif;
			break;
		case KeyEvent.VK_UP:
//			System.out.println("up");
			ydif = dif;
			cameraYSpeed = -rot;
			break;
		case KeyEvent.VK_DOWN:
//			System.out.println("down");
			ydif = -dif;
			cameraYSpeed = rot;
			break;
		case KeyEvent.VK_ESCAPE:
			System.exit(0);
			break;		
		case 'p':
		case 'P':
			if (animator.isAnimating())
				animator.stop();
			else 
				animator.start();
			break;
		case '+':
		case '=':
			animation_speed *= 1.2f;
			break;
		case '-':
		case '_':
			animation_speed /= 1.2;
			break;
		case 'w':	
		case 'W':
			cameraYSpeed = 1f;
		default:
			break;
		}
//		canvas.display();
	}
	

	
	public void keyReleased(KeyEvent e) { 
		switch(e.getKeyCode()) {
		case KeyEvent.VK_SPACE:
//			System.out.println("released");
			break;
		case KeyEvent.VK_RIGHT:
//			System.out.println("right");
			xdif = 0.0f;
			cameraXSpeed = 0;
			break;
		case KeyEvent.VK_LEFT:
//			System.out.println("left");
			cameraXSpeed = 0;
			xdif= -0.0f;
			break;
		case KeyEvent.VK_UP:
//			System.out.println("up");
			cameraYSpeed = 0;
			ydif = 0.0f;
			break;
		case KeyEvent.VK_DOWN:
//			System.out.println("down");
			cameraYSpeed = 0;
			ydif = -0.0f;
			break;
		case KeyEvent.VK_W:	
			cameraYSpeed = 0;	
		default:
			break;
		}
//		canvas.display();
	}
	
	/* GL, display, model transformation, and mouse control variables */
	private final GLCanvas canvas;
	private GL gl;
	private final GLU glu = new GLU();	
	private FPSAnimator animator;

	private int winW = 1200, winH = 800;
	private boolean wireframe = false;
	private boolean cullface = true;
	private boolean flatshade = false;
	
	private float xpos = 0, ypos = 0, zpos = 0;
	private float centerx, centery, centerz;
	private float roth = 0, rotv = 0;
	private float znear, zfar;
	private int mouseX, mouseY, mouseButton;
	private float motionSpeed, rotateSpeed;
	private float animation_speed = 1.0f;
	
	
	/* === YOUR WORK HERE === */
	/* Define more models you need for constructing your scene */
	private Player player = new Player(new objModel("dark_fighter_6.obj"), 0, 0 ,0);
	private float playerx = 0;
	private float playery = 0;
	private float playerz = 5f;
	
	private float xdif = 0;
	private float ydif = 0;
	Obstacle[] obstacles = new Obstacle[astcnt];

	

	private float depth = -50.f;
	
	/* Here you should give a conservative estimate of the scene's bounding box
	 * so that the initViewParameters function can calculate proper
	 * transformation parameters to display the initial scene.
	 * If these are not set correctly, the objects may disappear on start.
	 */
	private float xmin = -2.5f, ymin = -1.7f, zmin = -1.7f;
	private float xmax = 2.5f, ymax = 1.7f, zmax = 1.7f;	
	
	
	public void display(GLAutoDrawable drawable) {
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		
		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, wireframe ? GL.GL_LINE : GL.GL_FILL);	
		gl.glShadeModel(flatshade ? GL.GL_FLAT : GL.GL_SMOOTH);		
		if (cullface)
			gl.glEnable(GL.GL_CULL_FACE);
		else
			gl.glDisable(GL.GL_CULL_FACE);		
		
		gl.glLoadIdentity();
		
	    float mat_ambient[] = { 0.7f, 0.7f, 0.7f, 1.0f };
	    float mat_specular[] = { .8f, .8f, .8f, 1 };
	    float mat_diffuse[] = { .2f, .6f, .8f, 1 };
	    float mat_shininess[] = { 128 };

		/* this is the transformation of the entire scene */
	    
	    gl.glTranslatef(-playerx, -playery - .1f, 0);
	    
		gl.glTranslatef(-xpos, -ypos, -zpos);
		gl.glTranslatef(centerx, centery, centerz);
//		gl.glRotatef(360.f - roth, 0, 1.0f, 0);
//		gl.glRotatef(rotv, 1.0f, 0, 0);
		gl.glTranslatef(-centerx, -centery, -centerz);	

		
		/* === YOUR WORK HERE === */
		
		// Statue 
		for(int i = 0; i < obstacles.length; i++){
			gl.glPushMatrix();
			gl.glTranslatef(obstacles[i].x, obstacles[i].y, obstacles[i].z);
			
			gl.glScalef(obstacles[i].radius, obstacles[i].radius, obstacles[i].radius);
			obstacles[i].obj.Draw();
			gl.glPopMatrix();
		}
		
//		gl.glTranslatef(-playerx, -playery, 0);
		
		gl.glPushMatrix();
		gl.glTranslatef(playerx, playery, playerz);

		
		gl.glRotatef(360.f - roth, 0, 0, -1f);
		gl.glRotatef(rotv, -1.0f, 0, 0);
		
		gl.glRotatef(-90f, 0, 1f, 0);
		player.obj.Draw();
		gl.glPopMatrix();
		
		if (animator.isAnimating()){
			for(Obstacle obst : obstacles){
				obst.z += obst.speed;
				
				obst.x += (Math.random() - .5f) / 10;
				obst.y += (Math.random() -.5f)/ 10;
				
				if( obst.z >= 8 ){
					obst.x = Math.random() < .5 ? (float)Math.random()*obslim : (float)Math.random()*-obslim;
					obst.y = Math.random() < .5 ? (float)Math.random()*obslim : (float)Math.random()*-obslim;
					obst.z = -40f;
					obst.speed = (float)(Math.random()+1)/2;
					obst.radius = obst.r * ((float)Math.random() * 2 + 1) / 2;
					System.out.println(obst.radius);
				}
			}
			
			if(roth < 50f && roth > -50f)
				roth += cameraXSpeed;
			
			if(cameraXSpeed == 0){
				if(roth > 0.2f) 
					roth -= rot;
				else if (roth < -0.2f)
					roth += rot;
			}
			
			playerx += xdif;
			if(checkRangeX()){
				playerx -= xdif;
			}
			
			
			
			if(rotv < 20f && rotv > -20f)
				rotv += cameraYSpeed;
			
			if(cameraYSpeed == 0){
				if(rotv > 0.5f) 
					rotv -= 2*rot/3;
				else if(rotv < -0.5f)
					rotv += 2*rot/3;
			}
			
			playery += ydif;
			if(checkRangeY()){
				playery -= ydif;
			}
			
			
			
			if(checkCollision()){
				System.out.println("Deadd");
				//animator.stop();

			}
		}
	}	
	
	private boolean checkRangeX() {
		return playerx < -obslim || playerx > obslim;
	}
	private boolean checkRangeY() {
		return playery < -obslim || playery > obslim;
	}


	private boolean checkCollision() {
		for(Obstacle obst : obstacles){
			if(isCollided(obst)){
				return true;
			}
		}
		return false;
	}

	private boolean isCollided(Obstacle obst) {
		return dist(obst) < player.radius + obst.radius;
	}

	private float dist(Obstacle obst) {
		Vector3f playerPos = new Vector3f(playerx, playery, playerz);
		Vector3f obstPos = new Vector3f(obst.x, obst.y, obst.z);
		playerPos.sub(obstPos);
		
//		if(playerPos.length() < 5f)
//			System.out.println(playerPos.length());
		return playerPos.length();
	}

	int obslim = 10;
	static int astcnt = 200;
	public Environment() {
		super("Assignment 3 -- Hierarchical Modeling");
		for(int i = 0; i < astcnt; i++){
			obstacles[i] = new Obstacle();
			obstacles[i].obj = new objModel("crystal_1.obj");
			obstacles[i].x = Math.random() < .5 ? (float)Math.random()*obslim : (float)Math.random()*-obslim;
			obstacles[i].y = Math.random() < .5 ? (float)Math.random()*obslim : (float)Math.random()*-obslim;
			obstacles[i].z = -50f;
			obstacles[i].speed = (float)(Math.random()+1)/2;
		}
		canvas = new GLCanvas();
		canvas.addGLEventListener(this);
		canvas.addKeyListener(this);
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
		animator = new FPSAnimator(canvas, 30);	// create a 30 fps animator
		getContentPane().add(canvas);
		setSize(winW, winH);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
		animator.start();
		canvas.requestFocus();
	}
	
	public static void main(String[] args) {

		new Environment();
	}
	
	public void init(GLAutoDrawable drawable) {
		gl = drawable.getGL();

		initViewParameters();
		gl.glClearColor(.1f, .1f, .1f, 1f);
		gl.glClearDepth(1.0f);

	    // white light at the eye
	    float light0_position[] = { 0, 0, 1, 0 };
	    float light0_diffuse[] = { 1, 1, 1, 1 };
	    float light0_specular[] = { 1, 1, 1, 1 };
	    gl.glLightfv( GL.GL_LIGHT0, GL.GL_POSITION, light0_position, 0);
	    gl.glLightfv( GL.GL_LIGHT0, GL.GL_DIFFUSE, light0_diffuse, 0);
	    gl.glLightfv( GL.GL_LIGHT0, GL.GL_SPECULAR, light0_specular, 0);

	    //red light
	    float light1_position[] = { -.1f, .1f, 0, 0 };
	    float light1_diffuse[] = { .6f, .05f, .05f, 1 };
	    float light1_specular[] = { .6f, .05f, .05f, 1 };
	    gl.glLightfv( GL.GL_LIGHT1, GL.GL_POSITION, light1_position, 0);
	    gl.glLightfv( GL.GL_LIGHT1, GL.GL_DIFFUSE, light1_diffuse, 0);
	    gl.glLightfv( GL.GL_LIGHT1, GL.GL_SPECULAR, light1_specular, 0);

	    //blue light
	    float light2_position[] = { .1f, .1f, 0, 0 };
	    float light2_diffuse[] = { .05f, .05f, .6f, 1 };
	    float light2_specular[] = { .05f, .05f, .6f, 1 };
	    gl.glLightfv( GL.GL_LIGHT2, GL.GL_POSITION, light2_position, 0);
	    gl.glLightfv( GL.GL_LIGHT2, GL.GL_DIFFUSE, light2_diffuse, 0);
	    gl.glLightfv( GL.GL_LIGHT2, GL.GL_SPECULAR, light2_specular, 0);

	    //material
	    float mat_ambient[] = { 0, 0, 0, 1 };
	    float mat_specular[] = { .8f, .8f, .8f, 1 };
	    float mat_diffuse[] = { .4f, .4f, .4f, 1 };
	    float mat_shininess[] = { 128 };
	    gl.glMaterialfv( GL.GL_FRONT, GL.GL_AMBIENT, mat_ambient, 0);
	    gl.glMaterialfv( GL.GL_FRONT, GL.GL_SPECULAR, mat_specular, 0);
	    gl.glMaterialfv( GL.GL_FRONT, GL.GL_DIFFUSE, mat_diffuse, 0);
	    gl.glMaterialfv( GL.GL_FRONT, GL.GL_SHININESS, mat_shininess, 0);

	    float bmat_ambient[] = { 0, 0, 0, 1 };
	    float bmat_specular[] = { 0, .8f, .8f, 1 };
	    float bmat_diffuse[] = { 0, .4f, .4f, 1 };
	    float bmat_shininess[] = { 128 };
	    gl.glMaterialfv( GL.GL_BACK, GL.GL_AMBIENT, bmat_ambient, 0);
	    gl.glMaterialfv( GL.GL_BACK, GL.GL_SPECULAR, bmat_specular, 0);
	    gl.glMaterialfv( GL.GL_BACK, GL.GL_DIFFUSE, bmat_diffuse, 0);
	    gl.glMaterialfv( GL.GL_BACK, GL.GL_SHININESS, bmat_shininess, 0);

	    float lmodel_ambient[] = { 0, 0, 0, 1 };
	    gl.glLightModelfv( GL.GL_LIGHT_MODEL_AMBIENT, lmodel_ambient, 0);
	    gl.glLightModeli( GL.GL_LIGHT_MODEL_TWO_SIDE, 1 );

	    gl.glEnable( GL.GL_NORMALIZE );
	    gl.glEnable( GL.GL_LIGHTING );
	    gl.glEnable( GL.GL_LIGHT0 );
	    gl.glEnable( GL.GL_LIGHT1 );
	    gl.glEnable( GL.GL_LIGHT2 );

	    gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LESS);
		gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
		gl.glCullFace(GL.GL_BACK);
		gl.glEnable(GL.GL_CULL_FACE);
		gl.glShadeModel(GL.GL_SMOOTH);		
	}
	
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		winW = width;
		winH = height;

		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL.GL_PROJECTION);
			gl.glLoadIdentity();
			glu.gluPerspective(45.f, (float)width/(float)height, znear, zfar);
		gl.glMatrixMode(GL.GL_MODELVIEW);
	}
	
	
	
	public void mousePressed(MouseEvent e) {	
		mouseX = e.getX();
		mouseY = e.getY();
		mouseButton = e.getButton();
		canvas.display();
	}
	
	public void mouseReleased(MouseEvent e) {
		mouseButton = MouseEvent.NOBUTTON;
		canvas.display();
	}	
	
	public void mouseDragged(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		if (mouseButton == MouseEvent.BUTTON3) {
			zpos -= (y - mouseY) * motionSpeed;
			mouseX = x;
			mouseY = y;
			canvas.display();
		} else if (mouseButton == MouseEvent.BUTTON2) {
			xpos -= (x - mouseX) * motionSpeed;
			ypos += (y - mouseY) * motionSpeed;
			mouseX = x;
			mouseY = y;
			canvas.display();
		} else if (mouseButton == MouseEvent.BUTTON1) {
			roth -= (x - mouseX) * rotateSpeed;
			rotv += (y - mouseY) * rotateSpeed;
			mouseX = x;
			mouseY = y;
			canvas.display();
		}
	}

	
	/* computes optimal transformation parameters for OpenGL rendering.
	 * this is based on an estimate of the scene's bounding box
	 */	
	void initViewParameters()
	{
		roth = rotv = 0;

		float ball_r = (float) Math.sqrt((xmax-xmin)*(xmax-xmin)
							+ (ymax-ymin)*(ymax-ymin)
							+ (zmax-zmin)*(zmax-zmin)) * 0.707f;

		centerx = (xmax+xmin)/2.f;
		centery = (ymax+ymin)/2.f;
		centerz = (zmax+zmin)/2.f;
		xpos = centerx;
		ypos = centery;
		zpos = ball_r/(float) Math.sin(45.f*Math.PI/180.f)+centerz;

		znear = 0.01f;
		zfar  = 1000.f;

		motionSpeed = 0.002f * ball_r;
		rotateSpeed = 0.1f;

	}	
	
	// these event functions are not used for this assignment
	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) { }
	public void keyTyped(KeyEvent e) { }
	
	public void mouseMoved(MouseEvent e) { }
	public void actionPerformed(ActionEvent e) { }
	public void mouseClicked(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) {	}	
}

