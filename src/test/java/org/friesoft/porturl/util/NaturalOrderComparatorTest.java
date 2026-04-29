package org.friesoft.porturl.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NaturalOrderComparatorTest {

    private final NaturalOrderComparator comparator = new NaturalOrderComparator();

    @Test
    void compare_sortsNumerically() {
        assertTrue(comparator.compare("App 2", "App 10") < 0);
        assertTrue(comparator.compare("10", "2") > 0);
        assertTrue(comparator.compare("App 01", "App 1") > 0);
    }

    @Test
    void compare_isCaseInsensitive() {
        assertEquals(0, comparator.compare("abc", "ABC"));
        assertTrue(comparator.compare("abc", "abd") < 0);
    }

    @Test
    void compare_handlesNulls() {
        assertTrue(comparator.compare(null, "a") < 0);
        assertTrue(comparator.compare("a", null) > 0);
        assertEquals(0, comparator.compare(null, null));
    }

    @Test
    void compare_complexStrings() {
        List<String> list = Arrays.asList("z10.txt", "z1.txt", "z2.txt", "z20.txt");
        list.sort(comparator);
        assertEquals(Arrays.asList("z1.txt", "z2.txt", "z10.txt", "z20.txt"), list);
    }
}
