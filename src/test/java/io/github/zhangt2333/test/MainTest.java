package io.github.zhangt2333.test;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void getMsg() {
        assertEquals("Hello, Maven", Main.getMsg());
    }

}
