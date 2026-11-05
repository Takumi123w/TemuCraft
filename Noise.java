import java.util.Random;


final class Noise {
    static final int RES = 1024;
    static final int YWRAPB = 4;
    static final int YWRAP = 1 << YWRAPB;
    static final int ZWRAPB = 8;
    static final int ZWRAP = 1 << ZWRAPB;
    static final int OCTAVES = 4;

    private static final float[] perlin = new float[RES + 1];

    static {
        Random r = new Random(18295169L);
        for (int i = 0; i <= RES; i++) {
            perlin[i] = (r.nextInt() & 0x7FFFFFFF) / 2147483647f;
        }
    }

    private Noise() {}



    static float noise(float x, float y) {
        if (x < 0) x = -x;
        if (y < 0) y = -y;

        int xi = (int) x;
        int yi = (int) y;
        float xf = x - xi;
        float yf = y - yi;

        float r = 0;
        float ampl = 0.5f;
        float rxf, ryf, n1, n2, n3;

        for (int i = 0; i < OCTAVES; i++) {
            int of = xi + (yi << YWRAPB);

            rxf = Calc.scaledCosine(xf);
            ryf = Calc.scaledCosine(yf);

            n1 = perlin[of % RES];
            n1 += rxf * (perlin[(of + 1) % RES] - n1);
            n2 = perlin[(of + YWRAP) % RES];
            n2 += rxf * (perlin[(of + YWRAP + 1) % RES] - n2);
            n1 += ryf * (n2 - n1);

            of += ZWRAP;
            n2 = perlin[of % RES];
            n2 += rxf * (perlin[(of + 1) % RES] - n2);
            n3 = perlin[(of + YWRAP) % RES];
            n3 += rxf * (perlin[(of + YWRAP + 1) % RES] - n3);
            n2 += ryf * (n3 - n2);

            n1 += Calc.scaledCosine(0) * (n2 - n1);

            r += n1 * ampl;
            ampl *= 0.5f;
            xi <<= 1;
            xf *= 2;
            yi <<= 1;
            yf *= 2;

            if (xf >= 1.0f) {
                xi++;
                xf--;
            }
            if (yf >= 1.0f) {
                yi++;
                yf--;
            }
        }
        return r;
    }
}
