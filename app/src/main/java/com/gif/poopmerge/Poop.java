package com.gif.poopmerge;

import android.graphics.Bitmap;

public class Poop {

    private int type;
    private Bitmap image;
    float x, y; // Position (float für genauere Physik)
    float radius; // Radius für kreisförmige Kollision
    float velocityX; // Horizontale Geschwindigkeit
    float velocityY; // Vertikale Geschwindigkeit
    private static final float GRAVITY = 0.5f; // Schwerkraft
    private static final float FRICTION = 0.98f; // Reibung der Bewegung

    public Poop(int type, Bitmap image, float x, float y) {
        this.type = type;
        this.image = image;
        this.x = x;
        this.y = y;
        this.radius = image.getWidth() / 2.0f; // Radius ist die Hälfte der Bildbreite
        this.velocityX = 0;
        this.velocityY = 0;
    }

    public void update() {
        velocityY += GRAVITY; // Schwerkraft anwenden
        x += velocityX;
        y += velocityY;

        // Reibung anwenden, um Bewegungen zu verlangsamen
        velocityX *= FRICTION;
        if (Math.abs(velocityX) < 0.1f) {
            velocityX = 0; // Schwellenwert, um aufzuhören
        }
    }

    public void draw(android.graphics.Canvas canvas) {
        // Zeichne das Bild so, dass sein Mittelpunkt (x, y) ist
        canvas.drawBitmap(image, x - radius, y - radius, null);
    }

    // --- Getter und Setter ---
    public int getType() { return type; }
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getRadius() { return radius; }
    public void setRadius(float radius) { this.radius = radius; }
    public Bitmap getImage() { return image; }
    public void setImage(Bitmap image) { this.image = image; }
    public float getVelocityX() { return velocityX; }
    public void setVelocityX(float velocityX) { this.velocityX = velocityX; }
    public float getVelocityY() { return velocityY; }
    public void setVelocityY(float velocityY) { this.velocityY = velocityY; }
}