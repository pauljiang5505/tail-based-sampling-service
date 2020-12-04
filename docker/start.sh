if   [  $SERVER_PORT  ];
then
   java -Xms2048M -Xmx2048M -XX:+UseG1GC -Dserver.port=$SERVER_PORT -jar /usr/local/src/tailbaseSampling-1.0-SNAPSHOT.jar &
else
   java -Xms2048M -Xmx2048M -XX:+UseG1GC -Dserver.port=8000 -jar /usr/local/src/tailbaseSampling-1.0-SNAPSHOT.jar &
fi
tail -f /usr/local/src/start.sh
