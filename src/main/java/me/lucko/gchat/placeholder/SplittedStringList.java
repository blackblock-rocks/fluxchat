package me.lucko.gchat.placeholder;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A list specifically for holding SplittedString entries.
 *
 * @author   Jelle De Loecker
 * @since    3.2.0
 */
public class SplittedStringList implements List<StringSplitter.Entry> {

    private final List<StringSplitter.Entry> entries = new ArrayList<>();
    private final List<StringSplitter.Entry> open_tags_chain = new ArrayList<>();

    /**
     * Add an entry, but fix the chain
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    @Override
    public boolean add(StringSplitter.Entry entry) {

        if (entry.isReset()) {
            this.open_tags_chain.clear();
            this.entries.add(entry);
            return true;
        }

        if (entry.isOpeningTag()) {
            this.open_tags_chain.add(entry);
        } else if (entry.isClosingTag()) {
            this.fixClosingTags(entry);
        }

        return this.entries.add(entry);
    }

    /**
     * Make sure all open tags are closed,
     * so we don't have any overlapping tags.
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    private void fixClosingTags(StringSplitter.Entry entry) {

        if (this.open_tags_chain.isEmpty()) {
            return;
        }

        String closing_tag_name = entry.getTagName();

        if (closing_tag_name == null) {
            return;
        }

        // First see if the tag we're closing is actually in the chain
        // If it's not, we're just going to ignore it.
        Integer last_index = null;

        if (entry.isReset()) {
            last_index = -1;
        } else {

            for (int i = this.open_tags_chain.size() - 1; i >= 0; i--) {
                StringSplitter.Entry current = this.open_tags_chain.get(i);

                if (current.getTagName().equals(closing_tag_name)) {
                    last_index = i;
                    break;
                }
            }
        }

        if (last_index == null) {
            return;
        }

        // Now remove all the open tags that are after the last index
        for (int i = this.open_tags_chain.size() - 1; i >= last_index; i--) {

            if (i < 0) {
                break;
            }

            // Remove the opening tag from the chain
            StringSplitter.Entry current = this.open_tags_chain.remove(i);

            if (i == last_index) {
                break;
            }

            // But also: add a closing tag to the list
            this.entries.add(new StringSplitter.Entry(current.getTagName(), StringSplitter.EntryType.CLOSING_TAG));
        }

    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return entries.contains(o);
    }

    @NotNull
    @Override
    public Iterator<StringSplitter.Entry> iterator() {
        return entries.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return entries.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] ts) {
        return entries.toArray(ts);
    }

    @Override
    public boolean remove(Object o) {
        return entries.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> collection) {
        return entries.containsAll(collection);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends StringSplitter.Entry> collection) {
        return false;
    }

    @Override
    public boolean addAll(int i, @NotNull Collection<? extends StringSplitter.Entry> collection) {
        return false;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> collection) {
        return false;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> collection) {
        return false;
    }

    @Override
    public void clear() {
        this.entries.clear();
    }

    @Override
    public StringSplitter.Entry get(int i) {
        return this.entries.get(i);
    }

    @Override
    public StringSplitter.Entry set(int i, StringSplitter.Entry entry) {
        return null;
    }

    @Override
    public void add(int i, StringSplitter.Entry entry) {
        return;
    }

    @Override
    public StringSplitter.Entry remove(int i) {
        return null;
    }

    @Override
    public int indexOf(Object o) {
        return this.entries.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return this.entries.lastIndexOf(o);
    }

    @NotNull
    @Override
    public ListIterator<StringSplitter.Entry> listIterator() {
        return this.entries.listIterator();
    }

    @NotNull
    @Override
    public ListIterator<StringSplitter.Entry> listIterator(int i) {
        return this.entries.listIterator(i);
    }

    @NotNull
    @Override
    public List<StringSplitter.Entry> subList(int i, int i1) {
        return this.entries.subList(i, i1);
    }
}
