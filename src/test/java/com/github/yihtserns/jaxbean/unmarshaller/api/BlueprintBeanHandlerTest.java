/*
 * Copyright 2016 yihtserns.
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
package com.github.yihtserns.jaxbean.unmarshaller.api;

import com.github.yihtserns.jaxbean.unmarshaller.AbstractSpecTest;
import com.github.yihtserns.jaxbean.unmarshaller.JaxbeanUnmarshaller;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Set;
import org.apache.aries.blueprint.container.BlueprintContainerImpl;
import org.apache.aries.blueprint.container.SimpleNamespaceHandlerSet;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.parser.NamespaceHandlerSet;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author yihtserns
 */
public class BlueprintBeanHandlerTest extends AbstractSpecTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Override
    protected <T> T unmarshal(String xml, Class<T> rootType, Class<?>... otherTypes) throws Exception {
        return doUnmarshal(
                "<blueprint xmlns=\"http://www.osgi.org/xmlns/blueprint/v1.0.0\">" + xml + "</blueprint>",
                rootType,
                otherTypes);
    }

    private <T> T doUnmarshal(String blueprintXml, Class<T> rootType, Class<?>... otherTypes) throws Exception {
        File file = tempFolder.newFile();
        FileUtils.write(file, blueprintXml);

        final String id = "bean";
        final JaxbeanUnmarshaller unmarshaller = JaxbeanUnmarshaller.newInstance(merge(rootType, otherTypes));
        final URI jaxbNamespaceUri = new URI("http://example.com/jaxb");
        BlueprintContainerImpl blueprintContainer
                = new BlueprintContainerImpl(getClass().getClassLoader(), Arrays.asList(file.toURI().toURL()), false) {

                    @Override
                    protected NamespaceHandlerSet createNamespaceHandlerSet() {
                        SimpleNamespaceHandlerSet nsHandlerSet = (SimpleNamespaceHandlerSet) super.createNamespaceHandlerSet();
                        if (!nsHandlerSet.getNamespaces().contains(jaxbNamespaceUri)) {
                            nsHandlerSet.addNamespace(jaxbNamespaceUri, null, new UnmarshallerNamespaceHandler(unmarshaller, id));
                        }

                        return nsHandlerSet;
                    }

                };
        blueprintContainer.init(false);

        try {
            return rootType.cast(blueprintContainer.getComponentInstance(id));
        } finally {
            blueprintContainer.destroy();
        }
    }

    @Test
    public void canResolvePropertyPlaceholders() throws Exception {
        String xml = "<blueprint xmlns=\"http://www.osgi.org/xmlns/blueprint/v1.0.0\""
                + " xmlns:ext=\"http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.2.0\">"
                + "  <ext:property-placeholder>\n"
                + "    <ext:default-properties>\n"
                + "      <ext:property name=\"obj.count\" value=\"3\"/>\n"
                + "      <ext:property name=\"obj.displayName\" value=\"JAXB\"/>\n"
                + "      <ext:property name=\"obj.duration\" value=\"100\"/>\n"
                + "      <ext:property name=\"obj.valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.description\" value=\"${obj.displayName} Object\"/>\n"
                + "      <ext:property name=\"obj.skip\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.executable\" value=\"false\"/>\n"
                + "      <ext:property name=\"obj.child.name\" value=\"A Child\"/>\n"
                + "      <ext:property name=\"obj.child.counter\" value=\"100\"/>\n"
                + "      <ext:property name=\"obj.child.duration\" value=\"200\"/>\n"
                + "      <ext:property name=\"obj.child.valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.child.description\" value=\"${obj.child.name} Element\"/>\n"
                + "      <ext:property name=\"obj.child.skip\" value=\"false\"/>\n"
                + "      <ext:property name=\"obj.child.executable\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.secondObj.valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.child.named.valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.parent.child.valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.parent.child.named.valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.setter.child.valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.getter.child.valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.setter.child.named.valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.children[1].valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.children[2].valid\" value=\"false\"/>\n"
                + "      <ext:property name=\"obj.children[3].valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.children.1.valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.children.2.valid\" value=\"false\"/>\n"
                + "      <ext:property name=\"obj.children.3.valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.child.note\" value=\"A child\"/>\n"
                + "      <ext:property name=\"obj.alias[1]\" value=\"This\"/>\n"
                + "      <ext:property name=\"obj.alias[2]\" value=\"That\"/>\n"
                + "      <ext:property name=\"obj.alias[3]\" value=\"It\"/>\n"
                + "      <ext:property name=\"obj.options1.options1.1\" value=\"skip-invalid\"/>\n"
                + "      <ext:property name=\"obj.options1.options1.2\" value=\"purge-skipped\"/>\n"
                + "      <ext:property name=\"obj.options2.option2.1\" value=\"skip-invalid\"/>\n"
                + "      <ext:property name=\"obj.options2.option2.2\" value=\"purge-skipped\"/>\n"
                + "      <ext:property name=\"obj.wrappedOptions3.options3.1\" value=\"skip-invalid\"/>\n"
                + "      <ext:property name=\"obj.wrappedOptions3.options3.2\" value=\"purge-skipped\"/>\n"
                + "      <ext:property name=\"obj.wrappedOptions4.option4.1\" value=\"skip-invalid\"/>\n"
                + "      <ext:property name=\"obj.wrappedOptions4.option4.2\" value=\"purge-skipped\"/>\n"
                + "      <ext:property name=\"obj.wrappedOptions5.option5.1.valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.wrappedOptions5.option5.2.valid\" value=\"false\"/>\n"
                + "      <ext:property name=\"obj.child.typed.name\" value=\"A Child\"/>\n"
                + "      <ext:property name=\"obj.child1.valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.childList1.1.valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.childList2.1.valid\" value=\"false\"/>\n"
                + "      <ext:property name=\"obj.childList2.2.valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.childList1.2.valid\" value=\"false\"/>\n"
                + "      <ext:property name=\"obj.options6.child1.valid\" value=\"true\"/>\n"
                + "      <ext:property name=\"obj.options6.child2.valid\" value=\"false\"/>\n"
                + "      <ext:property name=\"obj2.annotation\" value=\"WIP\"/>\n"
                + "      <ext:property name=\"obj2.metadata.entry1.key\" value=\"author\"/>\n"
                + "      <ext:property name=\"obj2.metadata.entry1.value\" value=\"Me\"/>\n"
                + "      <ext:property name=\"obj2.metadata.entry2.key\" value=\"obsolete\"/>\n"
                + "      <ext:property name=\"obj2.metadata.entry2.value\" value=\"Yes\"/>\n"
                + "      <ext:property name=\"obj.message1\" value=\"1st rev: An object\"/>\n"
                + "      <ext:property name=\"obj.annotation\" value=\"Dangerous one\"/>\n"
                + "      <ext:property name=\"obj.message2\" value=\"2nd rev: Reduce power\"/>\n"
                + "      <ext:property name=\"obj2.obj.displayName\" value=\"First ref\"/>\n"
                + "      <ext:property name=\"obj2.annotationAttr\" value=\"WIP\"/>"
                + "    </ext:default-properties>\n"
                + "  </ext:property-placeholder>\n"
                + "\n"
                + "  <root xmlns=\"http://example.com/jaxb\">\n"
                + "    <jaxbObject"
                + "      count=\"${obj.count}\""
                + "      displayName=\"${obj.displayName}\""
                + "      duration=\"${obj.duration}\""
                + "      valid=\"${obj.valid}\""
                + "      description=\"${obj.description}\""
                + "      skip=\"${obj.skip}\""
                + "      executable=\"${obj.executable}\""
                + "    >\n"
                + "      <child"
                + "        name=\"${obj.child.name}\""
                + "        counter=\"${obj.child.counter}\""
                + "        duration=\"${obj.child.duration}\""
                + "        valid=\"${obj.child.valid}\""
                + "        description=\"${obj.child.description}\""
                + "        skip=\"${obj.child.skip}\""
                + "        executable=\"${obj.child.executable}\""
                + "      >\n"
                + "        <note>${obj.child.note}</note>\n"
                + "      </child>\n"
                + "      <secondJaxbObject valid=\"${obj.secondObj.valid}\"/>\n"
                + "      <childWithName valid=\"${obj.child.named.valid}\"/>\n"
                + "      <parentChild valid=\"${obj.parent.child.valid}\"/>\n"
                + "      <parentChildWithName valid=\"${obj.parent.child.named.valid}\"/>\n"
                + "      <setterChild valid=\"${obj.setter.child.valid}\"/>\n"
                + "      <getterChild valid=\"${obj.getter.child.valid}\"/>\n"
                + "      <setterChildWithName valid=\"${obj.setter.child.named.valid}\"/>\n"
                + "      <childrenArray valid=\"${obj.children[1].valid}\"/>\n"
                + "      <childrenArray valid=\"${obj.children[2].valid}\"/>\n"
                + "      <childrenArray valid=\"${obj.children[3].valid}\"/>\n"
                + "      <children valid=\"${obj.children.1.valid}\"/>\n"
                + "      <children valid=\"${obj.children.2.valid}\"/>\n"
                + "      <children valid=\"${obj.children.3.valid}\"/>\n"
                + "      <alias>${obj.alias[1]}</alias>\n"
                + "      <alias>${obj.alias[2]}</alias>\n"
                + "      <alias>${obj.alias[3]}</alias>\n"
                + "      <options1>\n"
                + "        <options1>${obj.options1.options1.1}</options1>\n"
                + "        <options1>${obj.options1.options1.2}</options1>\n"
                + "      </options1>\n"
                + "      <options2>\n"
                + "        <option2>${obj.options2.option2.1}</option2>\n"
                + "        <option2>${obj.options2.option2.2}</option2>\n"
                + "      </options2>\n"
                + "      <wrappedOptions3>\n"
                + "        <options3>${obj.wrappedOptions3.options3.1}</options3>\n"
                + "        <options3>${obj.wrappedOptions3.options3.2}</options3>\n"
                + "      </wrappedOptions3>\n"
                + "      <wrappedOptions4>\n"
                + "        <option4>${obj.wrappedOptions4.option4.1}</option4>\n"
                + "        <option4>${obj.wrappedOptions4.option4.2}</option4>\n"
                + "      </wrappedOptions4>\n"
                + "      <wrappedOptions5>\n"
                + "        <option5 valid=\"${obj.wrappedOptions5.option5.1.valid}\"/>\n"
                + "        <option5 valid=\"${obj.wrappedOptions5.option5.2.valid}\"/>\n"
                + "      </wrappedOptions5>\n"
                + "      <typedChild name=\"${obj.child.typed.name}\"/>\n"
                + "      <child1 valid=\"${obj.child1.valid}\"/>\n"
                + "      <childList1 valid=\"${obj.childList1.1.valid}\"/>\n"
                + "      <childList2 valid=\"${obj.childList2.1.valid}\"/>\n"
                + "      <childList2 valid=\"${obj.childList2.2.valid}\"/>\n"
                + "      <childList1 valid=\"${obj.childList1.2.valid}\"/>\n"
                + "      <options6>\n"
                + "        <child1 valid=\"${obj.options6.child1.valid}\"/>\n"
                + "        <child2 valid=\"${obj.options6.child2.valid}\"/>\n"
                + "      </options6>\n"
                + "      <message>${obj.message1}</message>\n"
                + "      <annotation>${obj.annotation}</annotation>\n"
                + "      <message>${obj.message2}</message>\n"
                + "    </jaxbObject>\n"
                + "    <secondJaxbObject annotationAttr=\"${obj2.annotationAttr}\">\n"
                + "      <jaxbObject displayName=\"${obj2.obj.displayName}\"/>\n"
                + "      <annotation>${obj2.annotation}</annotation>\n"
                + "      <metadata>\n"
                + "        <entry key=\"${obj2.metadata.entry1.key}\" value=\"${obj2.metadata.entry1.value}\"/>\n"
                + "        <entry key=\"${obj2.metadata.entry2.key}\" value=\"${obj2.metadata.entry2.value}\"/>\n"
                + "      </metadata>\n"
                + "    </secondJaxbObject>\n"
                + "  </root>\n"
                + "</blueprint>";

        Root root = doUnmarshal(xml, Root.class, AbstractSpecTest.JaxbObject.class, AbstractSpecTest.JaxbObject2.class, AbstractSpecTest.Message.class, AbstractSpecTest.Annotation.class);
        {
            AbstractSpecTest.JaxbObject result = root.getJaxbObject();
            assertThat(result.getCount(), is(3));
            assertThat(result.getId(), is("JAXB"));
            assertThat(result.getLength(), is(100L));
            assertThat(result.isValid(), is(true));
            assertThat(result.getDescription(), is("JAXB Object"));
            assertThat(result.isIgnore(), is(true));
            assertThat(result.isRunnable(), is(false));
            assertThat(result.getChild().getName(), is("A Child"));
            assertThat(result.getChild().getCount(), is(100L));
            assertThat(result.getChild().getLength(), is(200L));
            assertThat(result.getChild().isValid(), is(true));
            assertThat(result.getChild().getDescription(), is("A Child Element"));
            assertThat(result.getChild().isIgnore(), is(false));
            assertThat(result.getChild().isRunnable(), is(true));
            assertThat(result.getGlobalChild().isValid(), is(true));
            assertThat(result.getNamedChild().isValid(), is(true));
            assertThat(result.getParentChild().isValid(), is(true));
            assertThat(result.getNamedParentChild().isValid(), is(true));
            assertThat(result.getSetterChild().isValid(), is(true));
            assertThat(result.getGetterChild().isValid(), is(true));
            assertThat(result.getNamedSetterChild().isValid(), is(true));
            assertThat(result.getChildrenArray()[0].isValid(), is(true));
            assertThat(result.getChildrenArray()[1].isValid(), is(false));
            assertThat(result.getChildrenArray()[2].isValid(), is(true));
            assertThat(result.getChildren().get(0).isValid(), is(true));
            assertThat(result.getChildren().get(1).isValid(), is(false));
            assertThat(result.getChildren().get(2).isValid(), is(true));
            assertThat(result.getAliases(), contains("This", "That", "It"));
            assertThat(result.getOptions1(), contains("skip-invalid", "purge-skipped"));
            assertThat(result.getOptions2(), contains("skip-invalid", "purge-skipped"));
            assertThat(result.getOptions3(), contains("skip-invalid", "purge-skipped"));
            assertThat(result.getOptions4(), contains("skip-invalid", "purge-skipped"));
            assertThat(result.getOptions5().get(0).isValid(), is(true));
            assertThat(result.getOptions5().get(1).isValid(), is(false));
            assertThat(((AbstractSpecTest.JaxbChild) result.getTypedChild()).getName(), is("A Child"));
            assertThat(result.getTwoTypes().isValid(), is(true));
            assertThat(result.getTwoTypeList().get(0).isValid(), is(true));
            assertThat(result.getTwoTypeList().get(1).isValid(), is(false));
            assertThat(result.getTwoTypeList().get(2).isValid(), is(true));
            assertThat(result.getTwoTypeList().get(3).isValid(), is(false));
            assertThat(result.getOptions6().get(0).isValid(), is(true));
            assertThat(result.getOptions6().get(1).isValid(), is(false));
            assertThat(result.getChild().getNote().getText(), is("A child"));
            assertThat(result.getNotes().get(0).getText(), is("1st rev: An object"));
            assertThat(result.getNotes().get(1).getText(), is("Dangerous one"));
            assertThat(result.getNotes().get(2).getText(), is("2nd rev: Reduce power"));
        }
        {
            AbstractSpecTest.JaxbObject2 result = root.getSecondJaxbObject();
            assertThat(((AbstractSpecTest.JaxbObject) result.getMultiGlobalChild()).getId(), is("First ref"));
            assertThat(result.getAnnotation().getText(), is("WIP"));
            assertThat(result.getMetadata(), hasEntry("author", "Me"));
            assertThat(result.getMetadata(), hasEntry("obsolete", "Yes"));
            assertThat(result.getAnnotationAttr().getText(), is("WIP"));
        }
    }

    private static final class UnmarshallerNamespaceHandler implements org.apache.aries.blueprint.NamespaceHandler {

        private final JaxbeanUnmarshaller unmarshaller;
        private final String id;

        public UnmarshallerNamespaceHandler(JaxbeanUnmarshaller unmarshaller, String id) {
            this.unmarshaller = unmarshaller;
            this.id = id;
        }

        public org.osgi.service.blueprint.reflect.Metadata parse(Element element, org.apache.aries.blueprint.ParserContext parserContext) {
            try {
                MutableBeanMetadata beanMetadata = (MutableBeanMetadata) unmarshaller.unmarshal(element, new BlueprintBeanHandler(parserContext));
                beanMetadata.setId(id);

                return beanMetadata;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public URL getSchemaLocation(String string) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public Set<Class> getManagedClasses() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public ComponentMetadata decorate(Node node, ComponentMetadata cm, org.apache.aries.blueprint.ParserContext pc) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
