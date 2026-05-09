package com.example.campusbiome;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.graphics.PathParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CampusMapView extends View {

    private float svgWidth = 213f;
    private float svgHeight = 300f;

    private Paint paint;
    private Matrix baseMatrix; // Scales the map to fit the screen bounds initially
    private Matrix interactiveMatrix; // Handles pan, zoom, rotate
    private Matrix totalMatrix; // baseMatrix * interactiveMatrix
    private Matrix inverseTotalMatrix; // For mapping touch back to raw SVG coordinates

    private List<MapElement> mapElements;
    private float baseScaleFactor;
    private float baseDx, baseDy;

    // Interactive Transformation State
    private float currentScale = 1.0f;
    private float currentRotation = 0f;
    private float translateX = 0f;
    private float translateY = 0f;

    // Touch Handling State
    private float lastTouchX;
    private float lastTouchY;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private ScaleGestureDetector scaleDetector;

    // Rotation Handling State
    private float lastAngle = 0f;

    private OnBuildingClickListener listener;
    private OnTransformChangeListener transformListener;

    private static final List<String> CLICKABLE_BUILDINGS = Arrays.asList(
            "civil_block", "Lib_block", "path7", "F_block", "D_block", "open_cafe"
    );

    public interface OnBuildingClickListener {
        void onBuildingClick(String buildingName);
    }

    public interface OnTransformChangeListener {
        void onTransformChanged(boolean isModified);
    }

    public CampusMapView(Context context) {
        super(context);
        init(context);
    }

    public CampusMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CampusMapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void setOnBuildingClickListener(OnBuildingClickListener listener) {
        this.listener = listener;
    }

    public void setOnTransformChangeListener(OnTransformChangeListener listener) {
        this.transformListener = listener;
    }

    private void init(Context context) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        baseMatrix = new Matrix();
        interactiveMatrix = new Matrix();
        totalMatrix = new Matrix();
        inverseTotalMatrix = new Matrix();

        mapElements = new ArrayList<>();

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public void setSvgString(String svgContent) {
        mapElements.clear();
        parseSvg(svgContent);
        if (getWidth() > 0 && getHeight() > 0) {
            updateBaseScaleMatrix(getWidth(), getHeight());
        }
        requestLayout();
        invalidate();
    }

    @SuppressLint("RestrictedApi")
    private void parseSvg(String svgContent) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(svgContent));

            int eventType = parser.getEventType();
            String currentCommentName = "unknown";

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();
                    if ("svg".equalsIgnoreCase(tagName)) {
                        String viewBox = parser.getAttributeValue(null, "viewBox");
                        if (viewBox != null) {
                            String[] parts = viewBox.trim().split("\\s+");
                            if (parts.length >= 4) {
                                svgWidth = Float.parseFloat(parts[2]);
                                svgHeight = Float.parseFloat(parts[3]);
                            }
                        }
                    } else if ("path".equalsIgnoreCase(tagName)) {
                        String d = parser.getAttributeValue(null, "d");
                        String fill = parser.getAttributeValue(null, "fill");

                        if (d != null) {
                            if (fill == null) fill = "#000000";
                            Path path = PathParser.createPathFromPathData(d);
                            int color = Color.parseColor(fill);
                            boolean clickable = CLICKABLE_BUILDINGS.contains(currentCommentName);
                            mapElements.add(new MapElement(currentCommentName, path, color, clickable));
                        }
                    }
                } else if (eventType == XmlPullParser.COMMENT) {
                    currentCommentName = parser.getText().trim();
                }
                eventType = parser.nextToken();
            }
        } catch (Exception e) {
            Log.e("CampusMapView", "Error parsing SVG string", e);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateBaseScaleMatrix(w, h);
    }
    
    private void updateBaseScaleMatrix(int w, int h) {
        if (mapElements.isEmpty() || w == 0 || h == 0 || svgWidth == 0 || svgHeight == 0) return;
        
        float scaleX = w / svgWidth;
        float scaleY = h / svgHeight;
        baseScaleFactor = Math.min(scaleX, scaleY);
        
        baseDx = (w - (svgWidth * baseScaleFactor)) / 2f;
        baseDy = (h - (svgHeight * baseScaleFactor)) / 2f;
        
        baseMatrix.reset();
        baseMatrix.postScale(baseScaleFactor, baseScaleFactor);
        baseMatrix.postTranslate(baseDx, baseDy);

        for (MapElement el : mapElements) {
            RectF bounds = new RectF();
            el.path.computeBounds(bounds, true);
            el.rawRegion = new Region();
            el.rawRegion.setPath(el.path, new Region((int) bounds.left, (int) bounds.top, (int) bounds.right, (int) bounds.bottom));
        }
        
        updateTotalMatrix();
    }

    public void resetTransform() {
        currentScale = 1.0f;
        currentRotation = 0f;
        translateX = 0f;
        translateY = 0f;
        interactiveMatrix.reset();
        updateTotalMatrix();
        notifyTransformChanged();
        invalidate();
    }

    private void notifyTransformChanged() {
        if (transformListener != null) {
            boolean isModified = (currentScale != 1.0f || currentRotation != 0f || translateX != 0f || translateY != 0f);
            transformListener.onTransformChanged(isModified);
        }
    }

    private void updateTotalMatrix() {
        interactiveMatrix.reset();
        interactiveMatrix.postTranslate(translateX, translateY);
        interactiveMatrix.postScale(currentScale, currentScale, getWidth() / 2f, getHeight() / 2f);
        interactiveMatrix.postRotate(currentRotation, getWidth() / 2f, getHeight() / 2f);

        totalMatrix.set(baseMatrix);
        totalMatrix.postConcat(interactiveMatrix);
        totalMatrix.invert(inverseTotalMatrix);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mapElements.isEmpty()) return;

        canvas.save();
        canvas.concat(totalMatrix);

        for (MapElement el : mapElements) {
            paint.setColor(el.color);
            canvas.drawPath(el.path, paint);

            if (el.isClickable) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.DKGRAY);
                paint.setStrokeWidth(2f / (baseScaleFactor * currentScale)); // Keep stroke width consistent
                canvas.drawPath(el.path, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        final int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final int pointerIndex = event.getActionIndex();
                final float x = event.getX(pointerIndex);
                final float y = event.getY(pointerIndex);

                lastTouchX = x;
                lastTouchY = y;
                activePointerId = event.getPointerId(0);
                
                // Handle Click
                handleClick(x, y);
                break;
            }
            
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (event.getPointerCount() == 2) {
                    lastAngle = getRotationAngle(event);
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex != -1) {
                    final float x = event.getX(pointerIndex);
                    final float y = event.getY(pointerIndex);

                    if (!scaleDetector.isInProgress() && event.getPointerCount() == 1) {
                        final float dx = x - lastTouchX;
                        final float dy = y - lastTouchY;

                        translateX += dx;
                        translateY += dy;
                        updateTotalMatrix();
                        notifyTransformChanged();
                        invalidate();
                    }

                    lastTouchX = x;
                    lastTouchY = y;
                }

                if (event.getPointerCount() == 2) {
                    float newAngle = getRotationAngle(event);
                    float angleDiff = newAngle - lastAngle;
                    
                    // Simple threshold to avoid jitter
                    if (Math.abs(angleDiff) > 0.5f) {
                        currentRotation += angleDiff;
                        currentRotation = currentRotation % 360f;
                        updateTotalMatrix();
                        notifyTransformChanged();
                        invalidate();
                    }
                    lastAngle = newAngle;
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = event.getActionIndex();
                final int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == activePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    lastTouchX = event.getX(newPointerIndex);
                    lastTouchY = event.getY(newPointerIndex);
                    activePointerId = event.getPointerId(newPointerIndex);
                }
                break;
            }
        }
        return true;
    }

    private void handleClick(float screenX, float screenY) {
        float[] pts = {screenX, screenY};
        inverseTotalMatrix.mapPoints(pts);
        
        int rawX = (int) pts[0];
        int rawY = (int) pts[1];

        for (MapElement el : mapElements) {
            if (el.isClickable && el.rawRegion != null && el.rawRegion.contains(rawX, rawY)) {
                if (listener != null) {
                    listener.onBuildingClick(el.name);
                } else {
                    Toast.makeText(getContext(), "Clicked: " + el.name, Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    private float getRotationAngle(MotionEvent event) {
        double deltaX = (event.getX(0) - event.getX(1));
        double deltaY = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(deltaY, deltaX);
        return (float) Math.toDegrees(radians);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            currentScale *= detector.getScaleFactor();
            // Restrict scaling
            currentScale = Math.max(0.5f, Math.min(currentScale, 5.0f));
            updateTotalMatrix();
            notifyTransformChanged();
            invalidate();
            return true;
        }
    }

    private static class MapElement {
        String name;
        Path path;
        int color;
        boolean isClickable;
        Region rawRegion; // Unscaled, un-transformed bounds

        MapElement(String name, Path path, int color, boolean isClickable) {
            this.name = name;
            this.path = path;
            this.color = color;
            this.isClickable = isClickable;
        }
    }
}
