/*
 * Copyright 2015 yihtserns.
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
package com.github.yihtserns.jaxbean.unmarshaller;

import com.github.yihtserns.jaxbean.unmarshaller.api.BeanHandler;
import com.github.yihtserns.jaxbean.unmarshaller.Unmarshaller.InitializableElementUnmarshaller;
import com.github.yihtserns.jaxbean.unmarshaller.Unmarshaller.ElementUnmarshallerProvider;
import com.github.yihtserns.jaxbean.unmarshaller.Unmarshaller.ElementUnmarshallerProvider.Handler;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author yihtserns
 */
class BeanUnmarshaller implements InitializableElementUnmarshaller {

    public static final String AUTO_GENERATED_NAME = "##default";
    private Set<String> listTypeElementNames = new HashSet<String>();
    private Map<String, String> elementName2PropertyName = new HashMap<String, String>();
    private Map<String, String> attributeName2PropertyName = new HashMap<String, String>();
    private Map<String, Unmarshaller<Attr>> attributeName2Unmarshaller = new HashMap<String, Unmarshaller<Attr>>();
    private Map<String, Unmarshaller<Element>> localName2Unmarshaller = new HashMap<String, Unmarshaller<Element>>();
    private String textContentPropertyName = null;
    final Class<?> beanClass;

    protected BeanUnmarshaller(Class<?> beanClass) throws Exception {
        this.beanClass = beanClass;
    }

    @Override
    public void init(ElementUnmarshallerProvider unmarshallerProvider) throws Exception {
        Class<?> currentClass = beanClass;
        while (currentClass != Object.class) {
            XmlAccessorType xmlAccessorType = currentClass.getAnnotation(XmlAccessorType.class);
            PropertyResolver resolver = getResolverFor(xmlAccessorType);
            for (AccessibleObject accObj : resolver.getDirectMembers(currentClass)) {
                if (accObj.isAnnotationPresent(XmlAttribute.class)) {
                    addAttribute(accObj, resolver);
                } else if (accObj.isAnnotationPresent(XmlElement.class)) {
                    XmlElement[] xmlElements = {accObj.getAnnotation(XmlElement.class)};
                    addElements(xmlElements, accObj, resolver, unmarshallerProvider);
                } else if (accObj.isAnnotationPresent(XmlElements.class)) {
                    XmlElements xmlElements = accObj.getAnnotation(XmlElements.class);
                    addElements(xmlElements.value(), accObj, resolver, unmarshallerProvider);
                } else if (accObj.isAnnotationPresent(XmlElementRef.class)) {
                    addElementRef(accObj, resolver, unmarshallerProvider);
                } else if (accObj.isAnnotationPresent(XmlValue.class)) {
                    setTextContent(accObj, resolver);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    public <T extends AccessibleObject> void addAttribute(T accObj, PropertyResolver<T> resolver) throws Exception {
        XmlAttribute xmlAttribute = accObj.getAnnotation(XmlAttribute.class);

        String propertyName = resolver.getPropertyName(accObj);
        String attributeName = returnNameOrDefault(xmlAttribute.name(), propertyName);

        Unmarshaller<Attr> unmarshaller = AttributeValueUnmarshaller.INSTANCE;
        if (accObj.isAnnotationPresent(XmlJavaTypeAdapter.class)) {
            XmlAdapter adapter = accObj.getAnnotation(XmlJavaTypeAdapter.class).value().newInstance();
            unmarshaller = new XmlAdapterUnmarshaller(adapter, unmarshaller);
        }

        attributeName2PropertyName.put(attributeName, propertyName);
        attributeName2Unmarshaller.put(attributeName, unmarshaller);
    }

    public <T extends AccessibleObject> void addElements(
            XmlElement[] xmlElements,
            T accObj,
            PropertyResolver<T> resolver,
            ElementUnmarshallerProvider unmarshallerProvider) throws Exception {
        final String propertyName = resolver.getPropertyName(accObj);
        XmlElementWrapper elementWrapper = accObj.getAnnotation(XmlElementWrapper.class);

        if (elementWrapper != null) {
            String wrapperElementName = returnNameOrDefault(elementWrapper.name(), propertyName);

            ElementWrapperUnmarshaller wrapperUnmarshaller = newWrapperUnmarshaller();
            for (XmlElement xmlElement : xmlElements) {
                Unmarshaller<Element> childUnmarshaller = resolveUnmarshaller(resolver, accObj, xmlElement, unmarshallerProvider);

                String elementName = returnNameOrDefault(xmlElement.name(), propertyName);
                wrapperUnmarshaller.put(elementName, childUnmarshaller);
            }

            elementName2PropertyName.put(wrapperElementName, propertyName);
            localName2Unmarshaller.put(wrapperElementName, wrapperUnmarshaller);
        } else {
            for (XmlElement xmlElement : xmlElements) {
                String elementName = returnNameOrDefault(xmlElement.name(), propertyName);
                Unmarshaller<Element> childUnmarshaller = resolveUnmarshaller(resolver, accObj, xmlElement, unmarshallerProvider);

                if (resolver.isListType(accObj)) {
                    listTypeElementNames.add(elementName);
                }
                elementName2PropertyName.put(elementName, propertyName);
                localName2Unmarshaller.put(elementName, childUnmarshaller);
            }
        }
    }

    protected ElementWrapperUnmarshaller newWrapperUnmarshaller() {
        return new ElementWrapperUnmarshaller();
    }

    private String returnNameOrDefault(String name, String autogeneratedName) {
        return !name.equals(AUTO_GENERATED_NAME) ? name : autogeneratedName;
    }

    private <T extends AccessibleObject> Unmarshaller<Element> resolveUnmarshaller(
            PropertyResolver<T> resolver,
            T accObj,
            XmlElement xmlElement,
            ElementUnmarshallerProvider unmarshallerProvider) throws Exception {

        if (accObj.isAnnotationPresent(XmlJavaTypeAdapter.class)) {
            Class<? extends XmlAdapter> adapterClass = accObj.getAnnotation(XmlJavaTypeAdapter.class).value();

            Class<?> valueType = (Class) ((ParameterizedType) adapterClass.getGenericSuperclass()).getActualTypeArguments()[0];
            Unmarshaller<Element> unmarshaller = unmarshallerProvider.getUnmarshallerForType(valueType);

            XmlAdapter adapter = adapterClass.newInstance();
            return new XmlAdapterUnmarshaller(adapter, unmarshaller);
        }

        Class<?> type = xmlElement.type();
        if (type == XmlElement.DEFAULT.class) {
            type = resolver.getComponentType(accObj);
        }

        return unmarshallerProvider.getUnmarshallerForType(type);
    }

    public <T extends AccessibleObject> void addElementRef(
            final T accObj,
            final PropertyResolver<T> resolver,
            ElementUnmarshallerProvider unmarshallerProvider) {
        Class<?> propertyType = resolver.getComponentType(accObj);
        final String propertyName = resolver.getPropertyName(accObj);

        unmarshallerProvider.forGlobalUnmarshallerCompatibleWith(propertyType, new Handler() {
            public void handle(String globalName, Unmarshaller<Element> unmarshaller) {
                elementName2PropertyName.put(globalName, propertyName);
                localName2Unmarshaller.put(globalName, unmarshaller);
                if (resolver.isListType(accObj)) {
                    listTypeElementNames.add(globalName);
                }
            }
        });
    }

    private <T extends AccessibleObject> void setTextContent(T accObj, PropertyResolver<T> resolver) {
        this.textContentPropertyName = resolver.getPropertyName(accObj);
    }

    private PropertyResolver getResolverFor(XmlAccessorType xmlAccessorType) throws UnsupportedOperationException {
        switch (xmlAccessorType.value()) {
            case FIELD:
                return PropertyResolver.FIELD;
            case PROPERTY:
                return PropertyResolver.METHOD;
            default:
                throw new UnsupportedOperationException("XML Access Type not supported yet: " + xmlAccessorType.value());
        }
    }

    @Override
    public Object unmarshal(Element element, BeanHandler beanHandler) throws Exception {
        Object bean = beanHandler.createBean(beanClass);
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attr = (Attr) attributes.item(i);
            if (isNamespaceDeclaration(attr)) {
                continue;
            }
            String attributeName = attr.getName();
            Unmarshaller<Attr> unmarshaller = attributeName2Unmarshaller.get(attributeName);

            String propertyName = attributeName2PropertyName.get(attributeName);
            Object propertyValue = unmarshaller.unmarshal(attr, beanHandler);

            beanHandler.setBeanProperty(bean, propertyName, propertyValue);
        }

        PropertyValueMap propertyName2PropertyValue = new PropertyValueMap();
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element childElement = (Element) item;
            String localName = item.getLocalName();
            Unmarshaller<Element> childUnmarshaller = localName2Unmarshaller.get(localName);
            Object childInstance = childUnmarshaller.unmarshal(childElement, beanHandler);
            String propertyName = elementName2PropertyName.get(localName);
            if (listTypeElementNames.contains(localName)) {
                propertyName2PropertyValue.add(propertyName, childInstance);
            } else {
                propertyName2PropertyValue.put(propertyName, childInstance);
            }
        }
        propertyName2PropertyValue.setTo(bean, beanHandler);

        if (textContentPropertyName != null) {
            beanHandler.setBeanProperty(bean, textContentPropertyName, element.getTextContent());
        }
        return beanHandler.postProcess(bean);
    }

    private boolean isNamespaceDeclaration(Attr attr) {
        String fullName = attr.getName();
        return fullName.equals("xmlns") || fullName.startsWith("xmlns:");
    }

    private static final class PropertyValueMap extends LinkedHashMap<String, Object> {

        public void add(String propertyName, Object value) {
            List<Object> valueList = (List) get(propertyName);

            if (valueList == null) {
                valueList = new ArrayList<Object>();
                put(propertyName, valueList);
            }

            valueList.add(value);
        }

        public void setTo(Object bean, BeanHandler beanHandler) {
            for (Entry<String, Object> entry : entrySet()) {
                String propertyName = entry.getKey();
                Object propertyValue = entry.getValue();

                if (propertyValue instanceof List) {
                    propertyValue = beanHandler.postProcessList((List) propertyValue);
                }
                beanHandler.setBeanProperty(bean, propertyName, propertyValue);
            }
        }
    }
}
