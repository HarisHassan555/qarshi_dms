package com.bezkoder.spring.login.admin.utility.common;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


/**
 * @author Muhammad Khalil
 *
 */
public class ResourceBundleUtility {

	private static final Logger				log										= LogManager
			.getLogger(ResourceBundleUtility.class);

	private static ResourceBundleUtility	resourseBundleUtility					= null;

	public static final String				LANG_TRANSLATIONS_EN_US_RESOURCE_BUNDLE			= "internationalization.lang_translations_en_US";
	

	private ResourceBundle					langTranslationENUS							= null;
	

	private ResourceBundleUtility() {
		try {
			langTranslationENUS = ResourceBundle.getBundle(LANG_TRANSLATIONS_EN_US_RESOURCE_BUNDLE);
			
		} catch (Exception exception) {
			exception.printStackTrace();
			log.error(exception.getMessage());
		}

	}

	public static ResourceBundleUtility getInstance() {
		if (resourseBundleUtility == null) {
			resourseBundleUtility = new ResourceBundleUtility();
			return resourseBundleUtility;
		}
		else {
			return resourseBundleUtility;
		}
	}

	private ResourceBundle getBundle(String bundleName) {

		switch (bundleName) {
			case LANG_TRANSLATIONS_EN_US_RESOURCE_BUNDLE:
				return langTranslationENUS;

			default:
				return null;
		}
	}

	public String getString(final String bundleName, final String key, final Object... params) {
		String returnString = null;
		if ((bundleName != null && (!bundleName.equals(""))) && (key != null && (!key.equals("")))) {
			ResourceBundle resourceBundle = getBundle(bundleName);
			if (resourceBundle != null) {
				try {
					returnString = resourceBundle.getString(key);
					returnString = MessageFormat.format(returnString, params);
				} catch (MissingResourceException e) {
					log.error("");
					log.trace("");
					return null;
				}
			}
		}
		return returnString;
	}
}
