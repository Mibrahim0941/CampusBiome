package com.example.campusbiome;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.Base64;
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
    private OnMapTapListener tapListener;
    
    private List<com.example.campusbiome.models.WifiRouter> wifiRouters = new ArrayList<>();
    private java.util.Map<String, Integer> buildingDensities = new java.util.HashMap<>();

    private static final List<String> CLICKABLE_BUILDINGS = Arrays.asList(
            "civil_block", "Lib_block", "F_block", "D_block",
            "A_block", "B_block", "C_block", "E_block", "G_block", "H_block", "admin_block",
            "library_block"
    );

    public interface OnBuildingClickListener {
        void onBuildingClick(String buildingName);
    }

    public interface OnTransformChangeListener {
        void onTransformChanged(boolean isModified);
    }

    public interface OnMapTapListener {
        void onMapTap(float rawX, float rawY);
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

    public void setOnMapTapListener(OnMapTapListener listener) {
        this.tapListener = listener;
    }

    public void setWifiRouters(List<com.example.campusbiome.models.WifiRouter> routers) {
        this.wifiRouters = routers;
        invalidate();
    }

    public void setBuildingDensities(java.util.Map<String, Integer> densities) {
        this.buildingDensities = densities;
        invalidate();
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
                    } else if ("image".equalsIgnoreCase(tagName)) {
                        String href = parser.getAttributeValue(null, "href");
                        if (href == null) {
                            href = parser.getAttributeValue("http://www.w3.org/1999/xlink", "href");
                        }
                        if (href == null) {
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                String attrName = parser.getAttributeName(i);
                                if (attrName != null && attrName.contains("href")) {
                                    href = parser.getAttributeValue(i);
                                    break;
                                }
                            }
                        }

                        if (href != null && href.startsWith("data:image/")) {
                            int commaIndex = href.indexOf(',');
                            if (commaIndex != -1) {
                                String base64Data = href.substring(commaIndex + 1);
                                // Strip any whitespace, newlines, or carriage returns just in case
                                base64Data = base64Data.replaceAll("\\s+", "");
                                byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                
                                if (bitmap != null) {
                                    String xStr = parser.getAttributeValue(null, "x");
                                    String yStr = parser.getAttributeValue(null, "y");
                                    String wStr = parser.getAttributeValue(null, "width");
                                    String hStr = parser.getAttributeValue(null, "height");
                                    
                                    float x = xStr != null ? Float.parseFloat(xStr) : 0f;
                                    float y = yStr != null ? Float.parseFloat(yStr) : 0f;
                                    float w = wStr != null ? Float.parseFloat(wStr) : bitmap.getWidth();
                                    float h = hStr != null ? Float.parseFloat(hStr) : bitmap.getHeight();
                                    
                                    mapElements.add(new MapElement(currentCommentName, bitmap, new RectF(x, y, x + w, y + h)));
                                }
                            }
                        }
                    } else if ("text".equalsIgnoreCase(tagName)) {
                        String xStr = parser.getAttributeValue(null, "x");
                        String yStr = parser.getAttributeValue(null, "y");
                        String fill = parser.getAttributeValue(null, "fill");
                        String fontSize = parser.getAttributeValue(null, "font-size");
                        String textAnchor = parser.getAttributeValue(null, "text-anchor");
                        String transform = parser.getAttributeValue(null, "transform");

                        float x = xStr != null ? Float.parseFloat(xStr) : 0f;
                        float y = yStr != null ? Float.parseFloat(yStr) : 0f;
                        int color = fill != null ? Color.parseColor(fill) : Color.BLACK;
                        float size = fontSize != null ? Float.parseFloat(fontSize) : 12f;

                        float rot = 0f, rotX = 0f, rotY = 0f;
                        if (transform != null && transform.startsWith("rotate(")) {
                            try {
                                String inner = transform.substring(7, transform.length() - 1);
                                String[] parts = inner.split(",");
                                if (parts.length >= 1) rot = Float.parseFloat(parts[0].trim());
                                if (parts.length >= 3) {
                                    rotX = Float.parseFloat(parts[1].trim());
                                    rotY = Float.parseFloat(parts[2].trim());
                                }
                            } catch (Exception e) {
                                Log.e("CampusMapView", "Error parsing text transform", e);
                            }
                        }

                        String textContent = parser.nextText();
                        if (textContent != null && !textContent.trim().isEmpty()) {
                            mapElements.add(new MapElement(currentCommentName, textContent.trim(), x, y, size, color, textAnchor, rot, rotX, rotY));
                        }
                    } else if ("path".equalsIgnoreCase(tagName)) {
                        String d = parser.getAttributeValue(null, "d");
                        String fill = parser.getAttributeValue(null, "fill");
                        String id = parser.getAttributeValue(null, "id");

                        if (d != null) {
                            if (fill == null) fill = "#000000";
                            Path path = PathParser.createPathFromPathData(d);
                            int color = Color.parseColor(fill);
                            
                            String elementName = (id != null && !id.isEmpty()) ? id : currentCommentName;
                            boolean clickable = false;
                            for (String b : CLICKABLE_BUILDINGS) {
                                if (b.equalsIgnoreCase(elementName)) {
                                    clickable = true;
                                    break;
                                }
                            }
                            mapElements.add(new MapElement(elementName, path, color, clickable));
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
            if (el.path != null) {
                RectF bounds = new RectF();
                el.path.computeBounds(bounds, true);
                el.rawRegion = new Region();
                el.rawRegion.setPath(el.path, new Region((int) bounds.left, (int) bounds.top, (int) bounds.right, (int) bounds.bottom));
            } else if (el.bitmap != null && el.imageBounds != null) {
                el.rawRegion = new Region((int)el.imageBounds.left, (int)el.imageBounds.top, (int)el.imageBounds.right, (int)el.imageBounds.bottom);
            }
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
            if (el.bitmap != null && el.imageBounds != null) {
                canvas.drawBitmap(el.bitmap, null, el.imageBounds, paint);
            } else if (el.path != null) {
                if (el.isClickable) {
                    paint.setColor(Color.parseColor("#90CAF9")); // Slightly darker blue for clickable buildings
                } else if ("out_of_bounds".equalsIgnoreCase(el.name) || "Out of Bounds".equalsIgnoreCase(el.name)) {
                    paint.setColor(Color.parseColor("#a1a1a14f")); // 75% Transparent grey for out of bounds
                } else {
                    paint.setColor(el.color);
                }
                canvas.drawPath(el.path, paint);

                if (el.isClickable) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.parseColor("#1E88E5")); // Accent Blue border
                    paint.setStrokeWidth(2f / (baseScaleFactor * currentScale)); // Keep stroke width consistent
                    canvas.drawPath(el.path, paint);
                    paint.setStyle(Paint.Style.FILL);
                }
            } else if (el.textContent != null) {
                paint.setColor(Color.BLACK); // Force black text
                paint.setTextSize(el.textSize);
                paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD));
                
                if ("middle".equals(el.textAnchor)) {
                    paint.setTextAlign(Paint.Align.CENTER);
                } else if ("end".equals(el.textAnchor)) {
                    paint.setTextAlign(Paint.Align.RIGHT);
                } else {
                    paint.setTextAlign(Paint.Align.LEFT);
                }
                
                if (el.textRotation != 0) {
                    canvas.save();
                    canvas.rotate(el.textRotation, el.textRotX, el.textRotY);
                    canvas.drawText(el.textContent, el.textX, el.textY, paint);
                    canvas.restore();
                } else {
                    canvas.drawText(el.textContent, el.textX, el.textY, paint);
                }
            }
        }
        // Draw WiFi Routers Heatmap
        if (wifiRouters != null && !wifiRouters.isEmpty()) {
            Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            gradientPaint.setStyle(Paint.Style.FILL);
            
            Paint wifiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            wifiPaint.setColor(Color.WHITE);
            wifiPaint.setStrokeWidth(1.2f);
            wifiPaint.setStrokeCap(Paint.Cap.ROUND);
            
            Paint coreDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            coreDotPaint.setStyle(Paint.Style.FILL);
            
            for (com.example.campusbiome.models.WifiRouter router : wifiRouters) {
                int devices = router.getConnected_devices();
                int coreColor;
                if (devices <= 10) {
                    coreColor = Color.parseColor("#43A047"); // Green
                } else if (devices <= 30) {
                    coreColor = Color.parseColor("#FB8C00"); // Orange
                } else {
                    coreColor = Color.parseColor("#E53935"); // Red
                }
                
                // Add high opacity to the core color for a very dark gradient center (e.g. 80% alpha = #CC)
                int centerColor = Color.parseColor(String.format("#CC%s", Integer.toHexString(coreColor).substring(2)));
                int edgeColor = Color.TRANSPARENT;
                
                float cx = router.getX();
                float cy = router.getY();
                float radius = 220f; // Extremely spread out radius!
                
                // Draw fading heatmap circle
                gradientPaint.setShader(new android.graphics.RadialGradient(
                        cx, cy, radius, 
                        centerColor, edgeColor, 
                        android.graphics.Shader.TileMode.CLAMP));
                canvas.drawCircle(cx, cy, radius, gradientPaint);
                
                // Draw inner solid dot background for contrast behind the wifi icon
                coreDotPaint.setColor(coreColor);
                canvas.drawCircle(cx, cy, 7f, coreDotPaint); 
                
                // Draw cute WiFi icon centered inside the core dot
                // Bottom dot
                wifiPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy + 3.5f, 1f, wifiPaint);
                
                // Arcs
                wifiPaint.setStyle(Paint.Style.STROKE);
                
                RectF rect1 = new RectF(cx - 2.5f, cy + 1f, cx + 2.5f, cy + 6f);
                canvas.drawArc(rect1, 225, 90, false, wifiPaint);
                
                RectF rect2 = new RectF(cx - 5f, cy - 1.5f, cx + 5f, cy + 8.5f);
                canvas.drawArc(rect2, 225, 90, false, wifiPaint);
                
                RectF rect3 = new RectF(cx - 7.5f, cy - 4f, cx + 7.5f, cy + 11f);
                canvas.drawArc(rect3, 225, 90, false, wifiPaint);
            }
        }

        // Draw Building Heatmaps
        if (buildingDensities != null && !buildingDensities.isEmpty()) {
            Paint bldgGradient = new Paint(Paint.ANTI_ALIAS_FLAG);
            bldgGradient.setStyle(Paint.Style.FILL);
            
            Paint wifiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            wifiPaint.setColor(Color.WHITE);
            wifiPaint.setStrokeWidth(1.2f);
            wifiPaint.setStrokeCap(Paint.Cap.ROUND);
            
            Paint coreDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            coreDotPaint.setStyle(Paint.Style.FILL);

            for (MapElement el : mapElements) {
                if (el.name != null && buildingDensities.containsKey(el.name) && el.path != null) {
                    int devices = buildingDensities.get(el.name);
                    int coreColor;
                    if (devices <= 200) {
                        coreColor = Color.parseColor("#43A047"); // Green
                    } else if (devices <= 500) {
                        coreColor = Color.parseColor("#FB8C00"); // Orange
                    } else {
                        coreColor = Color.parseColor("#E53935"); // Red
                    }
                    
                    int centerColor = Color.parseColor(String.format("#CC%s", Integer.toHexString(coreColor).substring(2)));
                    int edgeColor = Color.TRANSPARENT;
                    
                    RectF bounds = new RectF();
                    el.path.computeBounds(bounds, true);
                    float cx = bounds.centerX();
                    float cy = bounds.centerY();
                    float radius = 55f; // Same huge spread out radius!
                    
                    bldgGradient.setShader(new android.graphics.RadialGradient(
                            cx, cy, radius, 
                            centerColor, edgeColor, 
                            android.graphics.Shader.TileMode.CLAMP));
                    canvas.drawCircle(cx, cy, radius, bldgGradient);
                }
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

        if (tapListener != null) {
            tapListener.onMapTap(pts[0], pts[1]);
            return;
        }

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
        
        Bitmap bitmap;
        RectF imageBounds;

        MapElement(String name, Path path, int color, boolean isClickable) {
            this.name = name;
            this.path = path;
            this.color = color;
            this.isClickable = isClickable;
        }

        MapElement(String name, Bitmap bitmap, RectF imageBounds) {
            this.name = name;
            this.bitmap = bitmap;
            this.imageBounds = imageBounds;
            this.isClickable = false;
        }

        // For text elements
        String textContent;
        float textX, textY, textSize;
        int textColor;
        String textAnchor;
        float textRotation;
        float textRotX, textRotY;

        MapElement(String name, String textContent, float x, float y, float size, int color, String anchor, float rot, float rotX, float rotY) {
            this.name = name;
            this.textContent = textContent;
            this.textX = x;
            this.textY = y;
            this.textSize = size;
            this.textColor = color;
            this.textAnchor = anchor;
            this.textRotation = rot;
            this.textRotX = rotX;
            this.textRotY = rotY;
            this.isClickable = false;
        }
    }
}
