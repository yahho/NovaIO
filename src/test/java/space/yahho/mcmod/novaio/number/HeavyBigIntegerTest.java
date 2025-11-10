package space.yahho.mcmod.novaio.number;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class HeavyBigIntegerTest {

    @Test
    @DisplayName("BigInteger間を跨ぐ左シフト")
    void shiftLeft() {
        ArrayList<BigInteger> twoZero = new ArrayList<>();
        twoZero.add(BigInteger.TWO);
        twoZero.add(0, BigInteger.ZERO);
        HeavyBigInteger heavyBigInteger = new HeavyBigInteger(twoZero);
        assertEquals(heavyBigInteger, HeavyBigInteger.ONE.shiftLeft(BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE)));
    }

    @Test
    @DisplayName("BigInteger間を跨ぐ右シフト")
    void shiftRight() {
        ArrayList<BigInteger> oneZero = new ArrayList<>();
        oneZero.add(BigInteger.ONE);
        oneZero.add(0, BigInteger.ZERO);
        HeavyBigInteger heavyBigInteger = new HeavyBigInteger(oneZero);
        assertEquals(new HeavyBigInteger(BigInteger.ONE.shiftLeft(Integer.MAX_VALUE - 1)), heavyBigInteger.shiftRight(BigInteger.ONE));
    }

    @Test
    @DisplayName("BigInteger間を跨ぐ加算")
    void add() {
        ArrayList<BigInteger> oneZero = new ArrayList<>();
        oneZero.add(BigInteger.ONE);
        oneZero.add(0, BigInteger.ZERO);
        HeavyBigInteger heavyBigInteger = new HeavyBigInteger(oneZero);
        assertEquals(heavyBigInteger, new HeavyBigInteger(HeavyBigInteger.MAX_BIGINT).add(HeavyBigInteger.ONE));
        assertEquals(heavyBigInteger.negate(), new HeavyBigInteger(HeavyBigInteger.MAX_BIGINT).negate().add(HeavyBigInteger.ONE.negate()));
    }

    @Test
    @DisplayName("BigInteger間を跨ぐ減算")
    void subtract() {
        ArrayList<BigInteger> oneZero = new ArrayList<>();
        oneZero.add(BigInteger.ONE);
        oneZero.add(0, BigInteger.ZERO);
        HeavyBigInteger heavyBigInteger = new HeavyBigInteger(oneZero);
        assertEquals(new HeavyBigInteger(HeavyBigInteger.MAX_BIGINT), heavyBigInteger.subtract(HeavyBigInteger.ONE));
        assertEquals(new HeavyBigInteger(HeavyBigInteger.MAX_BIGINT).negate(), heavyBigInteger.negate().subtract(HeavyBigInteger.ONE.negate()));
    }
}