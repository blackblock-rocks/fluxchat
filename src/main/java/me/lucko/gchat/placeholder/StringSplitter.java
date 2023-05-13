package me.lucko.gchat.placeholder;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Split a string based on placeholders
 *
 * @author   Jelle De Loecker
 * @since    3.2.0
 */
public class StringSplitter {

    private enum State {
        PLAIN_TEXT,
        OPEN_TAG,
        CLOSE_TAG,
        PLACEHOLDER
    }

    /**
     * Convert a string into a splitted string list
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    public static SplittedStringList parse(String input) {

        SplittedStringList result = new SplittedStringList();

        if (input == null || input.isEmpty()) {
            return result;
        }

        State current_state = State.PLAIN_TEXT;

        int length = input.length();
        int current_index = 0;
        StringBuilder string_buffer = new StringBuilder();

        for (current_index = 0; current_index < length; current_index++) {

            // We're going to parse this string 1 char at a time
            char current_char = input.charAt(current_index);
            State new_state = null;

            if (current_state == State.PLAIN_TEXT) {
                if (current_char == '<') {

                    // Look for the closing '>' character
                    int closing_index = input.indexOf('>', current_index);

                    if (closing_index == -1) {
                        string_buffer.append(current_char);
                        continue;
                    }

                    // See if there is another '<' character before the closing '>'
                    int opening_index = input.indexOf('<', current_index + 1);

                    if (opening_index != -1 && opening_index < closing_index) {
                        // There is another '<' before the closing '>', so this is not a tag
                        string_buffer.append(current_char);
                        continue;
                    }

                    // See if there is a '{' character before the closing '>'
                    opening_index = input.indexOf('{', current_index + 1);

                    if (opening_index != -1 && opening_index < closing_index) {
                        // There is a '{' before the closing '>', so this is not a tag
                        string_buffer.append(current_char);
                        continue;
                    }

                    // Get the next character
                    char next_char = input.charAt(current_index + 1);

                    if (next_char == '/') {
                        // This is a closing tag
                        new_state = State.CLOSE_TAG;
                        current_index++;
                    } else {

                        // If the next_char is not a letter, this is not a tag
                        if (!Character.isLetter(next_char)) {
                            string_buffer.append(current_char);
                            continue;
                        }

                        // This is an opening tag
                        new_state = State.OPEN_TAG;
                    }

                } else if (current_char == '{') {
                    new_state = State.PLACEHOLDER;
                }
            } else if (current_state == State.OPEN_TAG || current_state == State.CLOSE_TAG) {
                if (current_char == '>') {
                    new_state = State.PLAIN_TEXT;
                }
            } else if (current_state == State.PLACEHOLDER) {
                if (current_char == '}') {
                    new_state = State.PLAIN_TEXT;
                }
            }

            // Keep appending chars as long as the state doesn't change
            if (new_state == null) {
                string_buffer.append(current_char);
                continue;
            }

            if (!string_buffer.isEmpty()) {
                String current_string_result = string_buffer.toString();

                if (current_state == State.PLAIN_TEXT) {
                    // Add the plain text
                    Entry.parseSimplerText(current_string_result, result);
                } else if (current_state == State.OPEN_TAG) {
                    // Add the open tag
                    result.add(new Entry(current_string_result, EntryType.OPENING_TAG));
                } else if (current_state == State.CLOSE_TAG) {
                    // Add the close tag
                    result.add(new Entry(current_string_result, EntryType.CLOSING_TAG));
                } else if (current_state == State.PLACEHOLDER) {
                    // Add the placeholder
                    result.add(new Entry(current_string_result, EntryType.PLACEHOLDER));
                }

                string_buffer.setLength(0);
            }

            current_state = new_state;
        }

        if (!string_buffer.isEmpty()) {
            // Add the last part
            String current_string_result = string_buffer.toString();

            if (current_state == State.PLAIN_TEXT) {
                // Add the plain text
                Entry.parseSimplerText(current_string_result, result);
            } else if (current_state == State.OPEN_TAG) {
                // Add the open tag
                result.add(new Entry(current_string_result, EntryType.OPENING_TAG));
            } else if (current_state == State.CLOSE_TAG) {
                // Add the close tag
                result.add(new Entry(current_string_result, EntryType.CLOSING_TAG));
            } else if (current_state == State.PLACEHOLDER) {
                // Add the placeholder
                result.add(new Entry(current_string_result, EntryType.PLACEHOLDER));
            }
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
        private final EntryType type;
        private TextColor color = null;
        private boolean parsed_tag = false;
        private boolean is_bold = false;
        private boolean is_italic = false;
        private boolean is_obfuscated = false;
        private boolean is_underlined = false;
        private boolean is_strikethrough = false;
        private boolean allows_decoration = true;
        private boolean is_reset = false;
        private String tag_name = null;

        /**
         * Instantiate a new entry as plain text
         *
         * @author   Jelle De Loecker
         * @since    3.2.0
         */
        public Entry(String content) {
            this(content, EntryType.PLAIN_TEXT);
        }

        /**
         * Instantiate a new entry with a specific type
         *
         * @author   Jelle De Loecker
         * @since    3.2.0
         */
        public Entry(String content, EntryType type) {

            if (type == EntryType.PLACEHOLDER && content.startsWith("!")) {
                this.allows_decoration = false;
                content = content.substring(1);
            }

            this.string = content;
            this.type = type;
        }

        /**
         * Is this a reset?
         *
         * @author   Jelle De Loecker
         * @since    3.2.0
         */
        public boolean isReset() {
            this.parseTag();
            return this.is_reset;
        }

        /**
         * See if this entry is a placeholder key
         *
         * @author   Jelle De Loecker
         * @since    3.2.0
         */
        public boolean isPlaceholder() {
            return this.type == EntryType.PLACEHOLDER;
        }

        /**
         * Is this a color code?
         *
         * @author   Jelle De Loecker
         * @since    3.2.0
         */
        public boolean isColorCode() {
            return this.type == EntryType.COLOR_CODE;
        }

        /**
         * See if this entry is an opening tag of some kind
         *
         * @author   Jelle De Loecker
         * @since    3.2.0
         */
        public boolean isOpeningTag() {
            return this.type == EntryType.OPENING_TAG || this.type == EntryType.COLOR_CODE;
        }

        /**
         * See if this entry is a closing tag
         *
         * @author   Jelle De Loecker
         * @since    3.2.0
         */
        public boolean isClosingTag() {
            return this.type == EntryType.CLOSING_TAG;
        }

        /**
         * Get the tag name
         *
         * @author   Jelle De Loecker
         * @since    3.2.0
         */
        @Nullable
        public String getTagName() {
            this.parseTag();
            return this.tag_name;
        }

        /**
         * In case of a placeholder: should its replacement
         * be parsed for decorations?
         *
         * @author   Jelle De Loecker
         * @since    3.2.0
         */
        public boolean allowsDecoration() {
            return this.allows_decoration;
        }

        /**
         * Apply the style to the given builder
         *
         * @author   Jelle De Loecker
         * @since    3.2.0
         */
        public void applyStyle(TextComponent.Builder builder) {

            if (this.isOpeningTag()) {
                this.parseTag();
            } else {
                return;
            }

            if (this.is_bold) {
                builder.decoration(TextDecoration.BOLD, true);
            }

            if (this.is_italic) {
                builder.decoration(TextDecoration.ITALIC, true);
            }

            if (this.is_obfuscated) {
                builder.decoration(TextDecoration.OBFUSCATED, true);
            }

            if (this.is_underlined) {
                builder.decoration(TextDecoration.UNDERLINED, true);
            }

            if (this.is_strikethrough) {
                builder.decoration(TextDecoration.STRIKETHROUGH, true);
            }

            if (this.color != null) {
                builder.color(this.color);
            }
        }

        /**
         * Parse the tag
         *
         * @author   Jelle De Loecker
         * @since    3.2.0
         */
        private void parseTag() {

            if (this.parsed_tag) {
                return;
            }

            this.parsed_tag = true;

            if (!this.isOpeningTag() && !this.isClosingTag()) {
                return;
            }

            String color_name = this.string.toLowerCase().trim();

            if (this.isClosingTag()) {
                this.tag_name = color_name;
                return;
            }

            this.tag_name = color_name;

            if (this.isColorCode()) {
                String code = color_name.substring(1);

                switch (code) {
                    case "l" -> this.is_bold = true;
                    case "o" -> this.is_italic = true;
                    case "k" -> this.is_obfuscated = true;
                    case "n" -> this.is_underlined = true;
                    case "m" -> this.is_strikethrough = true;
                    case "r" -> this.is_reset = true;
                    default -> {
                        try {
                            this.color = this.parseColorCode(code);
                        } catch (Throwable nfe) {
                            // Ignore
                            this.color = null;
                        }
                    }
                }

                return;
            }

            switch (color_name) {
                case "bold" -> {
                    this.is_bold = true;
                    return;
                }
                case "italic" -> {
                    this.is_italic = true;
                    return;
                }
                case "obfuscated" -> {
                    this.is_obfuscated = true;
                    return;
                }
                case "underlined" -> {
                    this.is_underlined = true;
                    return;
                }
                case "strikethrough" -> {
                    this.is_strikethrough = true;
                    return;
                }
                case "reset" -> {
                    this.is_reset = true;
                    return;
                }
            }

            // If the color starts with a hashtag, it's a numeric value
            if (color_name.startsWith("#")) {
                try {
                    int color_value = Integer.parseInt(color_name.substring(1), 16);
                    this.color = TextColor.color(color_value);
                    return;
                } catch (NumberFormatException nfe) {
                    // Ignore
                }
            }

            // Remove all non-alphanumeric characters
            color_name = color_name.replaceAll("[^a-z0-9]", "");

            this.color = switch (color_name) {
                case "black" -> NamedTextColor.BLACK;
                case "darkblue" -> NamedTextColor.DARK_BLUE;
                case "darkgreen" -> NamedTextColor.DARK_GREEN;
                case "darkaqua" -> NamedTextColor.DARK_AQUA;
                case "darkred" -> NamedTextColor.DARK_RED;
                case "darkpurple" -> NamedTextColor.DARK_PURPLE;
                case "gold" -> NamedTextColor.GOLD;
                case "gray" -> NamedTextColor.GRAY;
                case "darkgray" -> NamedTextColor.DARK_GRAY;
                case "blue" -> NamedTextColor.BLUE;
                case "green" -> NamedTextColor.GREEN;
                case "aqua" -> NamedTextColor.AQUA;
                case "red" -> NamedTextColor.RED;
                case "lightpurple" -> NamedTextColor.LIGHT_PURPLE;
                case "yellow" -> NamedTextColor.YELLOW;
                case "white" -> NamedTextColor.WHITE;
                default -> null;
            };
        }

        /**
         * Parse a color code
         *
         * @author   Jelle De Loecker
         * @since    3.2.0
         */
        @Nullable
        private TextColor parseColorCode(String code) {

            if (code.length() == 2) {
                return this.parseColorCode(code.substring(1));
            }

            TextColor result = switch (code) {
                case "0" -> NamedTextColor.BLACK;
                case "1" -> NamedTextColor.DARK_BLUE;
                case "2" -> NamedTextColor.DARK_GREEN;
                case "3" -> NamedTextColor.DARK_AQUA;
                case "4" -> NamedTextColor.DARK_RED;
                case "5" -> NamedTextColor.DARK_PURPLE;
                case "6" -> NamedTextColor.GOLD;
                case "7" -> NamedTextColor.GRAY;
                case "8" -> NamedTextColor.DARK_GRAY;
                case "9" -> NamedTextColor.BLUE;
                case "a" -> NamedTextColor.GREEN;
                case "b" -> NamedTextColor.AQUA;
                case "c" -> NamedTextColor.RED;
                case "d" -> NamedTextColor.LIGHT_PURPLE;
                case "e" -> NamedTextColor.YELLOW;
                case "f" -> NamedTextColor.WHITE;
                default -> null;
            };

            return result;
        }

        /**
         * Get the color this entry starts
         *
         * @author   Jelle De Loecker
         * @since    3.2.0
         */
        @Nullable
        public TextColor getColor() {
            this.parseTag();
            return this.color;
        }

        public EntryType getType() {
            return this.type;
        }

        public String getContent() {
            return this.string;
        }

        /**
         * Parse a string that does not contain any placeholders or tags,
         * but might contain formatting codes.
         *
         * @author   Jelle De Loecker
         * @since    3.2.0
         */
        public static void parseSimplerText(String text, SplittedStringList list) {

            // The text can look like this at this point: "§eYellow §cRed"
            // We need to split it up into separate entries: "§e", "Yellow ", "§c", "Red"

            List<String> parts = new ArrayList<>();
            StringBuilder builder = new StringBuilder();
            boolean last_was_tag = false;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);

                if (c == '§') {
                    if (builder.length() > 0) {
                        parts.add(builder.toString());
                        builder.setLength(0);
                    }

                    builder.append(c);
                    last_was_tag = true;
                } else {
                    if (last_was_tag) {
                        builder.append(c);
                        parts.add(builder.toString());
                        builder.setLength(0);
                        last_was_tag = false;
                        continue;
                    }

                    builder.append(c);
                }
            }

            if (!builder.isEmpty()) {
                parts.add(builder.toString());
            }

            for (String part : parts) {
                if (part.isEmpty()) {
                    continue;
                }

                if (part.charAt(0) == '§') {
                    list.add(new Entry(part, EntryType.COLOR_CODE));
                } else {
                    list.add(new Entry(part, EntryType.PLAIN_TEXT));
                }
            }
        }
    }

    /**
     * The different type of entries
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    public enum EntryType {
        PLAIN_TEXT,
        PLACEHOLDER,
        OPENING_TAG,
        CLOSING_TAG,
        COLOR_CODE
    }
}
