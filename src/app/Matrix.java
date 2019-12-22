package app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Matrix {
    final long[] rows;
    final int m;

    private Matrix(int m) {
        rows = new long[m];
        this.m = m;
    }

    public Matrix(long... rows) {
        this.rows = Arrays.copyOf(rows, rows.length);
        this.m = rows.length;
    }

    public static Matrix zeros(int m) {
        return new Matrix(m);
    }

    public static Matrix ones(int m) {
        Matrix f = zeros(m);
        for (int i = 0; i < m; i++) {
            f.setEntry(i, i, 1);
        }
        return f;
    }

    public Matrix swapRows(int i, int j) {
        long ri = rows[i];
        long rj = rows[j];
        rows[j] = ri;
        rows[i] = rj;
        return this;
    }

    public Matrix addRows(int src, int dst) {
        rows[dst] ^= rows[src];
        return this;
    }

    public static void swapRows(int i, int j, int[] x) {
        int ri = x[i];
        int rj = x[j];
        x[j] = ri;
        x[i] = rj;
    }

    public static void addRows(int src, int dst, int[] x) {
        x[dst] ^= x[src];
    }

    public int[] getColumn(int j) {
        int[] column = new int[m];
        long b = 1L << (63 - j);
        for (int i = 0; i < m; i++) {
            if ((rows[i] & b) != 0)
                column[i] = 1;
        }
        return column;
    }

    public Matrix swapColumns(int i, int j) {
        int[] ci = getColumn(i);
        int[] cj = getColumn(j);
        this.setColumn(j, ci);
        this.setColumn(i, cj);
        return this;
    }

    public Matrix addColumns(int src, int dst) {
        int[] s = getColumn(src);
        int[] d = getColumn(dst);
        for (int i = 0; i < m; i++) {
            d[i] ^= s[i];
        }
        setColumn(dst, d);
        return this;
    }

    public Matrix trig() {
        int rank = 0;
        for (int j = 63; j >= 0; j--) {
            long b = 1L << j;
            for (int i = rank; i < m; i++) {
                if ((rows[i] & b) != 0) {
                    /* erase other rows */
                    for (int k = i + 1; k < m; k++) {
                        if ((rows[k] & b) != 0)
                            addRows(i, k);
                    }
                    swapRows(i, rank);
                    rank++;
                    break;
                }
            }
        }
        return this;
    }

    public Matrix setEntry(int i, int j, int value) {
        if (value != 0) {
            long b = 0x8000000000000000L >>> j;
            rows[i] |= b;
        } else {
            long b = ~(0x8000000000000000L >>> j);
            rows[i] &= b;
        }
        return this;
    }

    public Matrix setColumn(int j, int[] column) {
        for (int i = 0; i < m; i++) {
            setEntry(i, j, column[i]);
        }
        return this;
    }

    public int[] multVect(long v) {
        int[] result = new int[m];
        for (int i = 0; i < m; i++) {
            long p = rows[i] & v;
            int q = 0;
            for (int j = 0; j < 64; j++) {
                if (((1L << j) & p) != 0) {
                    q ^= 1;
                }
            }
            result[i] = q;
        }
        return result;
    }

    public List<Long> kernel() {
        Matrix f = new Matrix(this.rows); /* deep copy */
        f.trig(); /* triangulation doesn't affect the kernel */
        Matrix q = Matrix.ones(64);

        int rank = 0;
        for (int i = 0; i < f.m; i++) {
            for (int j = rank; i < 64; j++) {
                long b = 1L << (63 - j);
                if ((f.rows[i] & b) != 0) {
                    /* erase other columns */
                    for (int k = j + 1; k < 64; k++) {
                        long bb = 1L << (63 - k);
                        if ((f.rows[i] & bb) != 0) {
                            f.addColumns(j, k);
                            q.addRows(j, k);
                        }
                    }
                    f.swapColumns(j, rank);
                    q.swapRows(j, rank);
                    rank++;
                    break;
                }
            }
        }
        // for (long k : f.rows) {
        //     System.out.printf("0x%016x%n", k);
        // }
        List<Long> basis = new ArrayList<>();
        for (int i = rank; i < 64; i++) {
            basis.add(q.rows[i]);
        }
        return basis;
    }

    public static Matrix f() {
        int m = 57;
        Matrix f = Matrix.zeros(m);
        for (int j = 0; j < 64; j++) {
            long b = 1L << (63 - j);
            int[] column = DenGenerator.pure(b, 0);
            f.setColumn(j, column);
        }
        return f;
    }

    public static Matrix finv() {
        /*
         * left inverse like matrix
         * 
         * (finv)^t*f=[1,*;0,0]
         */
        int m = 57;
        Matrix finv = Matrix.ones(m);
        Matrix f = f();
        int rank = 0;
        for (int j = 63; j >= 0; j--) {
            long b = 1L << j;
            for (int i = rank; i < f.m; i++) {
                if ((f.rows[i] & b) != 0) {
                    /* erase other rows */
                    for (int k = 0; k < f.m; k++) {
                        if ((k != i) && (f.rows[k] & b) != 0) {
                            f.addRows(i, k);
                            finv.addColumns(i, k);
                        }
                    }
                    f.swapRows(i, rank);
                    finv.swapColumns(i, rank);
                    rank++;
                    break;
                }
            }
        }
        return finv;
    }
}