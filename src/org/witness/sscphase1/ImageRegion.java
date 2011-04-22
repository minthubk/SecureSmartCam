package org.witness.sscphase1;

import java.io.Serializable;
import java.util.ArrayList;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;

public class ImageRegion extends FrameLayout implements OnTouchListener, OnClickListener, Serializable {

	private static final long serialVersionUID = -244965540057504061L;

	float startX;
	float startY;
	float endX;
	float endY;
	
	PointF startPoint = new PointF();

	int imageWidth;
	int imageHeight;
	
	public static final int CORNER_TOUCH_TOLERANCE = 35;
		
	public static final int NORMAL_MODE = 0;
	public static final int EDIT_MODE = 1;
	public static final int ID_MODE = 2;
	int mode = EDIT_MODE;
	
	public static final int NOTHING = 0;
	public static final int OBSCURE = 1;
	public static final int ENCRYPT = 2;
	int whattodo = NOTHING;
	
	ImageEditor imageEditor;
	
	private ArrayList<SSCSubject> knownSubjects;
	private ArrayList<String> knownKeys;
	
	QuickAction qa;
	
	public static final String SSC = "[Camera Obscura : ImageRegion] **************************** ";
	public static final String LOGTAG = SSC;

	RectF scaledImage;
	Rect scaledRect;
	
	View topLeftCorner;
	View topRightCorner;
	View bottomLeftCorner;
	View bottomRightCorner;
	View moveRegion;
	
	ActionItem editAction;
	ActionItem idAction;
	ActionItem encryptAction;
	ActionItem destroyAction;
				
	public ImageRegion(
			ImageEditor imageEditor, 
			int _scaledStartX, int _scaledStartY, 
			int _scaledEndX, int _scaledEndY, 
			int _scaledImageWidth, int _scaledImageHeight, 
			int _imageWidth, int _imageHeight, 
			int _backgroundColor) 
	{
		super(imageEditor);
		
		this.imageEditor = imageEditor;
				
		startX = (float)_imageWidth/(float)_scaledImageWidth * (float)_scaledStartX;
		startY = (float)_imageHeight/(float)_scaledImageHeight * (float)_scaledStartY;
		endX = (float)_imageWidth/(float)_scaledImageWidth * (float)_scaledEndX;
		endY = (float)_imageHeight/(float)_scaledImageHeight * (float)_scaledEndY;
		
		Log.v(LOGTAG,"startX: " + startX);
		Log.v(LOGTAG,"startY: " + startY);
		Log.v(LOGTAG,"endX: " + endX);
		Log.v(LOGTAG,"endY: " + endY);
				
		imageWidth = _imageWidth;
		imageHeight = _imageHeight;
		
		setBackgroundColor(_backgroundColor);
		
		inflatePopup();

		this.setOnClickListener(this);
		this.setOnTouchListener(this);
		
		// Inflate Layout
		LayoutInflater inflater = LayoutInflater.from(imageEditor);        
        View innerView = inflater.inflate(R.layout.imageregioninner, null);
        
        topLeftCorner = innerView.findViewById(R.id.TopLeftCorner);
        topRightCorner = innerView.findViewById(R.id.TopRightCorner);
        bottomLeftCorner = innerView.findViewById(R.id.BottomLeftCorner);
        bottomRightCorner = innerView.findViewById(R.id.BottomRightCorner);
        moveRegion = innerView.findViewById(R.id.MoveRegion);

        /*  Currently in EDIT mode
		topLeftCorner.setVisibility(View.INVISIBLE);
		topRightCorner.setVisibility(View.INVISIBLE);
		bottomLeftCorner.setVisibility(View.INVISIBLE);
		bottomRightCorner.setVisibility(View.INVISIBLE);
		*/
        
        this.addView(innerView);

        this.knownSubjects = new ArrayList<SSCSubject>();
	}
	
	public void addSubjectId(String subjectName, int subjectConsent) {
		SSCSubject subject = new SSCSubject(subjectName,subjectConsent);
		knownSubjects.add(subject);
	}
	
	public void addEncryptedKey(ArrayList<String> eKeys) {
		this.knownKeys = eKeys;
	}
	
	private void inflatePopup() {
		qa = new QuickAction(this);
		
		editAction = new ActionItem();
		editAction.setTitle("Edit");
		editAction.setIcon(this.getResources().getDrawable(R.drawable.ic_context_edit));
		editAction.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				qa.dismiss();
				ImageRegion.this.changeMode(EDIT_MODE);
			}
		});
		qa.addActionItem(editAction);

		idAction = new ActionItem();
		idAction.setTitle("ID");
		idAction.setIcon(this.getResources().getDrawable(R.drawable.ic_context_id));
		idAction.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				qa.dismiss();
				ImageRegion.this.changeMode(ID_MODE);
				imageEditor.launchIdTagger(ImageRegion.this.toString());
			}
		});
		qa.addActionItem(idAction);
		
		encryptAction = new ActionItem();
		encryptAction.setTitle("Encrypt");
		encryptAction.setIcon(this.getResources().getDrawable(R.drawable.ic_context_encrypt));
		encryptAction.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				qa.dismiss();
				whattodo = ENCRYPT;
				imageEditor.launchEncryptTagger(ImageRegion.this.toString());
			}
		});
		qa.addActionItem(encryptAction);
		
		destroyAction = new ActionItem();
		destroyAction.setTitle("Redact");
		destroyAction.setIcon(this.getResources().getDrawable(R.drawable.ic_context_destroy));
		destroyAction.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				qa.dismiss();
				whattodo = OBSCURE;
			}
		});
		qa.addActionItem(destroyAction);
	}
	
	public void changeMode(int newMode) {
		mode = newMode;
		if (mode == EDIT_MODE) {
			topLeftCorner.setVisibility(View.VISIBLE);
			topRightCorner.setVisibility(View.VISIBLE);
			bottomLeftCorner.setVisibility(View.VISIBLE);
			bottomRightCorner.setVisibility(View.VISIBLE);
		} else if (mode == NORMAL_MODE) {
			topLeftCorner.setVisibility(View.GONE);
			topRightCorner.setVisibility(View.GONE);
			bottomLeftCorner.setVisibility(View.GONE);
			bottomRightCorner.setVisibility(View.GONE);
		}
	}
	
	public Rect getScaledRect(int _scaledImageWidth, int _scaledImageHeight) {
		
		float scaledStartX = (float)startX * (float)_scaledImageWidth/(float)imageWidth;
		float scaledStartY = (float)startY * (float)_scaledImageHeight/(float)imageHeight;
		float scaledEndX = (float)endX * (float)_scaledImageWidth/(float)imageWidth;
		float scaledEndY = (float)endY * (float)_scaledImageHeight/(float)imageHeight;
		
		return new Rect((int)scaledStartX, (int)scaledStartY, (int)scaledEndX, (int)scaledEndY);
	}
	
	public boolean onTouch(View v, MotionEvent event) {
		boolean handled = false;

		if (mode == EDIT_MODE) {
			Log.v(LOGTAG,"onTouch mode EDIT");
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
				
				case MotionEvent.ACTION_DOWN:
						startPoint = new PointF(event.getX(),event.getY());
	
						scaledImage = imageEditor.getScaleOfImage();
						scaledRect = getScaledRect((int)scaledImage.width(), (int)scaledImage.height());
						
						// To do it with coordinates.. Things are just ever so slightly off
						
						Log.v(LOGTAG,"startPoint.x: " + startPoint.x + " startPoint.y: " + startPoint.y);
						Log.v(LOGTAG,"scaledRect.left: " + scaledRect.left + " scaledRect.right: " + scaledRect.right);
						Log.v(LOGTAG,"scaledRect.top: " + scaledRect.top + " scaledRect.bottom: " + scaledRect.bottom);
						Log.v(LOGTAG,"moveRegion.left(): " + this.getLeft() + " moveRegion.right(): " + this.getRight());
						Log.v(LOGTAG,"moveRegion.top()" + this.getTop() + " moveRegion.bottom()" + this.getBottom());
						handled = true;
					break;
				
				case MotionEvent.ACTION_UP:
					break;
				
				case MotionEvent.ACTION_MOVE:
					Log.v(LOGTAG,"Action Move");
						float xdist = startPoint.x - event.getX();
						float ydist = startPoint.y - event.getY();

						if (v == topLeftCorner || 
								event.getX() < CORNER_TOUCH_TOLERANCE &&
								event.getY() < CORNER_TOUCH_TOLERANCE
								) {
							// Here we expand
							Log.v(LOGTAG,"TOP LEFT CORNER");
							scaledRect.left = scaledRect.left - (int)xdist;
							scaledRect.top = scaledRect.top - (int)ydist;
							handled = true;
						} else if (v == topRightCorner ||
								event.getX() > this.getWidth() - CORNER_TOUCH_TOLERANCE &&
								event.getY() < CORNER_TOUCH_TOLERANCE
						) {
							// Here we expand
							Log.v(LOGTAG,"TOP RIGHT CORNER");
							scaledRect.top = scaledRect.top - (int)ydist;
							scaledRect.right = scaledRect.right - (int)xdist;
							handled = true;
						} else if (v == bottomLeftCorner || (
								event.getX() < CORNER_TOUCH_TOLERANCE &&
								event.getY() > this.getHeight() - CORNER_TOUCH_TOLERANCE
						)) {
							// Here we expand
							Log.v(LOGTAG,"BOTTOM LEFT CORNER");
							scaledRect.left = scaledRect.left - (int)xdist;
							scaledRect.bottom = scaledRect.bottom - (int)ydist;			
							handled = true;
						} else if (v == bottomRightCorner || (
								event.getX() > this.getWidth() - CORNER_TOUCH_TOLERANCE &&
								event.getY() > this.getHeight() - CORNER_TOUCH_TOLERANCE
						)) {
							// Here we expand
							Log.v(LOGTAG,"BOTTOM RIGHT CORNER");
							scaledRect.right = scaledRect.right - (int)xdist;
							scaledRect.bottom = scaledRect.bottom - (int)ydist;
							handled = true;
						} else if (v == moveRegion || (
								event.getX() < this.getWidth() - CORNER_TOUCH_TOLERANCE &&
								event.getX() > CORNER_TOUCH_TOLERANCE &&
								event.getY() < this.getHeight() - CORNER_TOUCH_TOLERANCE &&
								event.getY() > CORNER_TOUCH_TOLERANCE						
						)) {
							Log.v(LOGTAG,"MOVE REGION " + xdist + " " + ydist);
							scaledRect.left = scaledRect.left - (int)xdist;
							scaledRect.top = scaledRect.top - (int)ydist;
							scaledRect.right = scaledRect.right - (int)xdist;
							scaledRect.bottom = scaledRect.bottom - (int)ydist;
							handled = true;
						}
						 
						if (handled == true) {
							startX = (float)scaledRect.left * (float)imageWidth/(float)scaledImage.width();
							startY = (float)scaledRect.top * (float)imageHeight/(float)scaledImage.height();
							endX = (float)scaledRect.right * (float)imageWidth/(float)scaledImage.width();
							endY = (float)scaledRect.bottom * (float)imageHeight/(float)scaledImage.height();
	
							imageEditor.redrawRegions();
						}
					break;				
			}
		}
		return true;
	}
	
	/*
	public boolean onTouchEvent(MotionEvent ev) {
		Log.v(LOGTAG,"onTouchEvent");
		return false;
	}
	*/
	/*
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		Log.v(LOGTAG,"onIntercept");
		return false;
	}
	*/
	public void onClick(View v) {
		Log.d(SSC,"CLICKED View " + v.toString());
		if (v == this || v == moveRegion) {
			/*
			qa = new QuickAction(v);
			for(int x=0;x<aiList.size();x++) {
				qa.addActionItem(aiList.get(x));
			}
			*/
			//qa.setAnimStyle(QuickAction.ANIM_REFLECT);
			qa.show();
		}
	}
	
	public class SSCSubject {
		String subjectName;
		int subjectConsent;
		
		SSCSubject(String sn, int sc) {
			this.subjectName = sn;
			this.subjectConsent = sc;
		}
	}
}
