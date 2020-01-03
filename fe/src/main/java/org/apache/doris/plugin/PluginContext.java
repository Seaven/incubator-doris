// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.doris.common.util.DigitalVersion;

import com.google.common.base.Strings;

public class PluginContext {

    private static final String DEFAULT_PLUGIN_PROPERTIES = "plugin.properties";

    private static final short PLUGIN_WORK_IN_FE = 1;

    private static final short PLUGIN_WORK_IN_BE = 2;

    protected String name;

    protected PluginType type;

    protected String description;

    protected DigitalVersion version;

    protected DigitalVersion javaVersion;

    protected String className;

    protected String soName;

    protected String source;

    protected String installPath;

    private short flags;

    public PluginContext(String name, PluginType type, String description, DigitalVersion version, DigitalVersion javaVersion,
                         String className, String soName, String source) {

        this.name = name;
        this.type = type;
        this.description = description;
        this.version = version;
        this.javaVersion = javaVersion;
        this.className = className;
        this.soName = soName;
        this.source = source;
    }

    public static PluginContext readFromProperties(final Path propertiesPath, final String source) throws IOException {
        final Path descriptor = propertiesPath.resolve(DEFAULT_PLUGIN_PROPERTIES);

        final Map<String, String> propsMap;
        {
            final Properties props = new Properties();
            try (InputStream stream = Files.newInputStream(descriptor)) {
                props.load(stream);
            }
            propsMap = props.stringPropertyNames().stream()
                    .collect(Collectors.toMap(Function.identity(), props::getProperty));
        }

        final String name = propsMap.remove("name");
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException(
                    "property [name] is missing in [" + descriptor + "]");
        }

        final String description = propsMap.remove("description");

        final PluginType type;
        final String typeStr = propsMap.remove("type");
        try {
            type = PluginType.valueOf(StringUtils.upperCase(typeStr));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("property [type] is missing for plugin [" + typeStr + "]");
        }

        final String versionString = propsMap.remove("version");
        if (null == versionString) {
            throw new IllegalArgumentException(
                    "property [version] is missing for plugin [" + name + "]");
        }

        DigitalVersion version = DigitalVersion.fromString(versionString);

        final String javaVersionString = propsMap.remove("java.version");
        DigitalVersion javaVersion = DigitalVersion.JDK_1_8_0;
        if (null != javaVersionString) {
            javaVersion = DigitalVersion.fromString(javaVersionString);
        }

        final String className = propsMap.remove("classname");

        final String soName = propsMap.remove("soname");

        // version check
        if (version.before(DigitalVersion.CURRENT_DORIS_VERSION)) {
            throw new IllegalArgumentException("plugin version is too old. plz recompile and modify property "
                    + "[version]");
        }

        PluginContext p = new PluginContext(name, type, description, version, javaVersion, className, soName, source);
        p.installPath = propertiesPath.toString();
        return p;
    }

    public String getName() {
        return name;
    }

    public PluginType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public DigitalVersion getVersion() {
        return version;
    }

    public DigitalVersion getJavaVersion() {
        return javaVersion;
    }

    public String getClassName() {
        return className;
    }

    public String getSoName() {
        return soName;
    }

    public String getSource() {
        return source;
    }

    public String getInstallPath() {
        return installPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PluginContext that = (PluginContext) o;
        return Objects.equals(name, that.name) &&
                type == that.type &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
