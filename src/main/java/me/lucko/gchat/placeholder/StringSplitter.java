package me.lucko.gchat.placeholder;

import java.util.ArrayList;
import java.util.List;

/**
 * Split a string based on placeholders
 *
 * @author   Jelle De Loecker
 * @since    3.2.0
 */
public class StringSplitter {

    public static List<Entry> parse(String input) {

        List<Entry> result = new ArrayList<>();
        int start_index = 0;
        int end_index;

        while (start_index < input.length()) {
            end_index = input.indexOf("{", start_index);

            if (end_index == -1) {
                // If there are no more "{", add the remaining part of the string
                result.add(new Entry(input.substring(start_index)));
                break;
            }

            if (end_index > start_index) {
                // Add the substring before the "{"
                result.add(new Entry(input.substring(start_index, end_index)));
            }

            start_index = end_index;

            end_index = input.indexOf("}", start_index + 1);

            if (end_index == -1) {
                // If there is no matching "}", add the remaining part of the string
                result.add(new Entry(input.substring(start_index)));
                break;
            }

            // Add the substring within the curly braces "{ }"
            result.add(new Entry(input.substring(start_index + 1, end_index), true));

            start_index = end_index + 1;
        }

        return result;
    }


    /**
     * An entry representing a piece of text (or a placeholder)
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    public static class Entry {

        private final String string;
        private final boolean is_placeholder;

        public Entry(String simple_text) {
            this.string = simple_text;
            this.is_placeholder = false;
        }

        public Entry(String original_text, boolean is_placeholder) {
            this.string = original_text;
            this.is_placeholder = is_placeholder;
        }

        public boolean isPlaceholder() {
            return this.is_placeholder;
        }

        public String getString() {
            return this.string;
        }
    }
}
