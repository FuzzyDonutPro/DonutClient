package com.donut.client.macros;

import java.util.*;

/**
 * Settings system for macros
 * Supports different setting types: boolean, integer, float, string, enum
 */
public class MacroSettings {

    private final Map<String, Setting<?>> settings = new LinkedHashMap<>();
    private final String macroName;

    public MacroSettings(String macroName) {
        this.macroName = macroName;
    }

    // Add different types of settings

    public void addBooleanSetting(String name, String description, boolean defaultValue) {
        settings.put(name, new BooleanSetting(name, description, defaultValue));
    }

    public void addIntSetting(String name, String description, int defaultValue, int min, int max) {
        settings.put(name, new IntSetting(name, description, defaultValue, min, max));
    }

    public void addFloatSetting(String name, String description, float defaultValue, float min, float max) {
        settings.put(name, new FloatSetting(name, description, defaultValue, min, max));
    }

    public void addStringSetting(String name, String description, String defaultValue) {
        settings.put(name, new StringSetting(name, description, defaultValue));
    }

    public <E extends Enum<E>> void addEnumSetting(String name, String description, E defaultValue) {
        settings.put(name, new EnumSetting<>(name, description, defaultValue));
    }

    // Get setting values

    public boolean getBoolean(String name) {
        Setting<?> setting = settings.get(name);
        if (setting instanceof BooleanSetting) {
            return ((BooleanSetting) setting).getValue();
        }
        return false;
    }

    public int getInt(String name) {
        Setting<?> setting = settings.get(name);
        if (setting instanceof IntSetting) {
            return ((IntSetting) setting).getValue();
        }
        return 0;
    }

    public float getFloat(String name) {
        Setting<?> setting = settings.get(name);
        if (setting instanceof FloatSetting) {
            return ((FloatSetting) setting).getValue();
        }
        return 0.0f;
    }

    public String getString(String name) {
        Setting<?> setting = settings.get(name);
        if (setting instanceof StringSetting) {
            return ((StringSetting) setting).getValue();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> E getEnum(String name) {
        Setting<?> setting = settings.get(name);
        if (setting instanceof EnumSetting) {
            return (E) ((EnumSetting<?>) setting).getValue();
        }
        return null;
    }

    // Set values

    public void setBoolean(String name, boolean value) {
        Setting<?> setting = settings.get(name);
        if (setting instanceof BooleanSetting) {
            ((BooleanSetting) setting).setValue(value);
        }
    }

    public void setInt(String name, int value) {
        Setting<?> setting = settings.get(name);
        if (setting instanceof IntSetting) {
            ((IntSetting) setting).setValue(value);
        }
    }

    public void setFloat(String name, float value) {
        Setting<?> setting = settings.get(name);
        if (setting instanceof FloatSetting) {
            ((FloatSetting) setting).setValue(value);
        }
    }

    public void setString(String name, String value) {
        Setting<?> setting = settings.get(name);
        if (setting instanceof StringSetting) {
            ((StringSetting) setting).setValue(value);
        }
    }

    public <E extends Enum<E>> void setEnum(String name, E value) {
        Setting<?> setting = settings.get(name);
        if (setting instanceof EnumSetting) {
            @SuppressWarnings("unchecked")
            EnumSetting<E> enumSetting = (EnumSetting<E>) setting;
            enumSetting.setValue(value);
        }
    }

    public Collection<Setting<?>> getAllSettings() {
        return settings.values();
    }

    public boolean hasSetting(String name) {
        return settings.containsKey(name);
    }

    // Base setting class
    public abstract static class Setting<T> {
        protected final String name;
        protected final String description;
        protected T value;

        public Setting(String name, String description, T defaultValue) {
            this.name = name;
            this.description = description;
            this.value = defaultValue;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public abstract String getValueString();
        public abstract SettingType getType();
    }

    // Setting types
    public static class BooleanSetting extends Setting<Boolean> {
        public BooleanSetting(String name, String description, Boolean defaultValue) {
            super(name, description, defaultValue);
        }

        public void toggle() {
            value = !value;
        }

        @Override
        public String getValueString() {
            return value ? "ON" : "OFF";
        }

        @Override
        public SettingType getType() {
            return SettingType.BOOLEAN;
        }
    }

    public static class IntSetting extends Setting<Integer> {
        private final int min;
        private final int max;

        public IntSetting(String name, String description, Integer defaultValue, int min, int max) {
            super(name, description, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override
        public void setValue(Integer value) {
            this.value = Math.max(min, Math.min(max, value));
        }

        public void increment(int amount) {
            setValue(value + amount);
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        @Override
        public String getValueString() {
            return String.valueOf(value);
        }

        @Override
        public SettingType getType() {
            return SettingType.INT;
        }
    }

    public static class FloatSetting extends Setting<Float> {
        private final float min;
        private final float max;

        public FloatSetting(String name, String description, Float defaultValue, float min, float max) {
            super(name, description, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override
        public void setValue(Float value) {
            this.value = Math.max(min, Math.min(max, value));
        }

        public void increment(float amount) {
            setValue(value + amount);
        }

        public float getMin() {
            return min;
        }

        public float getMax() {
            return max;
        }

        @Override
        public String getValueString() {
            return String.format("%.2f", value);
        }

        @Override
        public SettingType getType() {
            return SettingType.FLOAT;
        }
    }

    public static class StringSetting extends Setting<String> {
        public StringSetting(String name, String description, String defaultValue) {
            super(name, description, defaultValue);
        }

        @Override
        public String getValueString() {
            return value;
        }

        @Override
        public SettingType getType() {
            return SettingType.STRING;
        }
    }

    public static class EnumSetting<E extends Enum<E>> extends Setting<E> {
        private final E[] values;

        @SuppressWarnings("unchecked")
        public EnumSetting(String name, String description, E defaultValue) {
            super(name, description, defaultValue);
            this.values = (E[]) defaultValue.getDeclaringClass().getEnumConstants();
        }

        public void cycle() {
            int index = Arrays.asList(values).indexOf(value);
            value = values[(index + 1) % values.length];
        }

        public E[] getValues() {
            return values;
        }

        @Override
        public String getValueString() {
            return value.name();
        }

        @Override
        public SettingType getType() {
            return SettingType.ENUM;
        }
    }

    public enum SettingType {
        BOOLEAN, INT, FLOAT, STRING, ENUM
    }
}