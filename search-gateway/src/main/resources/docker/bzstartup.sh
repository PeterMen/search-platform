#!/bin/sh
if !(which java 2>/dev/null); then
    echo '请安装java环境'
    exit
fi



PROJECT_NAME="$1"
APPLOG_DIR="/data/ic/app_log/${PROJECT_NAME}"
JAVALOG_DIR="/data/tmp"
DT=`date +"%Y%m%d_%H%M%S"`
agentID=`hostname`

MEM_OPTS="-Xms2g -Xmx2g -Xmn768m"
GC_OPTS="$GC_OPTS -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:CMSInitiatingOccupancyFraction=60 -XX:CMSTriggerRatio=70"
GC_OPTS="$GC_OPTS -Xloggc:${JAVALOG_DIR}/gc_${DT}.log"
GC_OPTS="$GC_OPTS -XX:+PrintGCDateStamps -XX:+PrintGCDetails"
GC_OPTS="$GC_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${JAVALOG_DIR}/heapdump_${DT}.hprof"
START_OPTS="$START_OPTS -Djava.io.tmpdir=${JAVA_LOG}"
START_OPTS="$START_OPTS -DAPPID=${APPID} -DTEAM=${TEAM}"
PINPOINT_OPS="-javaagent:/data/pp-agent/pinpoint-bootstrap-1.6.2.jar -Dpinpoint.applicationName=${PROJECT_NAME} -Dpinpoint.agentId=${agentID}"


mkdir -p "${APPLOG_DIR}"
mkdir -p "${JAVALOG_DIR}"

java  $PINPOINT_OPS  $MEM_OPTS $GC_OPTS $JMX_OPTS $START_OPTS -jar -server   ${PROJECT_NAME}.jar