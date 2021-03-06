package livingimagesmetamaker;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import controlP5.Button;
import controlP5.ControlEvent;
import controlP5.ControlP5;
import controlP5.DropdownList;
import controlP5.Textfield;

import processing.core.PApplet;
import processing.core.PGraphicsJava2D;
import processing.core.PImage;
import toxi.geom.Rect;
import toxi.geom.Triangle2D;
import toxi.geom.Vec2D;

public class LivingImagesMetaMaker extends PApplet {
	private static final long serialVersionUID = 1L;
	
	static String[] pargs;
	
	BaseImageFactory baseImageFactory;
	
	ArrayList<String> filenames = new ArrayList<String>();
	BaseImage images = null;
	
	boolean donext = false, refreshMarkers = true, readtriangle = false, adjustFullrect = false, adjustCorner = false;
	boolean faceRect = false, adjustCenter = false;
	int nextimg = 0, clickcount = 0, faceTriId = 0, detailsIndex = 0, cornerVal = 0;
	Marker editingMarker = null;
	Vec2D[] faceTri = new Vec2D[3];
	Rect rectCornerAdjust = null;
	Vec2D centerAdjust = null;
	
	ControlP5 cp5;
	ArrayList<Button> markerbtn = new ArrayList<Button>();
	ArrayList<Textfield> markertxt = new ArrayList<Textfield>();
	Button processbtn, nextbutton, skipbutton, cmd;
	
	DropdownList dl;
	int dropmarker = 0;
	
	String ImageFilename = "";
	
	Graphics2D g2d;
	
	@Override
	public void setup() {
		//		println(PFont.list());
		
		if (pargs == null || pargs.length < 1) {
			println(pargs.length + " : Usage: LivingImagesMetaMaker [image1], [image2...]");
			println("or use LivingImagesMetaMaker -d [folder]");
			this.exit();
		}
		
		if (pargs[0].equalsIgnoreCase("-d")) {
			File f = new File(pargs[1]);
			if (f == null || !f.isDirectory()) {
				println("invalid folder - " + pargs[1]);
				this.exit();
			}
			FilenameFilter ffl = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					String[] comp = name.split("\\.");
					if (comp[comp.length - 1].equalsIgnoreCase("jpg") || comp[comp.length - 1].equalsIgnoreCase("jpeg")
							|| comp[comp.length - 1].equalsIgnoreCase("png"))
						return true;
					return false;
				}
			};
			
			String[] files = f.list(ffl);
			if (files.length < 1) {
				println("no files found in the folder");
				this.exit();
			}
			
			println(files.length + " images found.");
			
			String fnme = pargs[1];
			if (fnme.substring(fnme.length() - 1).equals("/"))
				fnme = fnme.substring(0, fnme.length() - 1);
			for (int i = 0; i < files.length; i++)
				filenames.add(fnme + "/" + files[i]);
			
		} else {
			for (int i = 0; i < pargs.length; i++)
				filenames.add(pargs[i]);
		}
		
		size(800, 600, JAVA2D);
		smooth();
		textFont(createFont("Play", 18));
		g2d = ((PGraphicsJava2D) g).g2;
		
		baseImageFactory = new BaseImageFactory(this, "/Users/hari/Work/code/LivingImages_MetaMaker2/src/data/");
		
		cp5 = new ControlP5(this);
		if (filenames.size() == 1)
			nextbutton = cp5.addButton("save and exit", 0, width - 100, height - 20, 100, 20);
		else
			nextbutton = cp5.addButton("save and next", 0, width - 100, height - 20, 100, 20);
		nextbutton.setId(0);
		
		skipbutton = cp5.addButton("skip", 0, width - 150, height - 20, 40, 20);
		skipbutton.setId(10000);
		
		processbtn = cp5.addButton("process", 0, width - 230, height - 20, 70, 20);
		processbtn.setId(10001);
		
		// DEBUG For testing loading from the xml file
		Directory dir = new Directory(baseImageFactory, "/Users/hari/Work/code/LivingImages_MetaMaker2/src/data/directory.xml");
		//		dir.scan(pargs[1]);
		//		dir.scan("/Users/hari/Work/code/LivingImages_MetaMaker2/src/data/images");
		//		dir.save("/Users/hari/Work/code/LivingImages_MetaMaker2/src/data/directory.xml");
		//		donext = false;
		
		dl = cp5.addDropdownList("name", 0, 0, 70, 100);
		dl.setId(5000);
		int i = 1;
		for (Person p : dir.values())
			dl.addItem(p.name, i++);
		dl.actAsPulldownMenu(true);
		dl.hide();
		
		cmd = cp5.addButton("full rect");
		cmd.setId(5001);
		cmd.hide();
		
		nextimg = 0;
		if (nextimg < filenames.size()) {
			//				donext = true;
			background(0);
			ImageFilename = getImageFoldername(filenames.get(nextimg));
			
			String fname = getBaseImageName(filenames.get(nextimg));
			File bfile = new File(fname);
			
			if (bfile.exists()) {
				images = new BaseImage(baseImageFactory, fname);
				refreshMarkers = true;
			} else {
				PImage img = loadImage(filenames.get(nextimg));
				if (img != null) {
					displayImage(img);
				}
				refreshMarkers = false;
			}
			
			if (nextimg >= filenames.size())
				nextbutton.setCaptionLabel("save and exit");
		}
	}
	
	private void displayImage(PImage img) {
		for (int i = 0; i < markerbtn.size(); i++)
			markerbtn.get(i).remove();
		markerbtn.clear();
		
		for (int i = 0; i < markertxt.size(); i++)
			markertxt.get(i).remove();
		markertxt.clear();
		
		dl.hide();
		
		background(0);
		pushMatrix();
		scale(getScaleFactor(img));
		
		image(img, 0, 0);
		popMatrix();
		
		nextbutton.hide();
	}
	
	/**
	 * LivingImagesMetaMaker - displayCollectInfo
	 * 
	 * @param bimg
	 * 
	 */
	private void displayCollectInfo() {
		for (int i = 0; i < markerbtn.size(); i++)
			markerbtn.get(i).remove();
		markerbtn.clear();
		
		for (int i = 0; i < markertxt.size(); i++)
			markertxt.get(i).remove();
		markertxt.clear();
		
		dl.hide();
		cmd.hide();
		
		background(0);
		pushMatrix();
		scale(getScaleFactor(images));
		
		image(images, 0, 0);
		
		pushStyle();
		noFill();
		int num = 0;
		for (Marker mrk : images.markers) {
			if (mrk.fullrect != null) {
				num++;
				stroke(255, 0, 0, 200);
				rect(mrk.fullrect.x, mrk.fullrect.y, mrk.fullrect.width, mrk.fullrect.height);
				
				// for the UI
				Vec2D tmp = mrk.fullrect.getTopLeft().scale(getScaleFactor(images));
				
				// Draw the mask
				pushMatrix();
				pushStyle();
				
				if (mrk.center != null) {
					ellipse(mrk.center.x, mrk.center.y, 10 * 1f / getScaleFactor(images), 10 * 1f / getScaleFactor(images));
					
					Vec2D tmpm = baseImageFactory.maskCenter.scale(mrk.fullrect.width / 206.5f);
					translate(mrk.center.x - tmpm.x, mrk.center.y - tmpm.y);
					scale(mrk.fullrect.width / 206.5f);
					baseImageFactory.mask.disableStyle();
					noFill();
					stroke(0, 128, 255);
					shape(baseImageFactory.mask);
					baseImageFactory.mask.enableStyle();
					
					//				g2d.setColor(new Color(color(0, 128, 255)));
					//				
					//				if (mrk.mask2d == null) {
					//					// DEBUG extract as Marker.SetMask()?!
					//					mrk.mask2d = (Path2D) baseImageFactory.mask2.clone();
					//					AffineTransform aff = new AffineTransform();
					//					Vec2D tmpm = baseImageFactory.maskCenter.scale(mrk.fullrect.width / 206.5f);
					//					aff.translate(mrk.center.x - tmpm.x, mrk.center.y - tmpm.y);
					//					aff.scale(mrk.fullrect.width / 206.5f, mrk.fullrect.width / 206.5f);
					//					mrk.mask2d.transform(aff);
					//				}
					//				
					//				g2d.draw(mrk.mask2d);
					//				// TODO implement path tweaking functionality! Draw the handles and stuff here
					//				epth.fromPath2D(mrk.mask2d);
					//				if (epth.codes.size() > 0) {
					//					noStroke();
					//					fill(0, 128, 255);
					//					epth.drawPoints(this, 10);
					//				}
					
				}
				popStyle();
				popMatrix();
				
				if (mrk.facerect != null) {
					rect(mrk.facerect.x, mrk.facerect.y, mrk.facerect.width, mrk.facerect.height);
					
					if (mrk.faceTriangle != null) {
						stroke(0, 255, 0, 200);
						beginShape(POLYGON);
						vertex(mrk.faceTriangle.a.x, mrk.faceTriangle.a.y);
						vertex(mrk.faceTriangle.b.x, mrk.faceTriangle.b.y);
						vertex(mrk.faceTriangle.c.x, mrk.faceTriangle.c.y);
						endShape(CLOSE);
					}
					if (mrk.faceTriangle == null || mrk.miscdata > 0) {
						Button tmpb = cp5.addButton("tri" + num, 0, Math.round(tmp.x + 62), Math.round(tmp.y - 25), 45, 25);
						tmpb.setId(images.markers.size() + num);
						tmpb.setCaptionLabel("face tri");
						markerbtn.add(tmpb);
					}
				}
				
				pushStyle();
				noStroke();
				fill(0);
				rect(mrk.fullrect.x, mrk.fullrect.y - 25, 30, 25);
				rect(mrk.fullrect.x, mrk.fullrect.y + mrk.fullrect.height, mrk.fullrect.width, 85);
				fill(255);
				text(num, mrk.fullrect.x + 5, mrk.fullrect.y - 5);
				text(mrk.name, mrk.fullrect.x + 5, mrk.fullrect.y + mrk.fullrect.height + 20);
				text(mrk.urlfacebook, mrk.fullrect.x + 5, mrk.fullrect.y + mrk.fullrect.height + 40);
				text(mrk.urlflickr, mrk.fullrect.x + 5, mrk.fullrect.y + mrk.fullrect.height + 60);
				text(mrk.urltwitter, mrk.fullrect.x + 5, mrk.fullrect.y + mrk.fullrect.height + 80);
				popStyle();
				
				Button tmpb = cp5.addButton("x" + num, 0, Math.round(tmp.x + 30), Math.round(tmp.y - 25), 30, 25);
				tmpb.setId(num);
				tmpb.setCaptionLabel("x");
				markerbtn.add(tmpb);
				
				tmpb = cp5.addButton("detail" + num, 0, Math.round(tmp.x + 110), Math.round(tmp.y - 25), 40, 25);
				tmpb.setId(images.markers.size() * 2 + num);
				tmpb.setCaptionLabel("detail");
				markerbtn.add(tmpb);
			}
		}
		popStyle();
		popMatrix();
		
		mouseDragged();
		
		nextbutton.show();
	}
	
	/**
	 * LivingImagesMetaMaker - getScaleFactor
	 * 
	 * @param bimg
	 * @return
	 * 
	 */
	private float getScaleFactor(BaseImage bimg) {
		if (bimg.width > bimg.height)
			return this.width / (float) bimg.width;
		else
			return this.height / (float) bimg.height;
	}
	
	private float getScaleFactor(PImage bimg) {
		if (bimg.width > bimg.height)
			return this.width / (float) bimg.width;
		else
			return this.height / (float) bimg.height;
	}
	
	public String getImageFoldername(String s) {
		String ts = "";
		String[] tmps = s.split("/");
		for (int i = 0; i < tmps.length - 1; i++)
			ts = ts + tmps[i] + "/";
		return ts;
	}
	
	public String getBaseImageName(String s) {
		String[] tmps = s.split("\\.");
		String basefilename = "";
		for (int i = 0; i < tmps.length - 1; i++)
			basefilename = basefilename + tmps[i] + ".";
		basefilename = basefilename + "xml";
		return basefilename;
	}
	
	private void readDetails(Marker mrk) {
		Float sf = getScaleFactor(images);
		Rect nr = new Rect(mrk.fullrect.getTopLeft().scale(sf), mrk.fullrect.getBottomRight().scale(sf));
		
		dl.setPosition(Math.round(nr.x + 5), Math.round(nr.y + 20));
		dl.show();
		
		cmd.setPosition(Math.round(nr.x + dl.getWidth() + 10), Math.round(nr.y + 20));
		cmd.show();
		
		dropmarker = mrk.miscdata1 - 1000;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		if (images != null) {
			float sf = getScaleFactor(images);
			
			if (readtriangle == true) {
				faceTri[clickcount] = new Vec2D(mouseX, mouseY).scale(1f / sf);
				clickcount++;
				if (clickcount >= 3) {
					readtriangle = false;
					Marker mrk = images.markers.get(faceTriId);
					mrk.faceTriangle = new Triangle2D(faceTri[0], faceTri[1], faceTri[2]);
					mrk.miscdata = 1;
					refreshMarkers = true;
					println(" - Ok!");
				}
			} else if (adjustFullrect == true && rectCornerAdjust != null) {
				float realy = rectCornerAdjust.y * sf;
				rectCornerAdjust.height = (mouseY - realy) * (1f / sf);
				adjustFullrect = false;
				refreshMarkers = true;
			}
		}
	}
	
	@Override
	public void draw() {
		if (refreshMarkers) {
			displayCollectInfo();
			if (images.markers.size() <= 0)
				println("done and found nothing!");
			refreshMarkers = false;
		}
	}
	
	public void controlEvent(ControlEvent theEvent) {
		this.mouseEvent.consume();
		
		if (theEvent.id() == 0 || theEvent.id() == 10000) {
			if (theEvent.id() == 0)
				images.save(getBaseImageName(filenames.get(nextimg)));
			
			nextimg++;
			if (nextimg < filenames.size()) {
				//				donext = true;
				background(0);
				ImageFilename = getImageFoldername(filenames.get(nextimg));
				
				String fname = getBaseImageName(filenames.get(nextimg));
				println(fname);
				File bfile = new File(fname);
				
				if (bfile.exists()) {
					images = new BaseImage(baseImageFactory, fname);
					refreshMarkers = true;
				} else {
					PImage img = loadImage(filenames.get(nextimg));
					if (img != null) {
						displayImage(img);
					}
					refreshMarkers = false;
				}
				
				if (nextimg >= filenames.size())
					nextbutton.setCaptionLabel("save and exit");
			}
			//			if (nextimg < filenames.size()) {
			//				//				donext = true;
			//				background(0);
			//				ImageFilename = getImageFoldername(filenames.get(nextimg));
			//				PImage img = loadImage(filenames.get(nextimg));
			//				if (img != null) {
			//					displayImage(img);
			//				}
			//				if (nextimg >= filenames.size())
			//					nextbutton.setCaptionLabel("save and exit");
			//			} 
			else
				this.exit();
			
		} else if (theEvent.id() == 10001) {
			if (nextimg < filenames.size()) {
				println("Detecting people in image - \"" + filenames.get(nextimg) + "\"");
				ImageFilename = getImageFoldername(filenames.get(nextimg));
				PImage img = loadImage(filenames.get(nextimg));
				if (img != null) {
					images = baseImageFactory.createBaseImage(img, filenames.get(nextimg));
					println(images.markers.size() + " markers found.");
					donext = false;
					refreshMarkers = true;
				}
			}
		} else if (theEvent.id() <= images.markers.size()) {
			images.markers.remove(theEvent.id() - 1);
			println(images.markers.size() + " markers found.");
			refreshMarkers = true;
			
		} else if (theEvent.id() <= images.markers.size() * 2) {
			faceTriId = theEvent.id() - images.markers.size() - 1;
			//			faceTri = new Triangle2D();
			clickcount = 0;
			readtriangle = true;
			//			refreshMarkers = true;
			print("Click one eyes and mouth for this face ");
			
		} else if (theEvent.id() <= images.markers.size() * 3) {
			Marker mrk = images.markers.get(theEvent.id() - images.markers.size() * 2 - 1);
			print("Enter the details...");
			detailsIndex = 0;
			mrk.miscdata1 = 1000 + images.markers.indexOf(mrk);
			readDetails(mrk);
			//			refreshMarkers = true;
			
		} else if (theEvent.id() >= 1000 && theEvent.id() < 5000) {
			Marker mrk = images.markers.get(theEvent.id() - 1000);
			switch (detailsIndex) {
			case 0:
				mrk.name = theEvent.controller().stringValue();
				detailsIndex++;
				theEvent.controller().remove();
				readDetails(mrk);
				break;
			case 1:
				mrk.urlfacebook = theEvent.controller().stringValue();
				detailsIndex++;
				theEvent.controller().remove();
				readDetails(mrk);
				break;
			case 2:
				mrk.urlflickr = theEvent.controller().stringValue();
				detailsIndex++;
				theEvent.controller().remove();
				readDetails(mrk);
				break;
			case 3:
				mrk.urltwitter = theEvent.controller().stringValue();
				detailsIndex++;
				refreshMarkers = true;
				break;
			
			default:
				break;
			}
			
		} else if (theEvent.id() == 5000) {
			Marker mrk = images.markers.get(dropmarker);
			mrk.name = dl.stringValue();
			refreshMarkers = true;
			
		} else if (theEvent.id() == 5001) {
			Marker mrk = images.markers.get(dropmarker);
			if (mrk.fullrect != null) {
				rectCornerAdjust = mrk.fullrect;
				adjustFullrect = true;
			}
		}
		
	}
	
	public void mousePressed() {
		if (images != null) {
			float sf = getScaleFactor(images);
			float wid = 10 * (1f / sf);
			Rect[] cr = new Rect[4];
			Vec2D ms = new Vec2D(mouseX, mouseY).scale(1f / sf);
			for (Marker mrk : images.markers) {
				if (mrk.facerect != null && mrk.facerect.containsPoint(ms)) {
					faceRect = true;
					
					cr[0] = new Rect(mrk.facerect.x, mrk.facerect.y, wid, wid);
					cr[1] = new Rect(mrk.facerect.x + mrk.facerect.width - wid, mrk.facerect.y, wid, wid);
					cr[2] = new Rect(mrk.facerect.x + mrk.facerect.width - wid, mrk.facerect.y + mrk.facerect.height - wid, wid, wid);
					cr[3] = new Rect(mrk.facerect.x, mrk.facerect.y + mrk.facerect.height - wid, wid, wid);
					
					for (int i = 0; i < cr.length; i++) {
						if (cr[i].containsPoint(ms)) {
							rectCornerAdjust = mrk.facerect;
							editingMarker = mrk;
							cornerVal = i;
							adjustCorner = true;
							adjustFullrect = false;
							adjustCenter = false;
							refreshMarkers = true;
							break;
						}
					}
					
					if (adjustCorner == false) {
						cr[0] = new Rect(mrk.center.x - wid / 2f, mrk.center.y - wid / 2f, wid, wid);
						if (cr[0].containsPoint(ms)) {
							centerAdjust = mrk.center;
							editingMarker = mrk;
							adjustCenter = true;
							adjustCorner = false;
							adjustFullrect = false;
							refreshMarkers = true;
							break;
						}
					}
				} else if (mrk.fullrect != null && mrk.fullrect.containsPoint(ms)) {
					faceRect = false;
					
					cr[0] = new Rect(mrk.fullrect.x, mrk.fullrect.y, wid, wid);
					cr[1] = new Rect(mrk.fullrect.x + mrk.fullrect.width - wid, mrk.fullrect.y, wid, wid);
					cr[2] = new Rect(mrk.fullrect.x + mrk.fullrect.width - wid, mrk.fullrect.y + mrk.fullrect.height - wid, wid, wid);
					cr[3] = new Rect(mrk.fullrect.x, mrk.fullrect.y + mrk.fullrect.height - wid, wid, wid);
					
					for (int i = 0; i < cr.length; i++) {
						if (cr[i].containsPoint(ms)) {
							rectCornerAdjust = mrk.fullrect;
							editingMarker = mrk;
							cornerVal = i;
							adjustCorner = true;
							adjustFullrect = false;
							refreshMarkers = true;
							break;
						}
					}
				}
				//				else {
				//					maskPath = new EditablePath();
				//					maskPath.fromPath2D(editingMarker.mask2d);
				//					for (int i = 0; i < maskPath.codes.size(); i++) {
				//						int type = maskPath.codes.get(i), idx = -1;
				//						Coord tc = maskPath.vertices.get(i);
				//						
				//						switch (type) {
				//						case PathIterator.SEG_MOVETO:
				//						case PathIterator.SEG_LINETO:
				//							idx = 0;
				//							break;
				//						
				//						case PathIterator.SEG_QUADTO:
				//							idx = 2;
				//							break;
				//						
				//						case PathIterator.SEG_CUBICTO:
				//							idx = 4;
				//							break;
				//						}
				//						
				//						cr[0] = new Rect(tc.val[idx], tc.val[idx + 1], 10 * (1f / sf), 10 * (1f / sf));
				//						cr[0].setPosition(cr[0].getTopLeft().scale(sf));
				//						cr[0].setDimension(cr[0].getDimensions().scale(sf));
				//						
				//						if (cr[0].containsPoint(ms)) {
				//							maskVertexIndex = i;
				//							adjustMask = true;
				//							editingMarker = mrk;
				//							adjustFullrect = false;
				//							adjustCorner = false;
				//							refreshMarkers = true;
				//							break;
				//						}
				//					}
				//				}
			}
		}
	}
	
	public void mouseDragged() {
		float sf = getScaleFactor(images);
		
		if (adjustCorner && rectCornerAdjust != null && (cornerVal >= 0 && cornerVal < 4)) {
			Rect nr = rectCornerAdjust.copy();
			nr.setPosition(nr.getTopLeft().scale(sf));
			nr.setDimension(nr.getDimensions().scale(sf));
			switch (cornerVal) {
			case 0:
				nr.set(mouseX, mouseY, nr.width - (mouseX - nr.x), nr.height - (mouseY - nr.y));
				break;
			case 1:
				nr.set(nr.x, mouseY, mouseX - nr.x, nr.height - (mouseY - nr.y));
				break;
			case 2:
				nr.set(nr.x, nr.y, mouseX - nr.x, mouseY - nr.y);
				break;
			case 3:
				nr.set(mouseX, nr.y, nr.width - (mouseX - nr.x), mouseY - nr.y);
				break;
			
			default:
				break;
			}
			
			pushStyle();
			noFill();
			stroke(0, 255, 0);
			rect(nr.x, nr.y, nr.width, nr.height);
			popStyle();
			refreshMarkers = true;
			
		} else if (adjustCenter && centerAdjust != null) {
			centerAdjust.set(mouseX, mouseY);
			centerAdjust.scaleSelf(1f / sf);
			refreshMarkers = true;
		}
		//		else if (adjustMask && maskPath != null && maskVertexIndex < maskPath.codes.size()) {
		//			int type = maskPath.codes.get(maskVertexIndex);
		//			Coord tc = maskPath.vertices.get(maskVertexIndex);
		//			Vec2D mv = new Vec2D(mouseX, mouseY);
		//			mv.scaleSelf(1f / sf);
		//			
		//			switch (type) {
		//			case PathIterator.SEG_MOVETO:
		//			case PathIterator.SEG_LINETO:
		//				tc.val[0] = mv.x;
		//				tc.val[1] = mv.y;
		//				break;
		//			
		//			case PathIterator.SEG_QUADTO:
		//				//				cr[0] = new Rect(tc.val[2], tc.val[3], 10, 10);
		//				tc.val[0] = mv.x;
		//				tc.val[1] = mv.y;
		//				break;
		//			
		//			case PathIterator.SEG_CUBICTO:
		//				//				cr[0] = new Rect(tc.val[4], tc.val[5], 10, 10);
		//				break;
		//			}
		//			
		//			editingMarker.mask2d = maskPath.toPath2D();
		//			
		//			refreshMarkers = true;
		//		}
	}
	
	public void mouseReleased() {
		float sf = 0;
		if (images != null)
			sf = getScaleFactor(images);
		
		if (adjustCorner && rectCornerAdjust != null && (cornerVal >= 0 && cornerVal < 4)) {
			Rect nr = rectCornerAdjust.copy();
			nr.setPosition(nr.getTopLeft().scale(sf));
			nr.setDimension(nr.getDimensions().scale(sf));
			switch (cornerVal) {
			case 0:
				nr.set(mouseX, mouseY, nr.width - (mouseX - nr.x), nr.height - (mouseY - nr.y));
				break;
			case 1:
				nr.set(nr.x, mouseY, mouseX - nr.x, nr.height - (mouseY - nr.y));
				break;
			case 2:
				nr.set(nr.x, nr.y, mouseX - nr.x, mouseY - nr.y);
				break;
			case 3:
				nr.set(mouseX, nr.y, nr.width - (mouseX - nr.x), mouseY - nr.y);
				break;
			
			default:
				break;
			}
			
			nr.setPosition(nr.getTopLeft().scale(1f / sf));
			nr.setDimension(nr.getDimensions().scale(1f / sf));
			rectCornerAdjust.set(nr);
			
			if (faceRect) {
				editingMarker.center = rectCornerAdjust.getCentroid();
			} else {
				// TODO adjust the mask to fit
				PGraphicsJava2D pg = (PGraphicsJava2D) createGraphics(images.width, images.height, JAVA2D);
				pg.beginDraw();
				pg.smooth();
				pg.background(0);
				pg.pushMatrix();
				Vec2D tmpm = baseImageFactory.maskCenter.scale(nr.width / 206.5f);
				pg.translate(editingMarker.center.x - tmpm.x, editingMarker.center.y - tmpm.y);
				pg.scale(nr.width / 206.5f);
				pg.shape(baseImageFactory.mask);
				pg.popMatrix();
				pg.endDraw();
				
				//DONE cut this imag out and save as sample
				Image img = BaseImage.TransformGrayToTransparency((BufferedImage) pg.image);
				BufferedImage img2 = BaseImage.ApplyTransparency(BaseImage.toBufferedImage(images), img);
				
				Rect rrx = editingMarker.fullrect.copy();
				Rect imgr = new Rect(0, 0, img2.getWidth(), img2.getHeight());
				rrx = rrx.intersectionRectWith(imgr);
				editingMarker.sample = img2.getSubimage(Math.round(rrx.x), Math.round(rrx.y), Math.round(rrx.width), Math.round(rrx.height));
				editingMarker.mask = img;
				
				// DEBUG extract as Marker.SetMask()?!
				//				editingMarker.mask2d = (Path2D) baseImageFactory.mask2.clone();
				//				AffineTransform aff = new AffineTransform();
				//				aff.translate(editingMarker.center.x - tmpm.x, editingMarker.center.y - tmpm.y);
				//				aff.scale(editingMarker.fullrect.width / 206.5f, editingMarker.fullrect.width / 206.5f);
				//				editingMarker.mask2d.transform(aff);
			}
			
			editingMarker = null;
			rectCornerAdjust = null;
			adjustCorner = false;
			refreshMarkers = true;
		} else if (adjustCenter && centerAdjust != null) {
			centerAdjust.set(mouseX, mouseY);
			centerAdjust.scaleSelf(1f / sf);
			
			// TODO adjust the mask to fit
			PGraphicsJava2D pg = (PGraphicsJava2D) createGraphics(images.width, images.height, JAVA2D);
			pg.beginDraw();
			pg.smooth();
			pg.background(0);
			pg.pushMatrix();
			Vec2D tmpm = baseImageFactory.maskCenter.scale(editingMarker.fullrect.width / 206.5f);
			pg.translate(editingMarker.center.x - tmpm.x, editingMarker.center.y - tmpm.y);
			pg.scale(editingMarker.fullrect.width / 206.5f);
			pg.shape(baseImageFactory.mask);
			pg.popMatrix();
			pg.endDraw();
			
			//DONE cut this imag out and save as sample
			Image img = BaseImage.TransformGrayToTransparency((BufferedImage) pg.image);
			BufferedImage img2 = BaseImage.ApplyTransparency(BaseImage.toBufferedImage(images), img);
			
			Rect rrx = editingMarker.fullrect.copy();
			Rect imgr = new Rect(0, 0, img2.getWidth(), img2.getHeight());
			rrx = rrx.intersectionRectWith(imgr);
			editingMarker.sample = img2.getSubimage(Math.round(rrx.x), Math.round(rrx.y), Math.round(rrx.width), Math.round(rrx.height));
			editingMarker.mask = img;
			
			editingMarker = null;
			rectCornerAdjust = null;
			adjustCenter = false;
			refreshMarkers = true;
		}
	}
	
	public static void main(String _args[]) {
		pargs = _args.clone();
		PApplet.main(new String[] { livingimagesmetamaker.LivingImagesMetaMaker.class.getName() });
	}
	
}