#basic image
FROM registry.cn-shanghai.aliyuncs.com/tcc-public/java:jdk_13.0.2

COPY tail-based-sampling-1.0-SNAPSHOT-jar-with-dependencies.jar /usr/local/src
WORKDIR /usr/local/src
COPY start.sh /usr/local/src
RUN chmod +x /usr/local/src/start.sh
ENTRYPOINT ["/bin/bash", "/usr/local/src/start.sh"]