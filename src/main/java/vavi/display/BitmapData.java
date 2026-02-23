package vavi.display;


public class BitmapData {

    public int width;
    public int height;
    private final int[] pixels;

    public BitmapData(int width, int height, boolean transparent, int fillColor) {
        this.width = width;
        this.height = height;
        this.pixels = new int[Math.max(0, width * height)];
        int color = transparent ? fillColor : (0xff000000 | (fillColor & 0x00ffffff));
        for (int i = 0; i < this.pixels.length; i++) {
            this.pixels[i] = color;
        }
    }

    public int getPixel(int x, int y) {
        if (!inBounds(x, y)) {
            return 0;
        }
        return pixels[y * width + x] & 0x00ffffff;
    }

    public void setPixel32(int x, int y, int color) {
        if (!inBounds(x, y)) {
            return;
        }
        pixels[y * width + x] = color;
    }

    public int getPixel32(int x, int y) {
        if (!inBounds(x, y)) {
            return 0;
        }
        return pixels[y * width + x];
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }
}
