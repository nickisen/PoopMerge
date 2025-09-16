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
    private Bitmap[] poopImages = new Bitmap[5];
    private final Random random = new Random();
    private int score = 0;
    private int highScore;
    private final Paint scorePaint;
    private final Paint containerPaint;
    private RectF containerRect; // Rechteck für den Spielbehälter (Topf)

    private boolean isDropping = false; // Steuert, ob der aktuelle Kothaufen fällt
    private List<Particle> particles = new ArrayList<>();


    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);

        // Lade die Kothaufen-Bilder und skaliere sie
        // poop_1 ist jetzt deutlich kleiner als poop_2
        float baseSizeSmallest = 60; // Größe für poop_1
        float scaleFactor = 1.6f; // Skalierungsfaktor für nachfolgende Poops

        for (int i = 0; i < 5; i++) {
            int resId = getResources().getIdentifier("poop_" + (i + 1), "drawable", context.getPackageName());
            Bitmap original = BitmapFactory.decodeResource(getResources(), resId);

            int width;
            if (i == 0) { // poop_1
                width = (int) baseSizeSmallest;
            } else { // poop_2 bis poop_5
                width = (int) (baseSizeSmallest * Math.pow(scaleFactor, i));
            }

            poopImages[i] = Bitmap.createScaledBitmap(original, width, width, true);
        }


        // Lade den Highscore
        loadHighScore();

        // Initialisiere die Paints
        scorePaint = new Paint();
        scorePaint.setColor(Color.BLACK);
        scorePaint.setTextSize(60);
        scorePaint.setTextAlign(Paint.Align.CENTER);

        containerPaint = new Paint();
        containerPaint.setColor(Color.parseColor("#8B4513")); // Braun für den Topf
        containerPaint.setStyle(Paint.Style.STROKE);
        containerPaint.setStrokeWidth(20);

        gameThread = new GameThread(getHolder(), this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Initialisiere den Behälter, wenn die Oberfläche erstellt wird
        int containerWidth = getWidth() * 8 / 10;
        int containerHeight = getHeight() * 7 / 10;
        int left = (getWidth() - containerWidth) / 2;
        int top = getHeight() - containerHeight;
        containerRect = new RectF(left, top, left + containerWidth, top + containerHeight);

        // Erzeuge den ersten Kothaufen, aber lasse ihn nicht sofort fallen
        spawnNewPoop(getWidth() / 2.0f); // Positioniere in der Mitte oben
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

    // Aktualisiert den Spielzustand mit Physik
    public void update() {
        // Aktualisiere den fallenden Kothaufen, wenn er aktiv ist
        if (fallingPoop != null && isDropping) {
            fallingPoop.update();
            // Kollision des fallenden Kothaufens mit dem Boden des Topfes
            if (fallingPoop.y + fallingPoop.radius > containerRect.bottom) {
                fallingPoop.y = containerRect.bottom - fallingPoop.radius;
                fallingPoop.setVelocityY(0);
                fallingPoop.setVelocityX(0); // Auch horizontale Geschwindigkeit stoppen
                placeFallingPoop();
            }
        }

        // Aktualisiere alle platzierten Kothaufen
        for (Poop poop : placedPoops) {
            poop.update();
        }

        // Partikel aktualisieren und entfernen
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle p = iterator.next();
            p.update();
            if (!p.isAlive()) {
                iterator.remove();
            }
        }


        // Wende Physik und Kollisionen an
        applyPhysics();
        checkCollisionsAndMerge();
    }

    // Wendet Physik auf alle Kothaufen im Topf an
    private void applyPhysics() {
        for (Poop poop : placedPoops) {
            // Begrenzungen des Topfes
            if (poop.x - poop.radius < containerRect.left) {
                poop.x = containerRect.left + poop.radius;
                poop.setVelocityX(Math.abs(poop.getVelocityX()) * 0.8f); // Abprallen von der Wand
            }
            if (poop.x + poop.radius > containerRect.right) {
                poop.x = containerRect.right - poop.radius;
                poop.setVelocityX(-Math.abs(poop.getVelocityX()) * 0.8f); // Abprallen von der Wand
            }
            if (poop.y + poop.radius > containerRect.bottom) {
                poop.y = containerRect.bottom - poop.radius;
                poop.setVelocityY(-Math.abs(poop.getVelocityY()) * 0.5f); // Abprallen vom Boden
                poop.setVelocityX(poop.getVelocityX() * 0.8f); // Auch Reibung am Boden
            }
        }
    }

    // Überprüft Kollisionen und führt Kothaufen zusammen
    private void checkCollisionsAndMerge() {
        // Erzeuge eine temporäre Liste für zu entfernende Poops und neue Poops
        List<Poop> poopsToRemove = new ArrayList<>();
        List<Poop> poopsToAdd = new ArrayList<>();

        for (int i = 0; i < placedPoops.size(); i++) {
            for (int j = i + 1; j < placedPoops.size(); j++) {
                Poop p1 = placedPoops.get(i);
                Poop p2 = placedPoops.get(j);

                // Wenn bereits zum Entfernen markiert, überspringen
                if (poopsToRemove.contains(p1) || poopsToRemove.contains(p2)) continue;

                float dx = p2.x - p1.x;
                float dy = p2.y - p1.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                float minDistance = p1.radius + p2.radius;

                if (distance < minDistance) {
                    // Kollision erkannt

                    // Wenn gleiche Typen und noch nicht die größte Stufe
                    if (p1.getType() == p2.getType() && p1.getType() < 4) {
                        // Markiere zum Zusammenführen
                        poopsToRemove.add(p1);
                        poopsToRemove.add(p2);

                        int newType = p1.getType() + 1;
                        float newX = (p1.x + p2.x) / 2;
                        float newY = (p1.y + p2.y) / 2;

                        // Partikeleffekt erzeugen
                        for (int k = 0; k < 20; k++) {
                            particles.add(new Particle(newX, newY));
                        }


                        Poop newPoop = new Poop(newType, poopImages[newType], newX, newY);
                        poopsToAdd.add(newPoop);

                        score += (newType) * 100; // Höhere Punkte für Verschmelzung
                        if (score > highScore) {
                            highScore = score;
                            saveHighScore();
                        }
                    } else {
                        // Physikalische Abstoßung (Kreis-Kollision mit Impulsübertragung)
                        float angle = (float) Math.atan2(dy, dx);
                        float overlap = minDistance - distance;

                        // Poops verschieben, um Überlappung zu vermeiden (Stapeln)
                        float moveX = overlap * (dx / distance);
                        float moveY = overlap * (dy / distance);
                        p1.x -= moveX / 2;
                        p1.y -= moveY / 2;
                        p2.x += moveX / 2;
                        p2.y += moveY / 2;


                        float targetX = p1.x + (float) Math.cos(angle) * minDistance;
                        float targetY = p1.y + (float) Math.sin(angle) * minDistance;
                        float ax = (targetX - p2.x) * 0.1f; // Stoßkraft
                        float ay = (targetY - p2.y) * 0.1f;

                        p1.setVelocityX(p1.getVelocityX() - ax);
                        p1.setVelocityY(p1.getVelocityY() - ay);
                        p2.setVelocityX(p2.getVelocityX() + ax);
                        p2.setVelocityY(p2.getVelocityY() + ay);
                    }
                }
            }
        }

        // Führe die Änderungen aus (Poops entfernen und hinzufügen)
        placedPoops.removeAll(poopsToRemove);
        placedPoops.addAll(poopsToAdd);
    }

    // Zeichnet alles auf die Leinwand
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas != null) {
            canvas.drawColor(Color.parseColor("#ADD8E6")); // Hellblauer Hintergrund

            // Zeichne den Topf
            canvas.drawRect(containerRect, containerPaint);

            // Zeichne alle platzierten Kothaufen
            for (Poop poop : placedPoops) {
                poop.draw(canvas);
            }
            // Partikel zeichnen
            for (Particle p : particles) {
                p.draw(canvas);
            }


            // Zeichne den nächsten fallenden Kothaufen, wenn er aktiv ist
            if (fallingPoop != null) {
                fallingPoop.draw(canvas);
            }

            // Zeichne den Punktestand
            canvas.drawText("Score: " + score, getWidth() / 2, 100, scorePaint);
            canvas.drawText("High: " + highScore, getWidth() / 2, 180, scorePaint);
        }
    }

    // Behandelt Touch-Eingaben
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (fallingPoop == null) return true; // Nichts zu tun, wenn kein Kothaufen zum Platzieren da ist

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                // Bewege den Kothaufen horizontal mit dem Finger, wenn er noch nicht fällt
                if (!isDropping) {
                    fallingPoop.setX(event.getX());
                    // Verhindere, dass der Kothaufen den Topf verlässt
                    if (fallingPoop.getX() - fallingPoop.getRadius() < containerRect.left) {
                        fallingPoop.setX(containerRect.left + fallingPoop.getRadius());
                    }
                    if (fallingPoop.getX() + fallingPoop.getRadius() > containerRect.right) {
                        fallingPoop.setX(containerRect.right - fallingPoop.getRadius());
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                // Lasse den Kothaufen fallen, wenn der Finger losgelassen wird
                if (!isDropping) {
                    isDropping = true;
                    // Der Kothaufen beginnt zu fallen
                    // Die Update-Methode von Poop wird die Schwerkraft anwenden
                }
                break;
        }
        return true;
    }

    // Platziert den fallenden Kothaufen in die Liste der platzierten Kothaufen
    private void placeFallingPoop() {
        if (fallingPoop != null) {
            placedPoops.add(fallingPoop);
            fallingPoop = null; // Markiere den Kothaufen als platziert
            isDropping = false; // Zurücksetzen, damit der nächste Kothaufen bewegt werden kann
            spawnNewPoop(getWidth() / 2.0f); // Erzeuge den nächsten Kothaufen in der Mitte
        }
    }

    // Erzeugt einen neuen Kothaufen am oberen Rand des Behälters
    private void spawnNewPoop(float initialX) {
        int type = random.nextInt(2); // Beginne mit den kleinsten Typen (0 oder 1)
        float startY = containerRect.top - poopImages[type].getHeight(); // Positioniere es über dem Behälter
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