package rocks.blackblock.fluxchat.placeholder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A class that holds the parameters for a placeholder.
 *
 * @author   Jelle De Loecker
 * @since    3.2.0
 */
public class PlaceholderParameters {

    private Map<String, String> simple_parameters = null;
    private Map<String, TextComponent> complex_parameters = null;

    /**
     * Set a simple string value
     *
     * @since    3.2.0
     */
    public void set(String key, String value) {
        if (this.simple_parameters == null) {
            this.simple_parameters = new HashMap<>();
        }

        this.simple_parameters.put(key, value);
    }

    /**
     * Set a value as a TextComponent
     *
     * @since    3.2.0
     */
    public void set(String key, TextComponent value) {
        if (this.complex_parameters == null) {
            this.complex_parameters = new HashMap<>();
        }

        this.complex_parameters.put(key, value);
    }

    /**
     * Specifically look in the text components
     *
     * @since    3.2.0
     */
    @Nullable
    public TextComponent getFromTextComponent(String key) {

        if (this.complex_parameters == null) {
            return null;
        }

        return this.complex_parameters.get(key);
    }

    /**
     * Specifically look in the string values
     *
     * @since    3.2.0
     */
    @Nullable
    public String getFromStringValues(String key) {

        if (this.simple_parameters == null) {
            return null;
        }

        return this.simple_parameters.get(key);
    }

    /**
     * Get a TextComponent value
     *
     * @since    3.2.0
     */
    public TextComponent get(String key) {

        TextComponent result = null;

        if (this.complex_parameters != null) {
            result = this.complex_parameters.get(key);

            if (result != null) {
                return result;
            }
        }

        if (this.simple_parameters != null) {
            String value = this.simple_parameters.get(key);

            if (value == null) {
                return null;
            }

            result = Component.text(value);

            // Remember for next time
            this.set(key, result);
        }

        return result;
    }
}
