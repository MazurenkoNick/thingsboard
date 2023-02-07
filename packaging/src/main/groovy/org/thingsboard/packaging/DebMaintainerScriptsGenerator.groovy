/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.packaging

import com.netflix.gradle.plugins.deb.Deb
import com.netflix.gradle.plugins.deb.TemplateHelper
import com.netflix.gradle.plugins.utils.FileSystemActions
import groovy.transform.Canonical
import groovy.transform.CompileDynamic

class DebMaintainerScriptsGenerator {
    private final Deb task
    private final TemplateHelper templateHelper
    private final File destination
    private final FileSystemActions fileSystem

    DebMaintainerScriptsGenerator(Deb task, TemplateHelper templateHelper, File destination, FileSystemActions fileSystem) {
        this.destination = destination
        this.task = task
        this.templateHelper = templateHelper
        this.fileSystem = fileSystem
    }

    void generate(Map<String, Object> context) {
        templateHelper.generateFile("control", context)

        def configurationFiles = task.allConfigurationFiles
        if (configurationFiles.any()) {
            templateHelper.generateFile("conffiles", [files: configurationFiles] )
        }

        List<MaintainerScript> scripts = [
                new MaintainerScript("preinst", task.preInstallFile, task.allPreInstallCommands),
                new PostInstScript(task.postInstallFile, task.allPostInstallCommands, context),
                new MaintainerScript("prerm", task.preUninstallFile, task.allPreUninstallCommands),
                new MaintainerScript("postrm", task.postUninstallFile, task.allPostUninstallCommands)
        ]
        if (task.configFile) {
            scripts << new MaintainerScript("config", task.configFile, [])
        }
        if (task.templatesFile) {
            scripts << new MaintainerScript("templates", task.templatesFile, [])
        }

        def installUtils = task.allCommonCommands.collect { stripShebang(it) }
        for (script in scripts) {
            if(script.file) {
                fileSystem.copy(script.file, new File(destination, script.name))
            } else if (script.needsTemplateGeneration()) {
                Map<String, Object> commands = [commands: installUtils + script.commands.collect { stripShebang(it) }] as Map<String, Object>
                templateHelper.generateFile(script.name, context + commands)
            }
        }
    }

    /**
     * Works with nulls, Strings and Files.
     *
     * @param script
     * @return
     */
    @CompileDynamic
    private static String stripShebang(Object script) {
        StringBuilder result = new StringBuilder()
        script?.eachLine { line ->
            if (!line.matches('^#!.*$')) {
                result.append line
                result.append "\n"
            }
        }
        result.toString()
    }

    @Canonical
    private static class MaintainerScript {
        String name
        File file
        List<Object> commands

        boolean needsTemplateGeneration() {
            return commands
        }
    }

    private static class PostInstScript extends MaintainerScript {
        Map<String, Object> context

        PostInstScript(File file, List<Object> commands, Map<String, Object> context) {
            super("postinst", file, commands)
            this.context = context
        }

        @Override
        boolean needsTemplateGeneration() {
            return super.needsTemplateGeneration() || context['dirs']
        }
    }
}

