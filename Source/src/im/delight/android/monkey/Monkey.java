package im.delight.android.monkey;

/**
 * Copyright 2015 delight.im <info@delight.im>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Map;
import java.util.HashMap;
import android.view.View;
import android.graphics.Rect;
import android.os.Handler;
import android.app.Activity;
import android.view.MotionEvent;
import android.os.SystemClock;
import android.view.WindowManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.os.Build;
import java.util.Random;
import java.lang.ref.WeakReference;

/** Library that generates pseudo-random streams of user input for Android apps */
public class Monkey {

	protected static final int INTERVAL_DEFAULT = 500;
	protected final Context mContext;
	@SuppressLint("UseSparseArrays")
	protected final Map<Integer, Rect> mExcludedAreas = new HashMap<Integer, Rect>();
	protected final Random mRandom = new Random();
	protected final Handler mHandler = new Handler();
	protected final Runnable mDispatcher = new Runnable() {

		@Override
		public void run() {
			if (mState == State.RUNNING && !mTarget.isEmpty()) {
				boolean hasValidCoordinates = false;
				float x = 0;
				float y = 0;

				while (!hasValidCoordinates) {
					x = Screen.getInstance(mContext).getWidth() * mRandom.nextFloat();
					y = Screen.getInstance(mContext).getHeight() * mRandom.nextFloat();

					hasValidCoordinates = true;
					for (Rect excludedArea : mExcludedAreas.values()) {
						if (excludedArea.contains((int) x, (int) y)) {
							hasValidCoordinates = false;
						}
					}
				}

				performClick(x, y);

				mHandler.postDelayed(this, mInterval);
			}
		}

	};
	protected final Target mTarget = new Target();
	protected int mInterval = INTERVAL_DEFAULT;
	protected volatile MotionEvent mEvent;
	protected volatile State mState = State.STOPPED;

	/**
	 * Creates a new monkey that executes tasks randomly
	 *
	 * @param context any valid `Context` reference
	 */
	public Monkey(final Context context) {
		mContext = context.getApplicationContext();
	}

	/**
	 * Returns the `Target` instance where `setActivity()` or `setDialog()` may be called
	 *
	 * @return the `Target` instance
	 */
	public Target getTarget() {
		return mTarget;
	}

	/**
	 * Defines a pre-determined sequence for predictability of results (must be called before `run()`)
	 *
	 * @param sequence any random `long` number that will be used as the seed for the PRNG
	 * @return this instance for chaining
	 */
	public Monkey setSequence(final long sequence) {
		mRandom.setSeed(sequence);

		return this;
	}

	/**
	 * Sets the interval between single events (in milliseconds)
	 *
	 * @param interval the interval in milliseconds
	 * @return this instance for chaining
	 */
	public Monkey setInterval(final int interval) {
		mInterval = interval;

		return this;
	}

	/**
	 * Excludes the specified rectangle of coordinates from receiving the monkey's actions
	 *
	 * @param rect the rectangle that should be excluded
	 * @return this instance for chaining
	 */
	public Monkey exclude(final Rect rect) {
		if (rect != null) {
			mExcludedAreas.put(rect.hashCode(), rect);
		}

		return this;
	}

	/**
	 * Excludes the specified `View` from receiving the monkey's actions
	 *
	 * In an `Activity`, this may not be called before `onWindowFocusChanged()`
	 *
	 * `View` instances can only be excluded if they're currently visible
	 *
	 * @param view the visible `View` that should be excluded
	 * @return this instance for chaining
	 */
	public Monkey exclude(final View view) {
		if (view != null) {
			final Rect rect = new Rect();
			if (view.getGlobalVisibleRect(rect)) {
				exclude(rect);
			}
		}

		return this;
	}

	/** Starts execution of the specified tasks */
	public void start() {
		if (mState == State.STOPPED) {
			mState = State.PAUSED;
			resume();
		}
	}

	/** Stops execution of the tasks */
	public synchronized void stop() {
		pause();
		mState = State.STOPPED;
		mTarget.clear();
	}

	public synchronized void pause() {
		mState = State.PAUSED;
		mHandler.removeCallbacks(mDispatcher);
	}

	public synchronized void resume() {
		if (mState == State.PAUSED) {
			mState = State.RUNNING;
			mHandler.post(mDispatcher);
		}
	}

	protected synchronized void performClick(final float x, final float y) {
		mEvent = MotionEvent.obtain(getTime(), getTime(), MotionEvent.ACTION_DOWN, x, y, 0);
		mTarget.dispatchTouchEvent(mEvent);

		mEvent.setAction(MotionEvent.ACTION_UP);
		mTarget.dispatchTouchEvent(mEvent);

		mEvent.recycle();
	}

	protected static long getTime() {
		return SystemClock.uptimeMillis();
	}

	/** Class that holds the target where events should be dispatched */
	public static class Target {

		protected WeakReference<Activity> mActivity;

		public void setActivity(final Activity activity) {
			if (activity != null) {
				mActivity = new WeakReference<Activity>(activity);
			}
			else {
				mActivity = null;
			}
		}

		public void dispatchTouchEvent(final MotionEvent event) {
			if (mActivity != null && mActivity.get() != null) {
				mActivity.get().dispatchTouchEvent(event);
			}
		}

		public void clear() {
			mActivity = null;
		}

		public boolean isEmpty() {
			return mActivity == null || mActivity.get() == null;
		}

	}

	/** Singleton that holds information about the device's screen */
	public static class Screen {

		protected int mWidth;
		protected int mHeight;
		protected static Screen mInstance;

		private Screen(final Context context) {
			measureScreenSize(context);
		}

		@SuppressLint("NewApi")
		@SuppressWarnings("deprecation")
		protected void measureScreenSize(final Context context) {
			final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			final Display display = wm.getDefaultDisplay();

			if (Build.VERSION.SDK_INT >= 13) {
				Point size = new Point();
				display.getSize(size);

				mWidth = size.x;
				mHeight = size.y;
			}
			else {
				mWidth = display.getWidth();
				mHeight = display.getHeight();
			}
		}

		public static Screen getInstance(final Context context) {
			if (mInstance == null) {
				mInstance = new Screen(context);
			}

			return mInstance;
		}

		public int getWidth() {
			return mWidth;
		}

		public int getHeight() {
			return mHeight;
		}

	}

	public static enum State {
		RUNNING, PAUSED, STOPPED;
	}

}
