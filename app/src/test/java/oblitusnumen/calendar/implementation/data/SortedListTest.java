package oblitusnumen.calendar.implementation.data;

import junit.framework.TestCase;

import java.util.*;

public class SortedListTest extends TestCase {

    public void testAdd2() {
        String[] strings = {"a", "c", "r", "g", "b", "m", "d", "e"};
        SortedList<A> list = new SortedList<>(Comparator.comparing(e -> e.s));
        for (int i = 0; i < 2; i++) {
            for (String string : strings) {
                list.add(new A(string, i));
            }
        }
        List<A> expect = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            for (String string : strings) {
                expect.add(new A(string, i));
            }
        }
        expect.sort(Comparator.comparing(e -> e.s));
        assertEquals(expect.toString(), list.toString());
        System.out.println(list);
    }

    public void testAdd() {
        String[] strings = {"a", "c", "r", "g", "b", "m", "d", "e"};
        SortedList<String> list = new SortedList<>(Comparator.naturalOrder());
        for (int i = 0; i < 2; i++) {
            for (String string : strings) {
                list.add(string);
            }
        }
        List<String> expect = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Collections.addAll(expect, strings);
        }
        expect.sort(Comparator.naturalOrder());
        assertEquals(String.join("\n", expect), String.join("\n", list));
    }

    record A(String s, int i) {
    }
}