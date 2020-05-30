package com.wyc.exp2;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

public class BouncingBallActivity extends Activity implements SensorEventListener{

	// sensor-related
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;

	// animated view
	private ShapeView mShapeView;

	// screen size
	private int mWidthScreen;
	private int mHeightScreen;


	// motion parameters
	private final float FACTOR_FRICTION = 0.5f; // imaginary friction on the screen
	private final float GRAVITY = 9.8f; // acceleration of gravity
	private float mAx; // acceleration along x axis
	private float mAy; // acceleration along y axis
	private final float mDeltaT = 0.5f; // imaginary time interval between each acceleration updates

	// 小球的颜色
	private int mBallColor = 0xFF78838B; // 默认色
	// 小球的数组
	private final int[] colorList = {
			0xFF78838B,
			0xFFDCF5F5,
			0xFFCDEBFF,
			0xFFFF0000,
			0xFF2A2AA5,
			0xFF87B8DE,
			0xFFA09E5F,
			0xFF00FF7F,
			0xFF7295EE,
			0xFFE0FFFF,
			0xFF621C8B,
			0xFFFF82AB,
			0xFF800000,
			0xFFCBC0FF,
			0xFF0000FF,
			0xFFCDA66C
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set the screen always portait
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// initializing sensors
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		// obtain screen width and height
		Display display = ((WindowManager)this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		mWidthScreen = display.getWidth();
		mHeightScreen = display.getHeight() + 80;


	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// obtain the three accelerations from sensors
		mAx = event.values[0];
		mAy = event.values[1];

		float mAz = event.values[2];

		// taking into account the frictions
		mAx = Math.signum(mAx) * Math.abs(mAx) * (1 - FACTOR_FRICTION * Math.abs(mAz) / GRAVITY);
		mAy = Math.signum(mAy) * Math.abs(mAy) * (1 - FACTOR_FRICTION * Math.abs(mAz) / GRAVITY);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// initializing the view that renders the ball
		mShapeView = new ShapeView(this);
		mShapeView.setOvalCenter((int)(mWidthScreen * 0.6), (int)(mHeightScreen * 0.6));

		setContentView(mShapeView);
		// start sensor sensing
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// stop senser sensing
		// mSensorManager.unregisterListener(this);
	}

	protected void onDestory() {
		super.onDestroy();
		mSensorManager.unregisterListener(this);
	}

	// the view that renders the ball
	private class ShapeView extends SurfaceView implements SurfaceHolder.Callback{

		private final int RADIUS = 50;
		private final float FACTOR_BOUNCEBACK = 0.75f;

		// 小球中心，可以用来判断屏幕边缘碰撞
		private int mXCenter;
		private int mYCenter;
		private RectF mRectF;
		private final Paint mPaint;
		private ShapeThread mThread;

		private float mVx;
		private float mVy;

		private boolean destroyed = false;

		public ShapeView(Context context) {
			super(context);

			getHolder().addCallback(this);
			mThread = new ShapeThread(getHolder(), this);
			setFocusable(true);

			mPaint = new Paint();
			mPaint.setColor(0xFFFFFFFF);
			mPaint.setAlpha(192);
			mPaint.setStyle(Paint.Style.FILL);
			mPaint.setAntiAlias(true);

			mRectF = new RectF();
		}

		// set the position of the ball
		public boolean setOvalCenter(int x, int y)
		{
			mXCenter = x;
			mYCenter = y;
			return true;
		}

		// calculate and update the ball's position
		public boolean updateOvalCenter()
		{
			mVx -= mAx * mDeltaT;
			mVy += mAy * mDeltaT;

			mXCenter += (int)(mDeltaT * (mVx + 0.5 * mAx * mDeltaT));
			mYCenter += (int)(mDeltaT * (mVy + 0.5 * mAy * mDeltaT));

			// 触碰左侧
			if(mXCenter < RADIUS)
			{
				mXCenter = RADIUS;
				mVx = -mVx * FACTOR_BOUNCEBACK;
				updateBallColor();
			}
			// 触碰上方
			if(mYCenter < RADIUS)
			{  mYCenter = RADIUS;  mVy = -mVy * FACTOR_BOUNCEBACK;
				updateBallColor(); }
			// 触碰右侧
			if(mXCenter > mWidthScreen - RADIUS)
			{
				mXCenter = mWidthScreen - RADIUS;
				mVx = -mVx * FACTOR_BOUNCEBACK;
				updateBallColor();
			}
			// 触碰下方
			if(mYCenter > mHeightScreen - 2 * RADIUS)
			{
				mYCenter = mHeightScreen - 2 * RADIUS;
				mVy = -mVy * FACTOR_BOUNCEBACK;
				updateBallColor();
			}

			return true;
		}

		// 判断是否触及屏幕边缘并修改小球颜色
		public void updateBallColor(){
			mBallColor += colorList[(int)(1+Math.random()*(15-1+1))];
		}

		// update the canvas
		protected void onDraw(Canvas canvas)
		{
			if(mRectF != null && destroyed == false )
			{
				mRectF.set(mXCenter - RADIUS, mYCenter - RADIUS, mXCenter + RADIUS, mYCenter + RADIUS);
				canvas.drawColor(0xFF000000); // 屏幕底色
				mPaint.setColor(mBallColor); // 设置小球颜色
				canvas.drawOval(mRectF, mPaint);
			}
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			mThread.setRunning(true);
			mThread.start();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			destroyed = true;
			boolean retry = true;
			mThread.setRunning(false);
			while(retry)
			{
				try{
					mThread.join();
					retry = false;
				} catch (InterruptedException e){

				}
			}
		}
	}
	class ShapeThread extends Thread {
		private SurfaceHolder mSurfaceHolder;
		private ShapeView mShapeView;
		private boolean mRun = false;

		public ShapeThread(SurfaceHolder surfaceHolder, ShapeView shapeView) {
			mSurfaceHolder = surfaceHolder;
			mShapeView = shapeView;
		}

		public void setRunning(boolean run) {
			mRun = run;
		}

		public SurfaceHolder getSurfaceHolder() {
			return mSurfaceHolder;
		}

		@Override
		public void run() {
			Canvas c;
			while (mRun) {
				mShapeView.updateOvalCenter();
				c = null;
				try {
					c = mSurfaceHolder.lockCanvas(null);
					synchronized (mSurfaceHolder) {
						mShapeView.onDraw(c);
					}
				} finally {
					if (c != null) {
						mSurfaceHolder.unlockCanvasAndPost(c);
					}
				}
			}
		}
	}
}