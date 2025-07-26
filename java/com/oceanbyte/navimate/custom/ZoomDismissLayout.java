package com.oceanbyte.navimate.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.core.view.ViewCompat;

import com.oceanbyte.navimate.R;

public class ZoomDismissLayout extends FrameLayout {

    private float downX;
    private float downY;
    private float translationY;
    private static final float DISMISS_THRESHOLD = 300f;
    private float dismissThreshold = DISMISS_THRESHOLD;
    private String accessibilityDescription = "Swipe down to dismiss";
    private OnDismissListener onDismissListener;

    public ZoomDismissLayout(Context context) {
        super(context);
        init();
    }

    // Allow threshold to be set via XML
    public ZoomDismissLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (attrs != null) {
            try (android.content.res.TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ZoomDismissLayout)) {
                dismissThreshold = a.getDimension(R.styleable.ZoomDismissLayout_dismissThreshold, DISMISS_THRESHOLD);
                accessibilityDescription = a.getString(R.styleable.ZoomDismissLayout_accessibilityDescription);
            }
        }
        init();
    }

    public ZoomDismissLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        setContentDescription(accessibilityDescription);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) return false;
        float dy;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downY = event.getRawY();
                sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                return true;
            case MotionEvent.ACTION_MOVE:
                dy = event.getRawY() - downY;
                if (dy > 0) {
                    translationY = dy;
                    setTranslationY(translationY);
                    float alpha = 1 - (translationY / getHeight());
                    setAlpha(Math.max(0f, Math.min(1f, alpha)));
                    ViewCompat.postInvalidateOnAnimation(this);
                    if (dragProgressListener != null) {
                        dragProgressListener.onDragProgress(Math.max(0f, Math.min(1f, translationY / dismissThreshold)));
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                boolean dismissed = false;
                if (translationY > dismissThreshold || (velocityTracker != null && velocityTracker.getYVelocity() > 2000)) {
                    if (onDismissListener != null) {
                        onDismissListener.onDismiss();
                    }
                    sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT);
                    dismissed = true;
                }
                if (!dismissed) {
                    animate().translationY(0).alpha(1f)
                        .setDuration(200)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
                }
                translationY = 0;
                setAlpha(1f);
                if (dragProgressListener != null) {
                    dragProgressListener.onDragProgress(0f);
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    // Velocity tracking for fast swipe
    private android.view.VelocityTracker velocityTracker;
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev == null) return false;
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                velocityTracker = android.view.VelocityTracker.obtain();
                velocityTracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - downX);
                float dy = Math.abs(ev.getY() - downY);
                boolean isVerticalSwipe = dy > dx && dy > 10;
                if (isVerticalSwipe) {
                    if (velocityTracker != null) velocityTracker.addMovement(ev);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public void setContentDescription(CharSequence contentDescription) {
        super.setContentDescription(contentDescription);
        this.accessibilityDescription = contentDescription != null ? contentDescription.toString() : "";
    }

    /**
     * Set the dismiss threshold in pixels.
     */
    public void setDismissThreshold(float thresholdPx) {
        this.dismissThreshold = thresholdPx;
    }

    /**
     * Set the accessibility description for screen readers.
     */
    public void setAccessibilityDescription(String description) {
        setContentDescription(description);
    }

    /**
     * Set the OnDismissListener. Null-safe.
     */
    public void setOnDismissListener(OnDismissListener listener) {
        this.onDismissListener = listener;
    }

    /**
     * Listener for dismiss events.
     */
    public interface OnDismissListener {
        void onDismiss();
    }

    // Drag progress listener
    public interface OnDragProgressListener {
        void onDragProgress(float progress); // 0..1
    }
    private OnDragProgressListener dragProgressListener;
    public void setOnDragProgressListener(OnDragProgressListener listener) {
        this.dragProgressListener = listener;
    }
}
