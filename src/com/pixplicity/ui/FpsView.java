package com.pixplicity.ui;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class FpsView extends TextView {

	private final class FpsHistoryTask extends TimerTask {

		@Override
		public void run() {
			synchronized (mFpsHistory) {
				double fps;
				if (mLastFrame < System.currentTimeMillis() - 500) {
					// Last frame was so long ago that the framerate must now be 0
					fps = 0;
				} else {
					fps = getFps();
				}
				if (mFpsHistory.size() > mHistorySize) {
					mFpsHistory.removeFirst();
				}
				Log.d("FpsView", "history: " + fps);
				mFpsHistory.add(fps);
			}
		}
	}

	private static final double FPS_WEIGHT_RATE = 1d / 60;
	private static final double FPS_WEIGHT_RATIO = 2d / 60;
	protected static final int DEFAULT_HISTORY_SIZE = 30;

	private final NumberFormat mFormatter = DecimalFormat.getInstance(Locale.ENGLISH);
	{
		setFractionDigits(0);
	}

	private int mHistorySize = DEFAULT_HISTORY_SIZE;
	private double mFpsWeightRatio = 1d;
	private long mLastFrame;
	private double mMsPerFrame;

	private final LinkedList<Double> mFpsHistory = new LinkedList<Double>();

	private final Runnable mFpsRunner = new Runnable() {

		@Override
		public void run() {
			String fps = mFormatter.format(getFps());
			setText(fps + " fps");
		}
	};
	private final Timer mTimer = new Timer();
	private TimerTask mFpsHistoryTask;
	private final RectF mRect = new RectF();
	private final Paint mPaint = new Paint();
	private boolean mPaused;

	public FpsView(Context _context) {
		super(_context);
		init(_context, null, 0);
	}

	public FpsView(Context _context, AttributeSet _attrs) {
		super(_context, _attrs);
		init(_context, _attrs, 0);
	}

	public FpsView(Context _context, AttributeSet _attrs, int _defStyle) {
		super(_context, _attrs, _defStyle);
		init(_context, _attrs, _defStyle);
	}

	private void init(Context _context, AttributeSet _attrs, int _defStyle) {
		mPaint.setColor(Color.rgb(255, 0, 0));
	}

	@Override
	protected void onDraw(Canvas _canvas) {
		double duration = (System.currentTimeMillis() - mLastFrame)
				* mFpsWeightRatio;
		if (mFpsWeightRatio > FPS_WEIGHT_RATIO) {
			mFpsWeightRatio -= FPS_WEIGHT_RATE;
			if (mFpsWeightRatio < FPS_WEIGHT_RATIO) {
				mFpsWeightRatio = FPS_WEIGHT_RATIO;
			}
		}
		mMsPerFrame = mMsPerFrame * (1.0d - mFpsWeightRatio) + duration;
		mLastFrame = System.currentTimeMillis();
		int startIndex = mHistorySize - mFpsHistory.size();
		float width = (float) _canvas.getWidth() / (float) mHistorySize;
		mRect.bottom = _canvas.getHeight();
		synchronized (mFpsHistory) {
			for (int x = startIndex; x < mHistorySize; x++) {
				Double fps = Math.max(0, Math.min(60, mFpsHistory.get(x - startIndex)));
				double fpsWarn = Math.max(0, fps - 20);
				double r = Math.pow((40d - fpsWarn) / 40d, 0.5);
				double g = Math.pow(fpsWarn / 40d, 0.5);
				mPaint.setColor(Color.argb(
						64,
						(int) (255 * r),
						(int) (255 * g),
						0));
				mRect.left = x * width;
				mRect.right = mRect.left + width;
				mRect.top = mRect.bottom - (float) (mRect.bottom * (fps / 60d));
				_canvas.drawRect(mRect, mPaint);
			}
		}
		super.onDraw(_canvas);
		if (mFpsHistoryTask != null) {
			nextFrame();
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	protected void nextFrame() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			postOnAnimation(mFpsRunner);
		} else {
			post(mFpsRunner);
		}
	}

	protected double getFps() {
		if (mMsPerFrame > 0d) {
			return 1000d / mMsPerFrame;
		}
		return 0d;
	}

	@Override
	protected void onWindowVisibilityChanged(int _visibility) {
		super.onWindowVisibilityChanged(_visibility);
		switch (_visibility) {
		case View.VISIBLE:
			if (!mPaused) {
				resume(false);
			}
			break;
		case View.INVISIBLE:
		case View.GONE:
			if (!mPaused) {
				pause(false);
			}
			break;
		}
	}

	public void resume() {
		resume(true);
	}

	protected void resume(boolean _switch) {
		if (_switch) {
			mPaused = false;
		}
		if (mFpsHistoryTask == null) {
			mFpsHistoryTask = new FpsHistoryTask();
			mTimer.scheduleAtFixedRate(mFpsHistoryTask, 1000, 1000);
		}
	}

	public void pause() {
		pause(true);
	}

	protected void pause(boolean _switch) {
		if (_switch) {
			mPaused = true;
		}
		if (mFpsHistoryTask != null) {
			removeCallbacks(mFpsRunner);
			mFpsHistoryTask.cancel();
			mFpsHistoryTask = null;
		}
	}

	public void setHistorySize(int historySize) {
		mHistorySize = historySize;
	}

	public void setFractionDigits(int fractionDigits) {
		mFormatter.setMinimumFractionDigits(fractionDigits);
		mFormatter.setMaximumFractionDigits(fractionDigits);
	}

}
