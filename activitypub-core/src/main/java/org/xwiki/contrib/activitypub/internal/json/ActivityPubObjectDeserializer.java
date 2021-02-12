/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.activitypub.internal.json;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.UnknownTypeObject;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.xwiki.contrib.activitypub.internal.json.AbstractActivityPubJsonParser.LOGGER_KEY;

/**
 * A custom Jackson deserializer for {@link ActivityPubObject}. The main role of this deserializer is to check the type
 * property of a JSON to create the right objects. Some utility methods are provided to allow finding the POJO in
 * various packages.
 *
 * @version $Id$
 */
public class ActivityPubObjectDeserializer extends JsonDeserializer<ActivityPubObject>
{
    /**
     * Packages where to look for POJOs.
     */
    private static final List<String> PACKAGES_ENTITIES = Arrays.asList(
        "org.xwiki.contrib.activitypub.entities"
    );

    /**
     * {@inheritDoc}
     * <p>
     * Deserialize the current object based on the given "type" property. If the type attribute does not exist, or  if
     * no class is found with the same type name, then the object is deserialized using {@link ActivityPubObject} as
     * fallback.
     */
    @Override
    public ActivityPubObject deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException
    {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        ObjectNode root = mapper.readTree(jsonParser);

        Logger logger = (Logger) deserializationContext.findInjectableValue(LOGGER_KEY, null, null);

        JsonNode type = root.get("type");
        Class<? extends ActivityPubObject> instanceClass;
        if (type != null) {
            instanceClass = findClass(type.asText(), logger).orElse(UnknownTypeObject.class);
        } else {
            instanceClass = UnknownTypeObject.class;
        }
        return mapper.treeToValue(root, instanceClass);
    }

    private Optional<Class<? extends ActivityPubObject>> findClass(String type, Logger logger)
    {
        Class<? extends ActivityPubObject> classInPackage;
        for (String packageExtension : PACKAGES_ENTITIES) {
            classInPackage = findClassInPackage(packageExtension, type);
            if (classInPackage != null) {
                return Optional.of(classInPackage);
            }
        }

        // The level is set to warn to ease the access to this information but this log level should be decreased to 
        // info or debug once the application gain in stability.
        logger.warn("ActivityPub Object type [{}] not found.", type);

        return Optional.empty();
    }

    private Class<? extends ActivityPubObject> findClassInPackage(String packageName, String type)
    {
        String fqn = String.format("%s.%s", packageName, type);
        try {
            return (Class<? extends ActivityPubObject>) getClass().getClassLoader().loadClass(fqn);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
