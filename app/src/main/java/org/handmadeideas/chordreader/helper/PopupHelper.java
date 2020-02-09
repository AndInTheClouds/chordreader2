package org.handmadeideas.chordreader.helper;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.PopupWindow;
import org.handmadeideas.chordreader.R;

public class PopupHelper {

	public static PopupWindow newBasicPopupWindow(Context context) {
		final PopupWindow window = new PopupWindow(context);
		
		// when a touch even happens outside of the window
		// make the window go away
		window.setTouchInterceptor(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_OUTSIDE) {
					window.dismiss();
					return true;
				}
				return false;
			}
		});
		
		window.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		window.setTouchable(true);
		window.setFocusable(true);
		window.setOutsideTouchable(true);
		
		window.setBackgroundDrawable(new BitmapDrawable());		
		
		return window;
	}
	
	/**
	 * Displays like a QuickAction from the anchor view.
	 * 
	 * @param xOffset
	 *            offset in the X direction
	 * @param yOffset
	 *            offset in the Y direction
	 */
	public static void showLikeQuickAction(PopupWindow window, View root, View anchor, WindowManager windowManager, 
			int xOffset, int yOffset, int heightOverride) {

		window.setAnimationStyle(R.style.Animations_GrowFromBottom);

		int[] location = new int[2];
		anchor.getLocationOnScreen(location);

		Rect anchorRect =
				new Rect(location[0], location[1], location[0] + anchor.getWidth(), location[1]
					+ anchor.getHeight());
		
		root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		int rootWidth = root.getMeasuredWidth();
		int rootHeight = heightOverride != -1 ? heightOverride : root.getMeasuredHeight();
		
		int xPos = anchorRect.left - rootWidth + xOffset;
		int yPos = anchorRect.top - rootHeight + yOffset;

		window.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);
	}
	
}
