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

    public Particle(float x, float y) {
        this.x = x;
        this.y = y;
        this.velocityX = (random.nextFloat() * 4) - 2; // Zufällige X-Geschwindigkeit
        this.velocityY = (random.nextFloat() * 4) - 2; // Zufällige Y-Geschwindigkeit
        this.life = 30; // Lebensdauer des Partikels (ca. 0,5 Sekunden)
        this.paint = new Paint();
        paint.setColor(Color.parseColor("#A52A2A")); // Braun
    }

    public void update() {
        x += velocityX;
        y += velocityY;
        life--;
    }

    public void draw(Canvas canvas) {
        if (life > 0) {
            paint.setAlpha(life * 8); // Ausblenden
            canvas.drawCircle(x, y, 5, paint); // Zeichne einen kleinen Kreis
        }
    }

    public boolean isAlive() {
        return life > 0;
    }
}