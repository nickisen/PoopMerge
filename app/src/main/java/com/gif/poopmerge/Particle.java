// Pfad: app/src/main/java/com/gif/poopmerge/Particle.java

package com.gif.poopmerge;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Random;

public class Particle {
    float x, y;
    float velocityX, velocityY;
    private int life;
    private Paint paint;
    private Random random = new Random();
    private float radius;

    /**
     * Konstruktor für Partikel.
     * @param x Startposition X
     * @param y Startposition Y
     * @param color Die Farbe des Partikels
     */
    public Particle(float x, float y, int color) {
        this.x = x;
        this.y = y;
        // Zufällige Geschwindigkeit in einem breiteren Bereich für einen "explosiveren" Effekt
        this.velocityX = (random.nextFloat() * 8) - 4;
        this.velocityY = (random.nextFloat() * 8) - 4;
        this.life = 30 + random.nextInt(20); // Lebensdauer zwischen 30 und 50 Frames
        this.radius = 4 + random.nextFloat() * 4; // Zufällige Größe zwischen 4 und 8
        this.paint = new Paint();
        paint.setColor(color);
    }

    public void update() {
        x += velocityX;
        y += velocityY;
        life--;
    }

    public void draw(Canvas canvas) {
        if (isAlive()) {
            // Lässt den Partikel am Ende seines Lebens ausblenden
            paint.setAlpha((int) (255 * (life / 50.0f)));
            canvas.drawCircle(x, y, radius, paint);
        }
    }

    public boolean isAlive() {
        return life > 0;
    }
}