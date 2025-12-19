#
# ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
#
# Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
#
# NOTICE: All information contained herein is, and remains
# the property of ThingsBoard, Inc. and its suppliers,
# if any.  The intellectual and technical concepts contained
# herein are proprietary to ThingsBoard, Inc.
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

FROM ${docker.base.image}

# For tb-report PDF renderer Debian Trixie slim fonts should be installed explicitly
RUN echo "Previously installed font libraries:" \
 && (dpkg -l | grep -E 'font|dejavu|harfbuzz' || echo "  (none)") \
 && apt-get update \
 && apt-get install -y --no-install-recommends \
      libharfbuzz0b fontconfig fonts-dejavu-core \
 && rm -rf /var/lib/apt/lists/* \
 && echo "Current fonts libraries:" \
 && (dpkg -l | grep -E 'font|dejavu|harfbuzz') \
 && echo "Font config list:" \
 && fc-list && test -n "$(fc-list)" \
 && (fc-list | grep -i dejavu) \
 && echo "Harfbuzz library test with ldconfig:" \
 && (ldconfig -p | grep harfbuzz)

COPY logback.xml start-tb-node.sh ${pkg.name}.deb /tmp/

RUN chmod a+x /tmp/*.sh \
    && mv /tmp/start-tb-node.sh /usr/bin && \
    echo "thingsboard thingsboard-pe/license boolean true" | debconf-set-selections && \
    (yes | DEBIAN_FRONTEND=noninteractive dpkg -i /tmp/${pkg.name}.deb) && \
    rm /tmp/${pkg.name}.deb && \
    mv /tmp/logback.xml ${pkg.installFolder}/conf && \
    chown -R ${pkg.user}:${pkg.user} ${pkg.installFolder}/conf/logback.xml && \
    (systemctl --no-reload disable --now ${pkg.name}.service > /dev/null 2>&1 || :) && \
    chown -R ${pkg.user}:${pkg.user} /tmp && \
    mkdir -p /data && \
    chown -R ${pkg.user}:${pkg.user} /data && \
    chmod 555 ${pkg.installFolder}/bin/${pkg.name}.jar

USER ${pkg.user}

VOLUME ["/data"]

CMD ["start-tb-node.sh"]
