package space.yahho.mcmod.novaio.number;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

public class HeavyBigInteger extends Number implements Comparable<HeavyBigInteger>, Serializable {

    private ArrayList<BigInteger> number = new ArrayList<>();
    public static final HeavyBigInteger approximateLimit = new HeavyBigInteger(new BigInteger(String.valueOf(Long.MAX_VALUE)));
    public static final HeavyBigInteger MAX_INTEGER = new HeavyBigInteger(new BigInteger(String.valueOf(Integer.MAX_VALUE)));
    public static final HeavyBigInteger ONE = new HeavyBigInteger(BigInteger.ONE);
    public static final BigInteger MAX_BIGINT = BigInteger.ONE.shiftLeft(Integer.MAX_VALUE - 1).subtract(BigInteger.ONE).shiftLeft(1).add(BigInteger.ONE);
    // max binary digits of HeavyBigInteger
    public static final BigInteger MAX_INT_POW2 = new BigInteger(String.valueOf(Integer.MAX_VALUE)).pow(2);
    @Serial
    private static final long serialVersionUID = 1L;

    public HeavyBigInteger(){}
    public HeavyBigInteger(BigInteger val) {
        this.number.add(val);
    }
    public HeavyBigInteger(ArrayList<BigInteger> vals) {
        this.number = new ArrayList<>(vals);
    }
    public HeavyBigInteger(@NotNull HeavyBigInteger val) {
        this.number = new ArrayList<>(val.number);
    }

    @Override
    public int compareTo(@NotNull HeavyBigInteger o) {
        if (this.number.equals(o.number)) return 0;
        if (this.signum() != o.signum()) return this.signum() < o.signum() ? -1 : 1 ;
        if (this.number.size() == o.number.size()) {
            for (int i = this.number.size() - 1; i >= 0; i--) {
                if (this.number.get(i).compareTo(o.number.get(i)) == 0) continue;
                return this.number.get(i).compareTo(o.number.get(i));
            }
            return 0;
        } else {
            if (this.number.isEmpty()) return o.signum();
            else if (o.number.isEmpty()) return this.signum();
            return ((this.number.size() > o.number.size()) ? 1 : -1) * this.signum();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof HeavyBigInteger) {
            return this.compareTo((HeavyBigInteger)o) == 0;
        }
        return false;
    }

    @Override
    public int intValue() {
        if (this.number.isEmpty()) return 0;
        return (this.number.size() > 1) ? (this.signum() < 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE) : this.number.get(0).intValue();
    }

    @Override
    public long longValue() {
        if (this.number.isEmpty()) return 0;
        return (this.number.size() > 1) ? (this.signum() < 0 ? Long.MIN_VALUE : Long.MAX_VALUE) : this.number.get(0).longValue();
    }

    @Override
    public float floatValue() {
        if (this.number.isEmpty()) return 0;
        return (this.number.size() > 1) ? (this.signum() < 0 ? -Float.MAX_VALUE : Float.MAX_VALUE) : this.number.get(0).floatValue();
    }

    @Override
    public double doubleValue() {
        if (this.number.isEmpty()) return 0;
        return (this.number.size() > 1) ? (this.signum() < 0 ? -Double.MAX_VALUE : Double.MAX_VALUE) : this.number.get(0).doubleValue();
    }

    public int signum() {
        if (this.number.isEmpty()) return 0;
        return this.number.get(this.number.size() - 1).signum();
    }

    public HeavyBigInteger negate() {
            HeavyBigInteger res = new HeavyBigInteger(this);
            res.number.set(res.number.size() - 1, res.number.get(res.number.size() - 1).negate());
            return res;
    }

    public HeavyBigInteger abs() {
        return (this.signum() > 0) ? this : this.negate();
    }

    public String toString() {
        if (this.number.isEmpty()) return "0";
        if (this.abs().compareTo(approximateLimit) <= 0) return this.number.get(0).toString();
        // belows are for approximated toString (in hexadecimal P Notation)
        StringBuilder res = new StringBuilder(this.signum() >= 0 ? "0x" : "-0x");
        int fractionHexDigits = 13;
        byte[] fractionHex = this.abs().shiftRight(this.abs().getCurrentBinDigits().subtract(BigInteger.valueOf(fractionHexDigits * 4 + 1))).toByteArray();
        for (byte hex : fractionHex) {
            res.append(String.format("%02X", Byte.toUnsignedInt(hex)));
        }
        return res.insert(res.indexOf("1") + 1,'.').append("p").append(this.abs().getCurrentBinDigits().subtract(BigInteger.ONE).toString(10)).toString();
    }

    private byte[] toByteArray() {
        if (this.number.isEmpty()) return new byte[0];
        return this.number.get(0).toByteArray();
    }

    public BigInteger getCurrentBinDigits() {
        BigInteger res = BigInteger.valueOf(Integer.MAX_VALUE).multiply(BigInteger.valueOf(this.number.size() - 1));
        return res.add(BigInteger.valueOf(this.number.get(this.number.size() - 1).bitLength()));
    }

    public HeavyBigInteger shiftLeft(@NotNull BigInteger val) {
        if (val.signum() == -1) return this.shiftRight(val.negate());
        if (val.signum() == 0) return this;// nothing to do :)

        BigInteger shiftable = MAX_INT_POW2.subtract(this.getCurrentBinDigits()).subtract(BigInteger.ONE);
        if (val.compareTo(shiftable) >= 0) throw new ArithmeticException("Exceed safe shift range");
        int[] shiftCounts = Arrays.stream(val.divideAndRemainder(BigInteger.valueOf(Integer.MAX_VALUE))).mapToInt(BigInteger::intValue).toArray();
        // prepare copy
        ArrayList<BigInteger> shifted = new ArrayList<>(this.number);
        for (int i = 0; i < shiftCounts[0]; i++) {
            shifted.add(0, BigInteger.ZERO);
        }
        for (int i = shifted.size() - 1; i >= 0; i--) {
            if ((long) Integer.MAX_VALUE < (long) shiftCounts[1] + shifted.get(i).bitLength()) {
                // process carry bits across BigInteger
                int additionalShift = Math.toIntExact((long) shiftCounts[1] + shifted.get(i).bitLength() - Integer.MAX_VALUE);
                BigInteger p = shifted.get(i).shiftRight(shifted.get(i).bitLength() - additionalShift);
                if (i == shifted.size() - 1) {
                    shifted.add(p);
                } else {
                    shifted.set(i + 1, shifted.get(i + 1).add(p));
                }
                shifted.set(i, shifted.get(i).subtract(p.shiftLeft(shifted.get(i).bitLength() - additionalShift)).shiftLeft(shiftCounts[1]));
            } else {
                shifted.set(i, shifted.get(i).shiftLeft(shiftCounts[1]));
            }
        }
        return new HeavyBigInteger(shifted);
    }

    public HeavyBigInteger shiftRight(@NotNull BigInteger val) {
        if (val.signum() == -1) return this.shiftLeft(val.negate());
        if (val.signum() == 0) return this;
        if (val.compareTo(this.getCurrentBinDigits()) >= 0) return new HeavyBigInteger(BigInteger.ZERO);

        int[] shiftCounts = Arrays.stream(val.divideAndRemainder(BigInteger.valueOf(Integer.MAX_VALUE))).mapToInt(BigInteger::intValue).toArray();
        ArrayList<BigInteger> shifted = new ArrayList<>(this.number);
        for (int i = shiftCounts[0]; i > 0; i--) {
            shifted.remove(0);
        }
        for (int i = 0; i <= shifted.size() - 1; i++) {
            if (i > 0) {
                BigInteger p = shifted.get(i).shiftRight(shiftCounts[1]).shiftLeft(shiftCounts[1]);
                shifted.set(i-1, shifted.get(i-1).add(shifted.get(i).subtract(p).shiftLeft(Integer.MAX_VALUE - shiftCounts[1])));
            }
            shifted.set(i, shifted.get(i).shiftRight(shiftCounts[1]));
        }
        for (int i = shifted.size() - 1; i >= 0; i--) {
            if (shifted.get(i).signum() == 0) shifted.remove(i);
        }
        return new HeavyBigInteger(shifted);
    }

    public HeavyBigInteger add(@NotNull HeavyBigInteger val) {
        if (val.signum() == 0) return new HeavyBigInteger(this);
        if (this.signum() == 0) return new HeavyBigInteger(val);
        if (val.signum() != this.signum()) return this.subtract(val.negate());

        HeavyBigInteger res = new HeavyBigInteger();
        for (int i = 0; i < Math.max(this.number.size(), val.number.size()); i++) {
            if (res.number.size() == i) res.number.add(BigInteger.ZERO);
            if (this.number.size() > i && val.number.size() > i) {
                if (Math.max(this.number.get(i).bitLength(), val.number.get(i).bitLength()) == Integer.MAX_VALUE) {
                    BigInteger sum;
                    sum = MAX_BIGINT.negate().add(this.number.get(i).abs()).add(val.number.get(i).abs()).subtract(BigInteger.ONE);
                    if (sum.signum() >= 0) {
                        if (this.signum() > 0) {
                            res.number.add(BigInteger.ONE);
                        } else {
                            res.number.add(BigInteger.ONE.negate());
                        }
                        res.number.set(i, sum);
                    } else {
                        res.number.set(i, sum.add(MAX_BIGINT));
                    }
                } else {
                    res.number.set(i, res.number.get(i).add(this.number.get(i)).add(val.number.get(i)));
                }
            } else {
                if (this.number.size() > val.number.size()) {
                    if (this.number.get(i).compareTo(MAX_BIGINT) < 0) {
                        res.number.set(i, res.number.get(i).add(this.number.get(i)));
                    } else if (res.number.get(i).signum() == 0) {
                        res.number.set(i, MAX_BIGINT);
                    } else {
                        res.number.set(i, BigInteger.ZERO);
                        res.number.add(BigInteger.ONE);
                    }
                } else {
                    if (val.number.get(i).compareTo(MAX_BIGINT) < 0) {
                        res.number.set(i, res.number.get(i).add(val.number.get(i)));
                    } else if (res.number.get(i).signum() == 0) {
                        res.number.set(i, MAX_BIGINT);
                    } else {
                        res.number.set(i, BigInteger.ZERO);
                        res.number.add(BigInteger.ONE);
                    }
                }
            }
        }
        return res;
    }

    public HeavyBigInteger subtract(@NotNull HeavyBigInteger val) {
        if (val.signum() == 0) return new HeavyBigInteger(this);
        if (this.signum() == 0) return val.negate();
        if (val.signum() != this.signum()) return this.add(val.negate());

        HeavyBigInteger res = new HeavyBigInteger(this);
        boolean isNegative = this.signum() < 0;
        res.number.set(res.number.size() - 1, res.number.get(res.number.size() - 1).abs());
        for (int i = 0; i < Math.max(this.number.size(), val.number.size()); i++) {
            if (res.number.size() <= i) {
                res.number.add(val.number.get(i).abs().negate());
                if (res.number.get(i - 1).signum() > 0) {
                    res.number.set(i - 1, res.number.get(i - 1).subtract(MAX_BIGINT).subtract(BigInteger.ONE));
                    res.number.set(i, res.number.get(i).add(BigInteger.ONE));
                } else {
                    res.number.set(i - 1, res.number.get(i - 1).negate());
                }
            } else {
                if (val.number.size() > i) {
                    res.number.set(i, res.number.get(i).subtract(val.number.get(i).abs()));
                }
                if (res.number.get(i).signum() < 0 && res.number.size() > i + 1) {
                    // borrow one from next BigInteger
                    res.number.set(i, res.number.get(i).add(MAX_BIGINT).add(BigInteger.ONE));
                    res.number.set(i + 1, res.number.get(i + 1).subtract(BigInteger.ONE));
                }
            }
        }
        while (res.number.get(res.number.size() - 1).signum() == 0 && res.number.size() > 1) res.number.remove(res.number.size() - 1);
        return isNegative ? res.negate() : res;
    }
}
