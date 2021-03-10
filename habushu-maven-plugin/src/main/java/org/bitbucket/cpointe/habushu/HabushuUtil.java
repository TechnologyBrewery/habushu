package org.bitbucket.cpointe.habushu;

import java.lang.reflect.Field;

/**
 * A util class for the habushu-maven-plugin, containing some shared logic
 * between a few other classes.
 *
 */
public final class HabushuUtil {
	
	private HabushuUtil() {}

	/**
	 * Changes a private field in the CleanMojo using reflection.
	 * 
	 * @param clazz    the class that has the field
	 * @param field    the field to change
	 * @param newValue the new value for the field
	 * @throws Exception
	 */
	public static void changePrivateCleanMojoField(Class<?> clazz, String fieldName, Object newValue) {
		try {
			Object instance = clazz.newInstance();
			Field fieldToModify = clazz.getDeclaredField(fieldName);
			fieldToModify.setAccessible(true);

			fieldToModify.set(instance, newValue);
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException
				| InstantiationException e) {
			throw new HabushuException("Could not set value for field " + fieldName, e);
		}
	}
}
