// DoodleView.java
package com.mandiecohoon.doodle;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.print.PrintHelper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class DoodleView extends View {   
   private static final float TOUCH_TOLERANCE = 10;

   private Bitmap bitmap;
   private Canvas bitmapCanvas;
   private final Paint paintScreen;
   private final Paint paintLine; 
   private int backgroundColor;
   
   private final Map<Integer, Path> pathMap = new HashMap<Integer, Path>(); 
   private final Map<Integer, Point> previousPointMap = new HashMap<Integer, Point>();
    
   private GestureDetector singleTapDetector; 
      
   public DoodleView(Context context, AttributeSet attrs) {
      super(context, attrs);
      paintScreen = new Paint();

      paintLine = new Paint();
      paintLine.setAntiAlias(true); 
      paintLine.setColor(Color.BLACK);
      paintLine.setStyle(Paint.Style.STROKE);
      paintLine.setStrokeWidth(5);
      paintLine.setStrokeCap(Paint.Cap.ROUND);
      
      singleTapDetector = new GestureDetector(getContext(), singleTapListener);
      
   } 

   @Override 
   public void onSizeChanged(int w, int h, int oldW, int oldH) {
      bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
      bitmapCanvas = new Canvas(bitmap);
      bitmap.eraseColor(backgroundColor);
   } 
   
   public void clear() {
      pathMap.clear();
      previousPointMap.clear();
      bitmap.eraseColor(backgroundColor);
      invalidate();
   }
   
   public void setDrawingColor(int color) {
      paintLine.setColor(color);
   } 

   public int getDrawingColor() {
      return paintLine.getColor();
   }

   public void setLineWidth(int width) {
      paintLine.setStrokeWidth(width);
   } 

   public int getLineWidth() {
      return (int) paintLine.getStrokeWidth();
   } 
   
   public void setBackgroundColor(int color) {
	   backgroundColor = color;
   }
   public int getBackgroundColor() {
	   return backgroundColor;
   }
   
   @Override
   protected void onDraw(Canvas canvas) {
      canvas.drawBitmap(bitmap, 0, 0, paintScreen);

      for (Integer key : pathMap.keySet()) 
         canvas.drawPath(pathMap.get(key), paintLine);
   } 

   public void hideSystemBars() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
         setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | 
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE);
   }

   public void showSystemBars() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
         setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
   }

   private SimpleOnGestureListener singleTapListener = new SimpleOnGestureListener() {
         @Override
         public boolean onSingleTapUp(MotionEvent e) {
            if ((getSystemUiVisibility() & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
               //hideSystemBars();
            } else {
               //showSystemBars();
            }
            return true;
         }            
      };

   @Override
   public boolean onTouchEvent(MotionEvent event) {     
      if (singleTapDetector.onTouchEvent(event))
         return true;
      
      int action = event.getActionMasked(); 
      int actionIndex = event.getActionIndex();
      
      if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
         touchStarted(event.getX(actionIndex), event.getY(actionIndex), event.getPointerId(actionIndex));
      } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
         touchEnded(event.getPointerId(actionIndex));
      } else {
         touchMoved(event); 
      }
      
      invalidate();
      return true;
   }

   private void touchStarted(float x, float y, int lineID) {
      Path path; 
      Point point;

      if (pathMap.containsKey(lineID)) {
         path = pathMap.get(lineID);
         path.reset();
         point = previousPointMap.get(lineID);
      } else {
         path = new Path(); 
         pathMap.put(lineID, path);
         point = new Point();
         previousPointMap.put(lineID, point);
      }
      
      path.moveTo(x, y);
      point.x = (int) x;
      point.y = (int) y;
   }

   private void touchMoved(MotionEvent event) {
      for (int i = 0; i < event.getPointerCount(); i++) {
         int pointerID = event.getPointerId(i);
         int pointerIndex = event.findPointerIndex(pointerID);
            
         if (pathMap.containsKey(pointerID)) {
            float newX = event.getX(pointerIndex);
            float newY = event.getY(pointerIndex);
            
            Path path = pathMap.get(pointerID);
            Point point = previousPointMap.get(pointerID);
            
            float deltaX = Math.abs(newX - point.x);
            float deltaY = Math.abs(newY - point.y);

            if (deltaX >= TOUCH_TOLERANCE || deltaY >= TOUCH_TOLERANCE) {
               path.quadTo(point.x, point.y, (newX + point.x) / 2,
                  (newY + point.y) / 2);

               point.x = (int) newX;
               point.y = (int) newY;
            } 
         } 
      }
   }

   private void touchEnded(int lineID) {
      Path path = pathMap.get(lineID);
      bitmapCanvas.drawPath(path, paintLine);
      path.reset();
   } 

   public void saveImage() {
      String name = "Doodlz" + System.currentTimeMillis() + ".jpg";
      
      String location = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), bitmap, name, "Doodlz Drawing");

      if (location != null) {
         Toast message = Toast.makeText(getContext(), R.string.message_saved, Toast.LENGTH_SHORT);
         message.setGravity(Gravity.CENTER, message.getXOffset() / 2, message.getYOffset() / 2);
         message.show();
      } else {
         Toast message = Toast.makeText(getContext(), R.string.message_error_saving, Toast.LENGTH_SHORT);
         message.setGravity(Gravity.CENTER, message.getXOffset() / 2, message.getYOffset() / 2);
         message.show(); 
      } 
   }
   
   public void printImage() {
      if (PrintHelper.systemSupportsPrint()) {
         PrintHelper printHelper = new PrintHelper(getContext());
         
         printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
         printHelper.printBitmap("Doodlz Image", bitmap); 
      } else {
         Toast message = Toast.makeText(getContext(), 
            R.string.message_error_printing, Toast.LENGTH_SHORT);
         message.setGravity(Gravity.CENTER, message.getXOffset() / 2, message.getYOffset() / 2);
         message.show(); 
      }
   }
}