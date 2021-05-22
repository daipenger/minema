/*
 ** 2014 August 10
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.minecraft.minema.util.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.lang3.text.WordUtils;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public abstract class ConfigValue<T> {
	
	private static final Map<ConfigCategory, Set<String>> linked = new HashMap<>();
	
	public static void clearUnlinkedProps() {
		for (Map.Entry<ConfigCategory, Set<String>> entry : linked.entrySet()) {
			ConfigCategory category = entry.getKey();
			Iterator<String> it = category.keySet().iterator();
			while (it.hasNext())
				if (!entry.getValue().contains(it.next()))
					it.remove();
		}
	}

	private final T valueDefault;
	private Supplier<Property> propSupplier;

	public ConfigValue(T value) {
		this.valueDefault = value;
	}

	public void link(Configuration config, ConfigCategory category, String propName, String langKeyPrefix) {
		String catName = category.getName();

		// set property language key and description
		String propLangKey = langKeyPrefix + "." + propName;
		String propDesc = WordUtils.wrap(I18n.format(propLangKey + ".tooltip"), 128);

		// create supplier so that later calls don't need all the variables
		// above
		propSupplier = () -> {
			Property prop = config.get(catName, propName, getPropDefault(), propDesc, getPropType());
			prop.setLanguageKey(propLangKey);
			return prop;
		};

		// initialize prop
		getProp();

		// make sure the properties have an insertion order
		List<String> immutableOrder = category.getPropertyOrder();
		ArrayList<String> newOrder = new ArrayList<>(immutableOrder.size() + 1);
		newOrder.addAll(immutableOrder);
		newOrder.add(category.getName() + "." + propName);
		category.setPropertyOrder(newOrder);
		
		if (linked.get(category) == null)
			linked.put(category, new HashSet<>());
		linked.get(category).add(propName);
	}

	protected abstract Property.Type getPropType();

	protected Property getProp() {
		if (propSupplier == null) {
			throw new IllegalStateException("ConfigValue hasn't been linked yet!");
		}
		return propSupplier.get();
	}

	protected String getPropDefault() {
		return String.valueOf(getDefault());
	}

	public abstract T get();

	public abstract void set(T value);

	public T getDefault() {
		return valueDefault;
	}

	public void reset() {
		set(getDefault());
	}
}
