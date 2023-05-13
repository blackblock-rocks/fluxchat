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

    public static List<Entry> parse(String input) {

        List<Entry> result = new ArrayList<>();
        int start_index = 0;
        int length = input.length();

        while (start_index < length) {

            // Find the next opening tag or placeholder
            int openingTagIndex = input.indexOf("<", start_index);
            int placeholderIndex = input.indexOf("{", start_index);

            if (openingTagIndex == -1 && placeholderIndex == -1) {
                // No more tags or placeholders found, add the remaining plain text
                String plainText = input.substring(start_index);
                Entry.parseFurther(plainText, result);
                break;
            }

            // Determine the next occurrence
            if (openingTagIndex != -1 && (placeholderIndex == -1 || openingTagIndex < placeholderIndex)) {
                // Found an opening tag
                if (openingTagIndex > start_index) {
                    String plainText = input.substring(start_index, openingTagIndex);
                    Entry.parseFurther(plainText, result);
                }

                // Find the corresponding closing tag
                int closingTagIndex = input.indexOf(">", openingTagIndex);
                if (closingTagIndex == -1) {
                    // Invalid tag format, treat it as plain text
                    String invalidTag = input.substring(openingTagIndex, openingTagIndex + 1);
                    Entry.parseFurther(invalidTag, result);
                    start_index = openingTagIndex + 1;
                } else {
                    // Extract the tag content
                    String tagContent = input.substring(openingTagIndex + 1, closingTagIndex);
                    result.add(new Entry(tagContent, EntryType.OPENING_TAG));
                    start_index = closingTagIndex + 1;
                }
            } else if (placeholderIndex != -1 && (openingTagIndex == -1 || placeholderIndex < openingTagIndex)) {
                // Found a placeholder
                if (placeholderIndex > start_index) {
                    String plainText = input.substring(start_index, placeholderIndex);
                    Entry.parseFurther(plainText, result);
                }

                // Find the corresponding closing placeholder
                int closingPlaceholderIndex = input.indexOf("}", placeholderIndex);
                if (closingPlaceholderIndex == -1) {
                    // Invalid placeholder format, treat it as plain text
                    String invalidPlaceholder = input.substring(placeholderIndex, placeholderIndex + 1);
                    Entry.parseFurther(invalidPlaceholder, result);
                    start_index = placeholderIndex + 1;
                } else {
                    // Extract the placeholder content
                    String placeholderContent = input.substring(placeholderIndex + 1, closingPlaceholderIndex);
                    result.add(new Entry(placeholderContent, EntryType.PLACEHOLDER));
                    start_index = closingPlaceholderIndex + 1;
                }
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

            String color_name = this.string.toLowerCase();

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
                            this.color = TextColor.color(Integer.parseInt(code, 16));
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
         * Parse a string that does not contain any placeholders or tags
         *
         * @author   Jelle De Loecker
         * @since    3.2.0
         */
        public static void parseFurther(String text, List<Entry> list) {

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
