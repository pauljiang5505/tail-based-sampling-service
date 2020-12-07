if   [  $SERVER_PORT  ];
then
   java -Dserver.port=$SERVER_PORT -jar /usr/local/src/tail-based-sampling-1.0-SNAPSHOT-jar-with-dependencies.jar &
else
   java -Dserver.port=8000 -jar /usr/local/src/tail-based-sampling-1.0-SNAPSHOT-jar-with-dependencies.jar &
fi
tail -f /usr/local/src/start.sh