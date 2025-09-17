package com.gif.poopmerge;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

public class Poop {

    private int type;
    private Bitmap image;
    float x, y;
    float radius;
    float velocityX;
    float velocityY;
    private float mass;
    private float rotation;
    private float angularVelocity;
    private static final float GRAVITY = 0.5f;
    private static final float AIR_FRICTION = 0.99f; // Leichter Luftwiderstand

    public Poop(int type, Bitmap image, float x, float y) {
        this.type = type;
        this.image = image;
        this.x = x;
        this.y = y;
        this.radius = image.getWidth() / 2.0f;
        this.velocityX = 0;
        this.velocityY = 0;
        this.mass = (float) Math.pow(1.6, type);
        this.rotation = 0;
        this.angularVelocity = 0;
    }

    public void update() {
        velocityY += GRAVITY;
        x += velocityX;
        y += velocityY;

        rotation += angularVelocity;
        // Wende leichten "Luftwiderstand" auf die Drehung an
        angularVelocity *= AIR_FRICTION;

        // Verlangsame die lineare Bewegung
        velocityX *= AIR_FRICTION;
        if (Math.abs(velocityX) < 0.05f) {
            velocityX = 0;
        }
        if (Math.abs(angularVelocity) < 0.05f) {
            angularVelocity = 0;
        }
    }

    public void draw(Canvas canvas) {
        Matrix matrix = new Matrix();
        matrix.postTranslate(-image.getWidth() / 2.0f, -image.getHeight() / 2.0f);
        matrix.postRotate(rotation);
        matrix.postTranslate(x, y);
        canvas.drawBitmap(image, matrix, null);
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
    public float getMass() { return mass; }
    public float getAngularVelocity() { return angularVelocity; } // NEU: Getter
    public void setAngularVelocity(float angularVelocity) { this.angularVelocity = angularVelocity; } // NEU: Setter
    public void addRotation(float rotation) { this.angularVelocity += rotation; }
}