/*
 ** 2014 September 05
 **
 ** The author disclaims copyright to this source code. In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.minecraft.minema.util.config;

import net.minecraftforge.common.config.Property;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class ConfigEnum<T extends Enum<T>> extends ConfigValue<T> {

	private final Class<T> type;
	private final String[] validValues;

	@SuppressWarnings("unchecked")
	public ConfigEnum(T value) {
		super(value);

		type = (Class<T>) value.getClass();

		T[] values = type.getEnumConstants();
		validValues = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			validValues[i] = enumToString(values[i]);
		}
	}

	private String enumToString(T e) {
		return e.name().toLowerCase();
	}

	private T stringToEnum(String name) {
		return (T) Enum.valueOf(type, name.toUpperCase());
	}

	@Override
	protected Property.Type getPropType() {
		return Property.Type.STRING;
	}

	@Override
	protected Property getProp() {
		Property prop = super.getProp();
		prop.setValidValues(validValues);
		return prop;
	}

	@Override
	protected String getPropDefault() {
		return enumToString(getDefault());
	}

	@Override
	public T get() {
		try {
			return stringToEnum(getProp().getString());
		} catch (IllegalArgumentException ex) {
			reset();
			return stringToEnum(getProp().getString());
		}
	}

	@Override
	public void set(T value) {
		getProp().set(enumToString(get()));
	}
}
