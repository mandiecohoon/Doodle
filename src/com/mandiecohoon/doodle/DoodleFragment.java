// DoodleFragment.java
// Fragment in which the DoodleView is displayed
package com.mandiecohoon.doodle;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class DoodleFragment extends Fragment {
   private DoodleView doodleView;
   private float acceleration; 
   private float currentAcceleration; 
   private float lastAcceleration; 
   private boolean dialogOnScreen = false;
   
   private static final int RESULT_LOAD_IMAGE = 1;
   private static View vw;
   private static Boolean imgFlag = false;
   
   private static final int ACCELERATION_THRESHOLD = 100000;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      super.onCreateView(inflater, container, savedInstanceState);    
      View view = inflater.inflate(R.layout.fragment_doodle, container, false);
           
      setHasOptionsMenu(true);
      vw = view;
      final DoodleView DoodleView = getDoodleView();
      doodleView = (DoodleView) view.findViewById(R.id.doodleView);
      acceleration = 0.00f; 
      currentAcceleration = SensorManager.GRAVITY_EARTH;    
      lastAcceleration = SensorManager.GRAVITY_EARTH;
      return view;
   }
     
   public void clearBackground() {
	   ImageView iv = (ImageView) getView().findViewById(R.id.imageView);
       iv.setImageResource(android.R.color.transparent);
       iv.setBackgroundColor(Color.WHITE);
       imgFlag = false;
   }
   
   public static void setImageViewBackgroundColor(int color) {
	   ImageView iv = (ImageView) vw.findViewById(R.id.imageView);
	   iv.setImageResource(android.R.color.transparent);
	   iv.setBackgroundColor(color);
   }
   
   public static void setImgFlag() {
	   imgFlag = false;
   }
   
   @Override
   public void onStart() {
      super.onStart();
      enableAccelerometerListening();           
   }

   public void enableAccelerometerListening() {
      SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

      sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
   } 
   
   @Override
   public void onPause() {
      super.onPause();
      disableAccelerometerListening();
   }
 
   public void disableAccelerometerListening() {
      SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

      sensorManager.unregisterListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));         
   } 

   private SensorEventListener sensorEventListener = new SensorEventListener() {
         @Override
         public void onSensorChanged(SensorEvent event) {  
            if (!dialogOnScreen) {
               float x = event.values[0];
               float y = event.values[1];
               float z = event.values[2];
      
               lastAcceleration = currentAcceleration;
      
               currentAcceleration = x * x + y * y + z * z;
      
               acceleration = currentAcceleration * (currentAcceleration - lastAcceleration);
      
               if (acceleration > ACCELERATION_THRESHOLD)
            	   confirmErase();
            } 
         }
      
         @Override
         public void onAccuracyChanged(Sensor sensor, int accuracy)
         {
         } 
      };
   
   private void confirmErase() {
      EraseImageDialogFragment fragment = new EraseImageDialogFragment();
      fragment.show(getFragmentManager(), "erase dialog");
   }

   @Override
   public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
      super.onCreateOptionsMenu(menu, inflater);
      inflater.inflate(R.menu.doodle_fragment_menu, menu);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         case R.id.color:
            ColorDialogFragment colorDialog = new ColorDialogFragment();      
            colorDialog.show(getFragmentManager(), "color dialog");
            return true;
         case R.id.lineWidth:
            LineWidthDialogFragment widthdialog = new LineWidthDialogFragment();      
            widthdialog.show(getFragmentManager(), "line width dialog");
            return true;
         case R.id.eraser:
            doodleView.setDrawingColor(doodleView.getBackgroundColor(imgFlag), 0, 0, 0);
            return true;
         case R.id.clear:
            confirmErase();
            return true;
         case R.id.save:     
            doodleView.saveImage();
            return true;
         case R.id.print:     
            doodleView.printImage();
            return true;
         case R.id.backgroundImg:
        	 Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        	 startActivityForResult(i, RESULT_LOAD_IMAGE);
        	 imgFlag = true;
             return true;
      }

      return super.onOptionsItemSelected(item);
   }
   
   public DoodleView getDoodleView() {
      return doodleView;
   }

   public void setDialogOnScreen(boolean visible) {
      dialogOnScreen = visible;  
   }
   
   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent data) {
       super.onActivityResult(requestCode, resultCode, data);
       if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data) {
           Uri selectedImage = data.getData();
           String[] filePathColumn = { MediaStore.Images.Media.DATA };
           
           Cursor cursor = getActivity().getContentResolver().query(selectedImage,filePathColumn, null, null, null);
           cursor.moveToFirst();
           
           int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
           String picturePath = cursor.getString(columnIndex);
           cursor.close();
           
           ImageView imageView = (ImageView) vw.findViewById(R.id.imageView);
           imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
       }
   }
}