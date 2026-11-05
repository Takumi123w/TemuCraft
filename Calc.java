


final class Calc {
    private Calc() {}

    

    
    static int floor(float v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    
    static int round(float v) {
        return (int) Math.floor(v + 0.5f);
    }

    
    static int mod(int a, int n) {
        int r = a % n;
        return r < 0 ? r + n : r;
    }

    
    static int wrapDeg(int deg) {
        return mod(deg, 360);
    }

    

    static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    
    static int channel(float unit) {
        int c = (int) (unit * 255f);
        return c < 0 ? 0 : (c > 255 ? 255 : c);
    }

    

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    
    static float fract(float v) {
        return v - floor(v);
    }

    
    static float scaledCosine(float t) {
        return (float) (0.5 * (1.0 - Math.cos(t * Math.PI)));
    }

    

    static float dist2(float dx, float dy, float dz) {
        return dx * dx + dy * dy + dz * dz;
    }

    static float dist(float dx, float dy, float dz) {
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    

    
    static float atan2(float y, float x) {
        final float pi = 3.1415927f;
        if (x == 0f) return y > 0f ? pi / 2f : (y == 0f ? 0f : -pi / 2f);
        float z = y / x;
        if (Math.abs(z) < 1f) {
            float a = z / (1f + 0.28f * z * z);
            if (x < 0f) return y < 0f ? a - pi : a + pi;
            return a;
        } else {
            float a = pi / 2f - z / (z * z + 0.28f);
            return y < 0f ? a - pi : a;
        }
    }

    
    static float toDeg(float rad) {
        return rad * 57.29578f;
    }
}
