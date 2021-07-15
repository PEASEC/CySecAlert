package de.tudarmstadt.peasec.util;

import java.util.Properties;

public class PropertyHandler {

    public static Properties mergeProperties(Properties... properties) {
        Properties mergedProperties = new Properties();
        for (Properties property : properties) {
            mergedProperties.putAll(property);
        }
        return mergedProperties;
    }
}
