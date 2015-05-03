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
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureData;
import com.sun.opengl.util.texture.TextureIO;

import java.util.ArrayList;

class Environment extends JFrame implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, ActionListener {

	/* This defines the objModel class, which takes care
	 * of loading a triangular mesh from an obj file,
	 * estimating per vertex average normal,
	 * and displaying the mesh.
	 */
	
	
	class Player extends Obstacle{

		public Player(objModel obj, float x, float y, float z) {
			super(obj, x, y, z);
			this.radius = 0f;
		}
		
	}
	
	class objModel {
		public FloatBuffer vertexBuffer;
		public IntBuffer faceBuffer;
		public FloatBuffer normalBuffer;
		public FloatBuffer textureBuffer;
		public Point3f center;
		public int num_verts;		// number of vertices
		public int num_faces;		// number of triangle faces
		public float radius = .5f;		// radius
		
		public void Draw() {
			vertexBuffer.rewind();
			normalBuffer.rewind();
			faceBuffer.rewind();
			textureBuffer.rewind();
			
			gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
			gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
			gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
			
			gl.glVertexPointer(3, GL.GL_FLOAT, 0, vertexBuffer);
			gl.glNormalPointer(GL.GL_FLOAT, 0, normalBuffer);
		
			gl.glClientActiveTexture(GL.GL_TEXTURE0);
			gl.glTexCoordPointer(3, GL.GL_FLOAT, 0, textureBuffer);
			gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);  //Bind the IBO
			
			
			gl.glDrawElements(GL.GL_TRIANGLES, num_faces*3, GL.GL_UNSIGNED_INT, faceBuffer);
			
			gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
			gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
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
			float xt, yt, zt;
			int vt1, vt2, vt3;
			
			float minx, miny, minz;
			float maxx, maxy, maxz;
			float bbx, bby, bbz;
			minx = miny = minz = 10000.f;
			maxx = maxy = maxz = -10000.f;
			
			String line;
			String[] tokens;
			ArrayList<Point3f> input_verts = new ArrayList<Point3f> ();
			ArrayList<Point3f> input_text_verts = new ArrayList<Point3f> ();
			
			ArrayList<Integer> input_textures = new ArrayList<Integer> ();
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
					vt1 = vt2 = vt3 = 0;
					tokens = line.split("[ ]+");
					String s1 = tokens[1];
					
					boolean hasText = false;
					
					if(s1.contains("/")){
						String[] array = s1.split("/");
						s1 = array[0];
						if(array.length > 1){
							vt1 = Integer.valueOf(array[1]);
							hasText = true;
						}
					}
					
					if(s1.charAt(0) == '-')
						s1 = s1.substring(1);
					
					v1 = Integer.valueOf(s1)-1;

					//
					String s2 = tokens[2];
					if(s2.contains("/")){
						String[] array = s2.split("/");
						s2 = array[0];
						if(array.length > 1)
							vt2 = Integer.valueOf(array[1]);
					}
					
					if(s2.charAt(0) == '-')
						s2 = s2.substring(1);
					
					v2 = Integer.valueOf(s2)-1;
					
					String s3 = tokens[3];
					if(s3.contains("/")){
						String[] array = s3.split("/");
						s3 = array[0];
						if(array.length > 1)
							vt3 = Integer.valueOf(array[1]);
					}
					
					if(s3.charAt(0) == '-')
						s3 = s3.substring(1);
					
					v3 = Integer.valueOf(s3)-1;
//					v2 = Integer.valueOf(tokens[2])-1;
//					v3 = Integer.valueOf(tokens[3])-1;
					input_faces.add(v1);
					input_faces.add(v2);
					input_faces.add(v3);
					
					if(hasText){
						input_textures.add(vt1);
						input_textures.add(vt2);
						input_textures.add(vt3);
					}
					break;
				default:
					if(line.length() >= 2){
						switch(line.charAt(1)){
						case 't':
							tokens = line.split("[ ]+");
							xt = Float.valueOf(tokens[1]);
							yt = Float.valueOf(tokens[2]);
							zt = Float.valueOf(tokens[3]);
							input_text_verts.add(new Point3f(xt, yt, zt));
							break;
						case 'n':
							
							break;
						default: 
							continue;
						}
					}
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
			textureBuffer = BufferUtil.newFloatBuffer(input_verts.size()*3);
			faceBuffer = BufferUtil.newIntBuffer(input_faces.size());
			
			for (i = 0; i < input_verts.size(); i ++) {
				vertexBuffer.put(input_verts.get(i).x);
				vertexBuffer.put(input_verts.get(i).y);
				vertexBuffer.put(input_verts.get(i).z);
				normalBuffer.put(input_norms.get(i).x);
				normalBuffer.put(input_norms.get(i).y);
				normalBuffer.put(input_norms.get(i).z);	
				
				if(input_text_verts.size() > 0){
					textureBuffer.put(input_text_verts.get(i).x);	
					textureBuffer.put(input_text_verts.get(i).y);	
					textureBuffer.put(input_text_verts.get(i).z);	
				}
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
		public float x, y, z, speed, speedx, speedy;
		public float r = 0.25f;
		public float radius;
		public double color;

		public Obstacle(objModel obj, float x, float y, float z) {
			this.obj = obj;
			this.x = x;
			this.y = y;
			this.z = z;
			this.radius = findRadius();
		}
		public float findRadius(){
//			return 1;
			double rand = Math.random();
			

			return R*r * (float)(rand*rand*rand*rand*rand*rand + 0.3);
//			return r;
		}
		public Obstacle() {
			obj = null;
			x = 0;
			y = 0;
			z = 0;
			this.radius = findRadius();
		}
	}
	
	public float cameraYSpeed = 0;
	public float cameraXSpeed = 0;
	public float dif = 0.4f;
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
		case 'f':
		case 'F':
			funMode = !funMode;
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
	
	private boolean funMode = false;
	public static float R = 8;
	
	/* === YOUR WORK HERE === */
	/* Define more models you need for constructing your scene */
	private Player player = new Player(new objModel("dark_fighter_6.obj"), 0, 0 ,0);
	private float playerx = 0;
	private float playery = 0;
	private float playerz = 5f;
	
	private float xdif = 0;
	private float ydif = 0;
	Obstacle[] obstacles = new Obstacle[astcnt];

	Texture[] skybox;
	

	private float depth = -50.f;
	
	/* Here you should give a conservative estimate of the scene's bounding box
	 * so that the initViewParameters function can calculate proper
	 * transformation parameters to display the initial scene.
	 * If these are not set correctly, the objects may disappear on start.
	 */
	private float xmin = -2.5f, ymin = -1.7f, zmin = -1.7f;
	private float xmax = 2.5f, ymax = 1.7f, zmax = 1.7f;	
	
	
	public void display(GLAutoDrawable drawable) {
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);
		
        // Change to projection matrix.
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        
        // Perspective.
        float widthHeightRatio = (float) getWidth() / (float) getHeight();
        glu.gluPerspective(45, widthHeightRatio, 1, 1000);
//        glu.gluLookAt(0, 0, 100, 0, 0, 0, 0, 1, 0);

        // Change back to model view matrix.
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
		
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

		/* Skybox */
		gl.glPushMatrix();
		
		int d = 200;
        int w = 150;
        
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glColor3f(0f, 1f, 0.2f);
		//Front
		skybox[0].enable();
		skybox[0].bind();
		gl.glBegin(GL.GL_QUADS);
		gl.glTexCoord2f(0, 0); 	gl.glVertex3f(-w, -w, -d);
		gl.glTexCoord2f(1, 0); gl.glVertex3f(+w, -w, -d);
		gl.glTexCoord2f(1, 1); 	gl.glVertex3f(w, w, -d);
		gl.glTexCoord2f(0, 1);	gl.glVertex3f(-w, w, -d);
        gl.glEnd();
        skybox[0].disable();
        
        
//        
//        //Left
//        skybox[1].enable();
//		skybox[1].bind();
//        gl.glBegin(GL.GL_QUADS);
//        gl.glVertex3f(-30, -10, -10);
//        gl.glVertex3f(-30, +10, -10);
//        gl.glVertex3f(-30, +10, +10);
//        gl.glVertex3f(-30, -10, +10);
//        gl.glEnd();
//        skybox[1].disable();
//        
//        //Behind
//		gl.glBegin(GL.GL_QUADS);
//        gl.glVertex3f(-w, w, +d);
//        gl.glVertex3f(w, w, +d);
//        gl.glVertex3f(+w, -w, +d);
//		gl.glVertex3f(-w, -w, +d);
//        gl.glEnd();
//        
//        // Right
//        gl.glBegin(GL.GL_QUADS);
//        gl.glVertex3f(+30, -10, +10);
//        gl.glVertex3f(+30, +10, +10);
//        gl.glVertex3f(+30, +10, -10);
//        gl.glVertex3f(+30, -10, -10);
//        gl.glEnd();
//
//        // Up
//        gl.glBegin(GL.GL_QUADS);
//        gl.glVertex3f(-10, +30, -10);
//        gl.glVertex3f(+10, +30, -10);
//        gl.glVertex3f(+10, +30, +10);
//        gl.glVertex3f(-10, +30, +10);
//        gl.glEnd();
//        
//        // Down
//        gl.glBegin(GL.GL_QUADS);
//        gl.glVertex3f(-10, -30, +10);
//        gl.glVertex3f(+10, -30, +10);
//        gl.glVertex3f(+10, -30, -10);
//        gl.glVertex3f(-10, -30, -10);
//        gl.glEnd();
        
        
        gl.glPopMatrix();
		/* === YOUR WORK HERE === */
		
		// Statue 
		for(int i = 0; i < obstacles.length; i++){
			gl.glPushMatrix();
			gl.glTranslatef(obstacles[i].x, obstacles[i].y, obstacles[i].z);
			
			gl.glScalef(obstacles[i].radius, obstacles[i].radius, obstacles[i].radius);
			
			double rand = obstacles[i].color;
			
			if(rand < 0.33){
				gl.glDisable( GL.GL_LIGHT1 );
				gl.glDisable( GL.GL_LIGHT2 );
				obstacles[i].obj.Draw();
				gl.glEnable( GL.GL_LIGHT1 );
				gl.glEnable( GL.GL_LIGHT2 );
			}
			else if( rand < 0.66){
//				gl.glDisable( GL.GL_LIGHT1 );
				gl.glDisable( GL.GL_LIGHT3 );
				obstacles[i].obj.Draw();
//				gl.glEnable( GL.GL_LIGHT1 );
				gl.glEnable( GL.GL_LIGHT3 );
			}
			else{
				gl.glDisable( GL.GL_LIGHT2 );
				gl.glDisable( GL.GL_LIGHT3 );
				obstacles[i].obj.Draw();
				gl.glEnable( GL.GL_LIGHT2 );
				gl.glEnable( GL.GL_LIGHT3 );
			}
			
			gl.glPopMatrix();
		}
		
//		gl.glTranslatef(-playerx, -playery, 0);
		
		gl.glPushMatrix();
		gl.glTranslatef(playerx, playery, playerz);

		
		gl.glRotatef(360.f - roth, 0, 0, -1f);
		gl.glRotatef(rotv, -1.0f, 0, 0);
		
		gl.glRotatef(-90f, 0, 1f, 0);
		player.obj.Draw();
		
//		gl.glPushMatrix();
//		gl.glTranslatef(0, 0.3f, 0);
//		obstacles[0].obj.Draw();
//		gl.glPopMatrix();
		
		gl.glPopMatrix();
		
		if (animator.isAnimating()){
			for(Obstacle obst : obstacles){
				obst.z += obst.speed;
				
				obst.x += obst.speedx + ((float)Math.random() - .5f) / 10;
				obst.y += obst.speedy + ((float)Math.random() -.5f)/ 10;
				
				if( obst.z >= 8 ){
					obst.x = Math.random() < .5 ? (float)Math.random()*obslim : (float)Math.random()*-obslim;
					obst.y = Math.random() < .5 ? (float)Math.random()*obslim : (float)Math.random()*-obslim;
					obst.z = far;
					obst.speed = (float)(Math.random()+1)/2;
					obst.speedx = (float)(Math.random()-1)/10;
					obst.speedy = (float)(Math.random()-1)/10;
					obst.radius = obst.findRadius();
					R += (0.1/(R));
//					System.out.println(obst.radius);
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
				
				if(!funMode)
					animator.stop();
				
			}
		}
	}	
	
	private boolean checkRangeX() {
		return playerx < -obslim *2/3 || playerx > obslim*2/3;
	}
	private boolean checkRangeY() {
		return playery < -obslim *2/3 || playery > obslim*2/3;
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
		return dist(obst) < (player.radius + obst.radius) / 2.4;
	}

	private float dist(Obstacle obst) {
		Vector3f playerPos = new Vector3f(playerx, playery, playerz);
		Vector3f obstPos = new Vector3f(obst.x, obst.y, obst.z);
		playerPos.sub(obstPos);
		
//		if(playerPos.length() < 5f)
//			System.out.println(playerPos.length());
		return playerPos.length();
	}

	int obslim = 40;
	float far = -80f;
	
	static int astcnt = 500;
	public Environment() {
		super("project");
		for(int i = 0; i < astcnt; i++){
			obstacles[i] = new Obstacle();
			
			double x = Math.random();
			obstacles[i].obj = null;
			if(x < 0.05){
				obstacles[i].obj = new objModel("crystal_1.obj");
			}
			
			else{
				obstacles[i].obj = new objModel("asteroid_model.obj");
			}
			obstacles[i].x = Math.random() < .5 ? (float)Math.random()*obslim : (float)Math.random()*-obslim;
			obstacles[i].y = Math.random() < .5 ? (float)Math.random()*obslim : (float)Math.random()*-obslim;
			obstacles[i].z = far;
			obstacles[i].speed = (float)(Math.random()+1)/2;
			obstacles[i].speedx = (float)(Math.random()-1)/15;
			obstacles[i].speedy = (float)(Math.random()-1)/15;
			obstacles[i].radius = obstacles[i].findRadius();
			obstacles[i].color = Math.random();
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
	    float light1_diffuse[] = { 1f, .05f, .05f, 1 };
	    float light1_specular[] = { 1f, .05f, .05f, 1 };
	    gl.glLightfv( GL.GL_LIGHT1, GL.GL_POSITION, light1_position, 0);
	    gl.glLightfv( GL.GL_LIGHT1, GL.GL_DIFFUSE, light1_diffuse, 0);
	    gl.glLightfv( GL.GL_LIGHT1, GL.GL_SPECULAR, light1_specular, 0);

	    //green light
	    float light3_position[] = { .1f, -.1f, 0, 0 };
	    float light3_diffuse[] = { .5f, 1f, .05f, 1 };
	    float light3_specular[] = { .5f, 1f, .05f, 1 };
	    gl.glLightfv( GL.GL_LIGHT3, GL.GL_POSITION, light3_position, 0);
	    gl.glLightfv( GL.GL_LIGHT3, GL.GL_DIFFUSE, light3_diffuse, 0);
	    gl.glLightfv( GL.GL_LIGHT3, GL.GL_SPECULAR, light3_specular, 0);
	    
	    
	    //blue light
	    float light2_position[] = { .1f, .1f, 0, 0 };
	    float light2_diffuse[] = { .05f, .05f, 1f, 1 };
	    float light2_specular[] = { .05f, .05f, 1f, 1 };
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
	    gl.glEnable( GL.GL_LIGHT3 );

	    gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LESS);
		gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
		gl.glCullFace(GL.GL_BACK);
		gl.glEnable(GL.GL_CULL_FACE);
		gl.glShadeModel(GL.GL_SMOOTH);		
		
		//Load Textures
		try {
			skybox = new Texture[6];
			TextureData sb0 = TextureIO.newTextureData(new File("stars_fr.jpg"), false, "front");
			System.out.println(sb0);
	        skybox[0] = TextureIO.newTexture(sb0);
	        TextureData sb1 = TextureIO.newTextureData(new File("stars_lf.jpg"), false, "left");
	        skybox[1] = TextureIO.newTexture(sb1);
	        TextureData sb2 = TextureIO.newTextureData(new File("stars_bk.jpg"), false, "back");
	        skybox[2] = TextureIO.newTexture(sb2);
	        TextureData sb3 = TextureIO.newTextureData(new File("stars_rt.jpg"), false, "right");
	        skybox[3] = TextureIO.newTexture(sb3);
	        TextureData sb4 = TextureIO.newTextureData(new File("stars_up.jpg"), false, "up");
	        skybox[4] = TextureIO.newTexture(sb4);
	        TextureData sb5 = TextureIO.newTextureData(new File("stars_dn.jpg"), false, "down");
	        skybox[5] = TextureIO.newTexture(sb5);

        }
        catch (IOException exc) {
            exc.printStackTrace();
            System.exit(1);
        }
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

