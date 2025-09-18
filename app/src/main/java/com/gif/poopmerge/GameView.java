package com.gif.poopmerge;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread gameThread;
    private List<Poop> placedPoops = new ArrayList<>();
    private Poop fallingPoop;
    private Bitmap[] poopImages = new Bitmap[10];
    private final Random random = new Random();
    private int score = 0;
    private int highScore;
    private final Paint scorePaint;
    private final Paint toiletPaint;
    private final Paint toiletContourPaint;
    private RectF containerRect;
    private boolean isDropping = false;
    private List<Particle> particles = new ArrayList<>();

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);

        float baseSize = 150;
        float scaleFactor = 1.25f;

        for (int i = 0; i < 10; i++) {
            int resId = getResources().getIdentifier("poop_" + (i + 1), "drawable", context.getPackageName());
            Bitmap original = BitmapFactory.decodeResource(getResources(), resId);
            int width = (int) (baseSize * Math.pow(scaleFactor, i));
            poopImages[i] = Bitmap.createScaledBitmap(original, width, width, true);
        }

        loadHighScore();

        scorePaint = new Paint();
        scorePaint.setColor(Color.BLACK);
        scorePaint.setTextSize(60);
        scorePaint.setTextAlign(Paint.Align.CENTER);

        toiletPaint = new Paint();
        toiletPaint.setColor(Color.WHITE);
        toiletPaint.setStyle(Paint.Style.FILL);

        toiletContourPaint = new Paint();
        toiletContourPaint.setColor(Color.GRAY);
        toiletContourPaint.setStyle(Paint.Style.STROKE);
        toiletContourPaint.setStrokeWidth(30);

        gameThread = new GameThread(getHolder(), this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        int containerWidth = getWidth();
        int containerHeight = getHeight() * 7 / 10;
        int left = 0;
        int top = getHeight() - containerHeight;
        containerRect = new RectF(left, top, left + containerWidth, top + containerHeight);

        spawnNewPoop(getWidth() / 2.0f);
        gameThread.setRunning(true);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        gameThread.setRunning(false);
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void update() {
        if (fallingPoop != null && isDropping) {
            fallingPoop.update();

            if (fallingPoop.y + fallingPoop.radius > containerRect.bottom - toiletContourPaint.getStrokeWidth()) {
                fallingPoop.y = containerRect.bottom - toiletContourPaint.getStrokeWidth() - fallingPoop.radius;
                placeFallingPoop();
                return;
            }

            for (Poop p : placedPoops) {
                float dx = p.x - fallingPoop.x;
                float dy = p.y - fallingPoop.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                if (distance < fallingPoop.radius + p.radius) {
                    placeFallingPoop();
                    break;
                }
            }
        }

        for (Poop poop : placedPoops) {
            poop.update();
        }

        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle p = iterator.next();
            p.update();
            if (!p.isAlive()) {
                iterator.remove();
            }
        }

        applyPhysics();
        for (int i = 0; i < 4; i++) {
            checkCollisionsAndMerge();
        }
    }

    private void applyPhysics() {
        for (Poop poop : placedPoops) {
            float wallThickness = toiletContourPaint.getStrokeWidth() / 2;

            if (poop.x - poop.radius < containerRect.left + wallThickness) {
                poop.x = containerRect.left + wallThickness + poop.radius;
                poop.setVelocityX(Math.abs(poop.getVelocityX()) * 0.5f);
            }
            if (poop.x + poop.radius > containerRect.right - wallThickness) {
                poop.x = containerRect.right - wallThickness - poop.radius;
                poop.setVelocityX(-Math.abs(poop.getVelocityX()) * 0.5f);
            }
            if (poop.y + poop.radius > containerRect.bottom - toiletContourPaint.getStrokeWidth()) {
                poop.y = containerRect.bottom - toiletContourPaint.getStrokeWidth() - poop.radius;
                poop.setVelocityY(-Math.abs(poop.getVelocityY()) * 0.2f);
                poop.setAngularVelocity(poop.getAngularVelocity() * 0.9f);
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas != null) {
            canvas.drawColor(Color.parseColor("#ADD8E6"));
            canvas.drawRect(containerRect, toiletPaint);

            float wallThickness = toiletContourPaint.getStrokeWidth() / 2;
            canvas.drawLine(containerRect.left + wallThickness, containerRect.top, containerRect.left + wallThickness, containerRect.bottom - wallThickness, toiletContourPaint);
            canvas.drawLine(containerRect.left, containerRect.bottom - wallThickness, containerRect.right, containerRect.bottom - wallThickness, toiletContourPaint);
            canvas.drawLine(containerRect.right - wallThickness, containerRect.top, containerRect.right - wallThickness, containerRect.bottom - wallThickness, toiletContourPaint);

            for (Poop poop : placedPoops) {
                poop.draw(canvas);
            }
            for (Particle p : particles) {
                p.draw(canvas);
            }
            if (fallingPoop != null) {
                fallingPoop.draw(canvas);
            }
            canvas.drawText("Score: " + score, getWidth() / 2, 100, scorePaint);
            canvas.drawText("High: " + highScore, getWidth() / 2, 180, scorePaint);
        }
    }

    private void checkCollisionsAndMerge() {
        List<Poop> poopsToRemove = new ArrayList<>();
        List<Poop> poopsToAdd = new ArrayList<>();

        for (int i = 0; i < placedPoops.size(); i++) {
            for (int j = i + 1; j < placedPoops.size(); j++) {
                Poop p1 = placedPoops.get(i);
                Poop p2 = placedPoops.get(j);

                if (poopsToRemove.contains(p1) || poopsToRemove.contains(p2)) continue;

                float dx = p2.x - p1.x;
                float dy = p2.y - p1.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                float minDistance = p1.radius + p2.radius;

                if (distance < minDistance) {
                    if (p1.getType() == p2.getType() && p1.getType() < 9) {
                        poopsToRemove.add(p1);
                        poopsToRemove.add(p2);
                        int newType = p1.getType() + 1;
                        float newX = (p1.x + p2.x) / 2;
                        float newY = (p1.y + p2.y) / 2;
                        for (int k = 0; k < 20; k++) {
                            particles.add(new Particle(newX, newY));
                        }
                        Poop newPoop = new Poop(newType, poopImages[newType], newX, newY);
                        poopsToAdd.add(newPoop);
                        score += (newType + 1) * 100;
                        if (score > highScore) {
                            highScore = score;
                            saveHighScore();
                        }
                    } else {
                        float overlap = minDistance - distance;
                        float nx = dx / distance;
                        float ny = dy / distance;

                        p1.x -= overlap / 2 * nx;
                        p1.y -= overlap / 2 * ny;
                        p2.x += overlap / 2 * nx;
                        p2.y += overlap / 2 * ny;

                        float relVelX = p2.getVelocityX() - p1.getVelocityX();
                        float relVelY = p2.getVelocityY() - p1.getVelocityY();
                        float velAlongNormal = relVelX * nx + relVelY * ny;

                        if (velAlongNormal > 0) continue;

                        float restitution = 0.2f;
                        float impulse = -(1 + restitution) * velAlongNormal / (1 / p1.getMass() + 1 / p2.getMass());

                        p1.setVelocityX(p1.getVelocityX() - impulse / p1.getMass() * nx);
                        p1.setVelocityY(p1.getVelocityY() - impulse / p1.getMass() * ny);
                        p2.setVelocityX(p2.getVelocityX() + impulse / p2.getMass() * nx);
                        p2.setVelocityY(p2.getVelocityY() + impulse / p2.getMass() * ny);

                        // *** NEU: DESTABILISIERUNG FÜR BALANCIERENDE BÄLLE ***
                        float dxAbs = Math.abs(p1.getX() - p2.getX());
                        // Prüft, ob sie fast vertikal gestapelt sind und sich kaum bewegen
                        if (dxAbs < (p1.getRadius() + p2.getRadius()) * 0.1f && Math.abs(velAlongNormal) < 0.5f) {
                            // Identifiziert den oberen Poop
                            Poop topPoop = (p1.getY() < p2.getY()) ? p1 : p2;
                            // Wendet einen kleinen zufälligen horizontalen Stoß an
                            float push = (random.nextFloat() - 0.5f) * 2f; // Kleiner Stoß zwischen -1 und 1
                            topPoop.setVelocityX(topPoop.getVelocityX() + push);
                        }


                        if (Math.abs(velAlongNormal) < 1.0f) {
                            p1.setAngularVelocity(p1.getAngularVelocity() * 0.9f);
                            p2.setAngularVelocity(p2.getAngularVelocity() * 0.9f);
                        } else {
                            float tangentX = -ny;
                            float tangentY = nx;
                            float velAlongTangent = relVelX * tangentX + relVelY * tangentY;
                            float frictionImpulse = -velAlongTangent / (1 / p1.getMass() + 1 / p2.getMass()) * 0.5f;

                            p1.addRotation(-frictionImpulse / p1.getMass());
                            p2.addRotation(-frictionImpulse / p2.getMass());
                        }
                    }
                }
            }
        }

        placedPoops.removeAll(poopsToRemove);
        placedPoops.addAll(poopsToAdd);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (fallingPoop == null) return true;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (!isDropping) {
                    fallingPoop.setX(event.getX());
                    float wallThickness = toiletContourPaint.getStrokeWidth() / 2;
                    if (fallingPoop.getX() - fallingPoop.getRadius() < containerRect.left + wallThickness) {
                        fallingPoop.setX(containerRect.left + wallThickness + fallingPoop.getRadius());
                    }
                    if (fallingPoop.getX() + fallingPoop.getRadius() > containerRect.right - wallThickness) {
                        fallingPoop.setX(containerRect.right - wallThickness - fallingPoop.getRadius());
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!isDropping) {
                    isDropping = true;
                }
                break;
        }
        return true;
    }

    private void placeFallingPoop() {
        if (fallingPoop != null) {
            fallingPoop.setVelocityX(0);
            fallingPoop.setVelocityY(0);
            placedPoops.add(fallingPoop);
            fallingPoop = null;
            isDropping = false;
            spawnNewPoop(getWidth() / 2.0f);
        }
    }

    private void spawnNewPoop(float initialX) {
        int type = random.nextInt(3);
        float startY = containerRect.top - poopImages[type].getHeight();
        fallingPoop = new Poop(type, poopImages[type], initialX, startY);
    }

    private void loadHighScore() {
        SharedPreferences prefs = getContext().getSharedPreferences("PoopMerge", Context.MODE_PRIVATE);
        highScore = prefs.getInt("highScore", 0);
    }

    private void saveHighScore() {
        SharedPreferences prefs = getContext().getSharedPreferences("PoopMerge", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("highScore", highScore);
        editor.apply();
    }
}