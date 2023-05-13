package me.lucko.gchat.placeholder;

import me.lucko.gchat.GChatPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Actually convert a SplittedStringList to a TextComponent.
 *
 * @author   Jelle De Loecker
 * @since    3.2.0
 */
public class SplittedStringConverter {

    private final SplittedStringList list;
    private final GChatPlayer.PlaceholderResolver resolver;
    private TextComponent.Builder root = null;
    private TextComponent.Builder current = null;
    private List<TextComponent.Builder> chain;

    /**
     * Create the instance
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    public SplittedStringConverter(SplittedStringList list, GChatPlayer.PlaceholderResolver resolver) {
        this.list = list;
        this.resolver = resolver;
    }

    /**
     * Actually convert the list to a TextComponent
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    public TextComponent toTextComponent() {

        if (this.root != null) {
            return this.root.build();
        }

        this.root = Component.text();
        this.current = this.root;
        this.chain = new ArrayList<>();

        for (StringSplitter.Entry entry : this.list) {
            this.handleEntry(entry);
        }

        // Close everything
        this.doReset();

        return this.root.build();
    }

    /**
     * Handle a single entry
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    private void handleEntry(StringSplitter.Entry entry) {

        if (entry.isReset()) {
            this.doReset();
            return;
        }

        if (entry.isOpeningTag()) {
            this.handleOpeningTag(entry);
        } else if (entry.isClosingTag()) {
            this.handleClosingTag(entry);
        } else {
            this.handleText(entry);
        }
    }

    /**
     * Handle an opening tag
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    private void handleOpeningTag(StringSplitter.Entry entry) {

        TextComponent.Builder new_builder = Component.text();

        this.current = new_builder;
        this.chain.add(new_builder);

        entry.applyStyle(new_builder);
    }

    /**
     * Handle a closing tag
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    private void handleClosingTag(StringSplitter.Entry entry) {

        TextComponent.Builder ending = this.current;

        if (ending == this.root) {
            return;
        }

        // Remove the current builder from the chain
        this.chain.remove(this.chain.size() - 1);

        if (this.chain.size() == 0) {
            this.current = this.root;
        } else {
            // Get the previous builder
            this.current = this.chain.get(this.chain.size() - 1);
        }

        // Append the ending builder to the current one
        this.current.append(ending.build());
    }

    /**
     * Handle text
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    private void handleText(StringSplitter.Entry entry) {

        TextComponent entry_component;

        if (entry.isPlaceholder()) {
            entry_component = this.resolver.replace(entry);
        } else {
            String piece = entry.getContent();
            entry_component = Component.text(piece);
        }

        if (entry_component == null) {
            return;
        }

        this.current.append(entry_component);
    }

    /**
     * Reset the styles (close everything, basically)
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    private void doReset() {
        while (this.chain.size() > 0) {
            this.handleClosingTag(null);
        }
    }
}
