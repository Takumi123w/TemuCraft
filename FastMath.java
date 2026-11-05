final class FastMath {
    private static final int TRIG_SETS = 360;
    private static final float[] SIN_TABLE = new float[TRIG_SETS];
    private static final float[] COS_TABLE = new float[TRIG_SETS];

    static {
        for (int i = 0; i < TRIG_SETS; i++) {
            double rad = Math.toRadians(i);
            SIN_TABLE[i] = (float) Math.sin(rad);
            COS_TABLE[i] = (float) Math.cos(rad);
        }
    }

    
    static float sinDeg(int deg) {
        deg = (deg % 360 + 360) % 360;
        return SIN_TABLE[deg];
    }

    
    static float cosDeg(int deg) {
        deg = (deg % 360 + 360) % 360;
        return COS_TABLE[deg];
    }

    
    static float invSqrt(float x) {
        float xhalf = 0.5f * x;
        int i = Float.floatToIntBits(x);
        i = 0x5f3759df - (i >> 1);
        x = Float.intBitsToFloat(i);
        x = x * (1.5f - xhalf * x * x); 
        return x;
    }

    
    static void normalize(float[] v) {
        float lenSq = v[0] * v[0] + v[1] * v[1] + v[2] * v[2];
        if (lenSq < 1e-6f) return;
        float invLen = invSqrt(lenSq);
        v[0] *= invLen;
        v[1] *= invLen;
        v[2] *= invLen;
    }
}