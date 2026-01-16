#!/bin/bash
#
# The Thingsboard Authors ("COMPANY") CONFIDENTIAL
#
# Copyright © 2016-2026 The Thingsboard Authors All Rights Reserved.
#
# NOTICE: All information contained herein is, and remains
# the property of The Thingsboard Authors and its suppliers,
# if any.  The intellectual and technical concepts contained
# herein are proprietary to The Thingsboard Authors
# and its suppliers and may be covered by U.S. and Foreign Patents,
# patents in process, and are protected by trade secret or copyright law.
#
# Dissemination of this information or reproduction of this material is strictly forbidden
# unless prior written permission is obtained from COMPANY.
#
# Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
# managers or contractors who have executed Confidentiality and Non-disclosure agreements
# explicitly covering such access.
#
# The copyright notice above does not evidence any actual or intended publication
# or disclosure  of  this source code, which includes
# information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
# ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
# OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
# THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
# AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
# THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
# DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
# OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
#

jarfile=${pkg.installFolder}/bin/${pkg.name}.jar
configfile=${pkg.name}.conf

CONF_FOLDER="/config"
if [ -d "${CONF_FOLDER}" ]; then
  LOGGING_CONFIG="${CONF_FOLDER}/logback.xml"
  source "${CONF_FOLDER}/${configfile}"
  export LOADER_PATH=${CONF_FOLDER},${LOADER_PATH}
else
  CONF_FOLDER="/usr/share/${pkg.name}/conf"
  LOGGING_CONFIG="/usr/share/${pkg.name}/conf/logback.xml"
  source "${CONF_FOLDER}/${configfile}"
fi

echo "Starting '${project.name}' ..."

cd ${pkg.installFolder}/bin

exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.edqs.ThingsboardEdqsApplication \
                    -Dspring.jpa.hibernate.ddl-auto=none \
                    -Dlogging.config=${LOGGING_CONFIG} \
                    org.springframework.boot.loader.launch.PropertiesLauncher
