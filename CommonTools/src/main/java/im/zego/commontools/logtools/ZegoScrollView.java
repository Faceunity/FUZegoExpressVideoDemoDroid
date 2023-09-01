package im.zego.commontools.logtools;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.ScrollView;

import androidx.annotation.RequiresApi;

public class ZegoScrollView extends ScrollView {
    private int maxScrollVertical;
    public ZegoScrollView(Context context) {
        super(context);
    }

    public ZegoScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ZegoScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ZegoScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    int currentY;
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(ev.getAction() == MotionEvent.ACTION_DOWN){

            ViewParent pp = getParent().getParent();
            pp.requestDisallowInterceptTouchEvent(true);
            currentY = (int)ev.getY();
        }
        else if (ev.getAction() == MotionEvent.ACTION_UP) {
// 把滚动事件恢复给父Scrollview
            getParent().getParent().requestDisallowInterceptTouchEvent(false);
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        View child = getChildAt(0);
        if (getParent().getParent() != null) {
            if (ev.getAction() == MotionEvent.ACTION_MOVE) {
                int height = child.getMeasuredHeight();
                height = height - getMeasuredHeight();

                int scrollY = getScrollY();
                int y = (int)ev.getY();
                if (currentY < y) {
                    if (scrollY <= 0) {
// 如果向下滑动到头，就把滚动交给父Scrollview
                        getParent().getParent().requestDisallowInterceptTouchEvent(false);
                        return false;
                    } else {
                        getParent().getParent().requestDisallowInterceptTouchEvent(true);
                    }
                } else if (currentY > y) {
                    if (scrollY >= height) {
// 如果向上滑动到头，就把滚动交给父Scrollview
                        getParent().getParent().requestDisallowInterceptTouchEvent(false);
                        return false;
                    } else {
                        getParent().getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                currentY = y;
            }
        }

        return super.onTouchEvent(ev);
    }
}
