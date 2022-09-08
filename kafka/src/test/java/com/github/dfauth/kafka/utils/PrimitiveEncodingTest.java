package com.github.dfauth.kafka.utils;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class PrimitiveEncodingTest {

    @Test
    public void testString() {
        String i = "test";
        byte[] b = PrimitiveEncoding.trySerialize(i).orElseThrow();
        assertNotNull(b);
        assertEquals(6, b.length);
        Object result = PrimitiveEncoding.tryDeserialize(b).orElseThrow();
        assertNotNull(result);
        assertTrue(result instanceof String);
        assertEquals(i, result);
    }

    @Test
    public void testInt() {
        int i = 1;
        byte[] b = PrimitiveEncoding.trySerialize(i).orElseThrow();
        assertNotNull(b);
        assertEquals(6, b.length);
        Object result = PrimitiveEncoding.tryDeserialize(b).orElseThrow();
        assertNotNull(result);
        assertTrue(result instanceof Integer);
        assertEquals(i, result);
    }

    @Test
    public void testBoolean() {
        {
            boolean i = true;
            byte[] b = PrimitiveEncoding.trySerialize(i).orElseThrow();
            assertNotNull(b);
            assertEquals(3, b.length);
            Object result = PrimitiveEncoding.tryDeserialize(b).orElseThrow();
            assertNotNull(result);
            assertTrue(result instanceof Boolean);
            assertEquals(i, result);
        }
        {
            boolean i = false;
            byte[] b = PrimitiveEncoding.trySerialize(i).orElseThrow();
            assertNotNull(b);
            assertEquals(3, b.length);
            Object result = PrimitiveEncoding.tryDeserialize(b).orElseThrow();
            assertNotNull(result);
            assertTrue(result instanceof Boolean);
            assertEquals(i, result);
        }
    }

    @Test
    public void testShort() {
        short i = 1;
        byte[] b = PrimitiveEncoding.trySerialize(i).orElseThrow();
        assertNotNull(b);
        assertEquals(4, b.length);
        Object result = PrimitiveEncoding.tryDeserialize(b).orElseThrow();
        assertNotNull(result);
        assertTrue(result instanceof Short);
        assertEquals(i, result);
    }

    @Test
    public void testChar() {
        char i = 'c';
        byte[] b = PrimitiveEncoding.trySerialize(i).orElseThrow();
        assertNotNull(b);
        assertEquals(6, b.length);
        Object result = PrimitiveEncoding.tryDeserialize(b).orElseThrow();
        assertNotNull(result);
        assertTrue(result instanceof Character);
        assertEquals(i, result);
    }

    @Test
    public void testDouble() {
        double i = 0.2;
        byte[] b = PrimitiveEncoding.trySerialize(i).orElseThrow();
        assertNotNull(b);
        assertEquals(10, b.length);
        Object result = PrimitiveEncoding.tryDeserialize(b).orElseThrow();
        assertNotNull(result);
        assertTrue(result instanceof Double);
        assertEquals(i, result);
    }

    @Test
    public void testFloat() {
        float i = 0.2f;
        byte[] b = PrimitiveEncoding.trySerialize(i).orElseThrow();
        assertNotNull(b);
        assertEquals(10, b.length);
        Object result = PrimitiveEncoding.tryDeserialize(b).orElseThrow();
        assertNotNull(result);
        assertTrue(result instanceof Float);
        assertEquals(i, result);
    }

    @Test
    public void testLong() {
        long i = 1;
        byte[] b = PrimitiveEncoding.trySerialize(i).orElseThrow();
        assertNotNull(b);
        assertEquals(10, b.length);
        Object result = PrimitiveEncoding.tryDeserialize(b).orElseThrow();
        assertNotNull(result);
        assertTrue(result instanceof Long);
        assertEquals(i, result);
    }

    @Test
    public void testOther() {
        // no magic number, should fail
        Optional<Object> result = PrimitiveEncoding.tryDeserialize("BLAH".getBytes());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
