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
    private List<Poop> placedPoops = new ArrayList<>(); // Platzierten Kothaufen
    private Poop fallingPoop; // Der Kothaufen, der gerade vom Spieler bewegt wird oder fällt
    // HINZUGEFÜGT: Array auf 10 Bilder erweitert
    private Bitmap[] poopImages = new Bitmap[10];
    private final Random random = new Random();
    private int score = 0;
    private int highScore;
    private final Paint scorePaint;
    private final Paint toiletPaint; // Neuer Paint für die Toilette
    private final Paint toiletContourPaint; // Neuer Paint für die Toilettenkontur

    private RectF containerRect; // Rechteck für den Spielbehälter (Topf)

    private boolean isDropping = false; // Steuert, ob der aktuelle Kothaufen fällt
    private List<Particle> particles = new ArrayList<>();


    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);

        // --- VERÄNDERT: Lade die Kothaufen-Bilder und skaliere sie ---
        // Jede neue Stufe wird um 25% größer.
        float baseSize = 150; // Startgröße für poop_1 (vorher 75, jetzt verdoppelt)
        float scaleFactor = 1.25f; // Jede Evolution ist 25% größer als die vorherige

        // Schleife auf 10 Bilder erweitert
        for (int i = 0; i < 10; i++) {
            int resId = getResources().getIdentifier("poop_" + (i + 1), "drawable", context.getPackageName());
            Bitmap original = BitmapFactory.decodeResource(getResources(), resId);

            // Berechne die neue Breite basierend auf der Evolutionsstufe
            int width = (int) (baseSize * Math.pow(scaleFactor, i));

            poopImages[i] = Bitmap.createScaledBitmap(original, width, width, true);
        }
        // --- ENDE DER ÄNDERUNG ---


        // Lade den Highscore
        loadHighScore();

        // Initialisiere die Paints
        scorePaint = new Paint();
        scorePaint.setColor(Color.BLACK);
        scorePaint.setTextSize(60);
        scorePaint.setTextAlign(Paint.Align.CENTER);

        // Neuer Paint für die Toilette
        toiletPaint = new Paint();
        toiletPaint.setColor(Color.WHITE); // Weiße Toilettenschüssel
        toiletPaint.setStyle(Paint.Style.FILL);

        toiletContourPaint = new Paint();
        toiletContourPaint.setColor(Color.GRAY); // Grauer Rand
        toiletContourPaint.setStyle(Paint.Style.STROKE);
        toiletContourPaint.setStrokeWidth(30); // Dickere Kontur


        gameThread = new GameThread(getHolder(), this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Container an die Bildschirmränder anpassen
        int containerWidth = getWidth(); // Volle Breite
        int containerHeight = getHeight() * 7 / 10;
        int left = 0; // Startet am linken Rand
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
            if (fallingPoop.y + fallingPoop.radius > containerRect.bottom) {
                fallingPoop.y = containerRect.bottom - fallingPoop.radius;
                fallingPoop.setVelocityY(0);
                fallingPoop.setVelocityX(0);
                placeFallingPoop();
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
        checkCollisionsAndMerge();
    }

    private void applyPhysics() {
        for (Poop poop : placedPoops) {
            // Kollision mit den Innenwänden des Containers
            float wallThickness = toiletContourPaint.getStrokeWidth() / 2;
            if (poop.x - poop.radius < containerRect.left + wallThickness) {
                poop.x = containerRect.left + wallThickness + poop.radius;
                poop.setVelocityX(Math.abs(poop.getVelocityX()) * 0.8f);
            }
            if (poop.x + poop.radius > containerRect.right - wallThickness) {
                poop.x = containerRect.right - wallThickness - poop.radius;
                poop.setVelocityX(-Math.abs(poop.getVelocityX()) * 0.8f);
            }
            if (poop.y + poop.radius > containerRect.bottom - wallThickness) {
                poop.y = containerRect.bottom - wallThickness - poop.radius;
                poop.setVelocityY(-Math.abs(poop.getVelocityY()) * 0.5f);
                poop.setVelocityX(poop.getVelocityX() * 0.8f);
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas != null) {
            canvas.drawColor(Color.parseColor("#ADD8E6"));

            // Zeichne die Toilette
            canvas.drawRect(containerRect, toiletPaint);
            canvas.drawRect(containerRect, toiletContourPaint);

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
                        if (distance == 0) distance = 0.1f;
                        float nx = dx / distance;
                        float ny = dy / distance;

                        p1.x -= overlap / 2 * nx;
                        p1.y -= overlap / 2 * ny;
                        p2.x += overlap / 2 * nx;
                        p2.y += overlap / 2 * ny;

                        float relVelX = p1.getVelocityX() - p2.getVelocityX();
                        float relVelY = p1.getVelocityY() - p2.getVelocityY();
                        float velAlongNormal = relVelX * nx + relVelY * ny;

                        if (velAlongNormal > 0) continue;

                        float restitution = 0.8f; // Erhöhter Abpralleffekt
                        float impulse = -(1 + restitution) * velAlongNormal;
                        impulse /= 2;

                        float impulseX = impulse * nx;
                        float impulseY = impulse * ny;

                        p1.setVelocityX(p1.getVelocityX() + impulseX);
                        p1.setVelocityY(p1.getVelocityY() + impulseY);
                        p2.setVelocityX(p2.getVelocityX() - impulseX);
                        p2.setVelocityY(p2.getVelocityY() - impulseY);
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