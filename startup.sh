#!/bin/bash
#
# JBILLING CONFIDENTIAL
# _____________________
#
# [2003] - [2012] Enterprise jBilling Software Ltd.
# All Rights Reserved.
#
# NOTICE:  All information contained herein is, and remains
# the property of Enterprise jBilling Software.
# The intellectual and technical concepts contained
# herein are proprietary to Enterprise jBilling Software
# and are protected by trade secret or copyright law.
# Dissemination of this information or reproduction of this material
# is strictly forbidden.


# Startup script for jBilling under Cruise Control. This script sets the
# server port on the running jBilling instance so that multiple jBilling 
# projects can be tested under the same Cruise Control build loop.
#
# see also 'cc-build.properties'

#This will redirect the output to the target file
exec 2>&1> nohup.out

# load properties if file exists, otherwise use port 8080
if [ -f cc-build.properties ]; then
    . cc-build.properties
else
    server_port=8080
fi

# grails runtime options
GRAILS_OPTS_DEFAULT="-server -Xmx1536M -Xms256M -XX:MaxPermSize=384m"
export GRAILS_OPTS=${GRAILS_OPTS:-${GRAILS_OPTS_DEFAULT}}

# start jbilling and record process id
$GRAILS_HOME/bin/grails $GRAILS_STARTUP_OPT -Ddisable.auto.recompile=true -Dserver.port=${server_port} -Dgrails.reload.enabled=false -Dspring.profiles.active="billing.master,billing.slave,mediation.slave" run-app --non-interactive
#echo $!> jbilling.pid
#
#echo "Started jBilling on port ${server_port}."
#
exit 0;
