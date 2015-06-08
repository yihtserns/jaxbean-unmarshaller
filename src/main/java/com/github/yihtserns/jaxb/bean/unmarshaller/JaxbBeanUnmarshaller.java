/*
 * Copyright 2015 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.yihtserns.jaxb.bean.unmarshaller;

import com.github.yihtserns.jaxb.bean.unmarshaller.Unmarshaller.InitializableUnmarshaller;
import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.bind.annotation.XmlRootElement;
import org.w3c.dom.Element;

/**
 *
 * @author yihtserns
 */
public class JaxbBeanUnmarshaller implements Unmarshaller.Provider {

    private Map<String, Unmarshaller> globalName2Unmarshaller = new HashMap<String, Unmarshaller>();
    private Map<Class<?>, String> globalType2Name = new HashMap<Class<?>, String>();
    private Map<Class<?>, InitializableUnmarshaller> type2Unmarshaller
            = new HashMap<Class<?>, InitializableUnmarshaller>();
    private Map<Class<?>, InitializableUnmarshaller> type2InitializedUnmarshaller
            = new HashMap<Class<?>, InitializableUnmarshaller>();

    /**
     * @see #newInstance(java.lang.Class...)
     */
    private JaxbBeanUnmarshaller() {
    }

    public Object unmarshal(Element element) throws Exception {
        String globalName = element.getLocalName();
        Unmarshaller unmarshaller = globalName2Unmarshaller.get(globalName);

        return unmarshaller.unmarshal(element);
    }

    private void addGlobalType(Class<?> type) throws Exception {
        String elementName = resolveRootElementName(type);
        Unmarshaller unmarshaller = getUnmarshallerForType(type);

        globalName2Unmarshaller.put(elementName, unmarshaller);
        globalType2Name.put(type, elementName);
    }

    @Override
    public Unmarshaller getUnmarshallerForType(Class<?> type) throws Exception {
        if (type2InitializedUnmarshaller.containsKey(type)) {
            return type2InitializedUnmarshaller.get(type);
        }
        if (type2Unmarshaller.containsKey(type)) {
            return type2Unmarshaller.get(type);
        }
        if (type == String.class) {
            return StringUnmarshaller.INSTANCE;
        }

        InitializableUnmarshaller unmarshaller = new BeanUnmarshaller(type.getDeclaredConstructor());
        type2Unmarshaller.put(type, unmarshaller);

        return unmarshaller;
    }

    @Override
    public void forGlobalUnmarshallerCompatibleWith(Class<?> type, Handler handler) {
        for (Entry<Class<?>, String> entry : globalType2Name.entrySet()) {
            Class<?> globalType = entry.getKey();
            String globalName = entry.getValue();

            if (!type.isAssignableFrom(globalType)) {
                continue;
            }
            Unmarshaller unmarshaller = globalName2Unmarshaller.get(globalName);
            handler.handle(globalName, unmarshaller);
        }
    }

    private void init() throws Exception {
        while (!type2Unmarshaller.isEmpty()) {
            Collection<InitializableUnmarshaller> toBeInitialized = new ArrayList(type2Unmarshaller.values());
            type2InitializedUnmarshaller.putAll(type2Unmarshaller);
            type2Unmarshaller.clear();

            for (InitializableUnmarshaller unmarshaller : toBeInitialized) {
                unmarshaller.init(this);
            }
        }
    }

    public static JaxbBeanUnmarshaller newInstance(Class<?>... types) throws Exception {
        JaxbBeanUnmarshaller jaxbBeanUnmarshaller = new JaxbBeanUnmarshaller();
        for (Class<?> type : types) {
            jaxbBeanUnmarshaller.addGlobalType(type);
        }
        jaxbBeanUnmarshaller.init();

        return jaxbBeanUnmarshaller;
    }

    private static String resolveRootElementName(Class<?> type) {
        XmlRootElement xmlRootElement = type.getAnnotation(XmlRootElement.class);
        String name = xmlRootElement.name();
        if (name.equals(BeanUnmarshaller.AUTO_GENERATED_NAME)) {
            name = Introspector.decapitalize(type.getSimpleName());
        }
        return name;
    }
}
