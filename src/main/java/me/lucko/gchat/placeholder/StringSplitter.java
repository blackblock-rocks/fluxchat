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
                result.add(new Entry(plainText));
                break;
            }

            // Determine the next occurrence
            if (openingTagIndex != -1 && (placeholderIndex == -1 || openingTagIndex < placeholderIndex)) {
                // Found an opening tag
                if (openingTagIndex > start_index) {
                    String plainText = input.substring(start_index, openingTagIndex);
                    result.add(new Entry(plainText));
                }

                // Find the corresponding closing tag
                int closingTagIndex = input.indexOf(">", openingTagIndex);
                if (closingTagIndex == -1) {
                    // Invalid tag format, treat it as plain text
                    String invalidTag = input.substring(openingTagIndex, openingTagIndex + 1);
                    result.add(new Entry(invalidTag));
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
                    result.add(new Entry(plainText));
                }

                // Find the corresponding closing placeholder
                int closingPlaceholderIndex = input.indexOf("}", placeholderIndex);
                if (closingPlaceholderIndex == -1) {
                    // Invalid placeholder format, treat it as plain text
                    String invalidPlaceholder = input.substring(placeholderIndex, placeholderIndex + 1);
                    result.add(new Entry(invalidPlaceholder));
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
         * See if this entry is an opening tag of some kind
         *
         * @author   Jelle De Loecker
         * @since    3.2.0
         */
        public boolean isOpeningTag() {
            return this.type == EntryType.OPENING_TAG;
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

            if (color_name.equals("bold")) {
                this.is_bold = true;
                return;
            } else if (color_name.equals("italic")) {
                this.is_italic = true;
                return;
            } else if (color_name.equals("obfuscated")) {
                this.is_obfuscated = true;
                return;
            } else if (color_name.equals("underlined")) {
                this.is_underlined = true;
                return;
            } else if (color_name.equals("strikethrough")) {
                this.is_strikethrough = true;
                return;
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
        CLOSING_TAG
    }
}
