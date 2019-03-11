# netspeaker-rx
```
Usage: <main class> [COMMAND]

Commands:
  server
  client
```
# server mode
```
Usage: <main class> server [-ag=VALUE] -m=MODE [-p=PORT]
  -m=MODE         mode: <play|record|relay|play_and_relay>
  -p=PORT         port (13801)
      -ag=VALUE   auto gain signal on play or record (0.0)
```

_example command line:_
```
java -jar netspeaker-rx.jar server -m=play_and_relay
```
# client mode
```
Usage: <main class> client [-ag=VALUE] -m=MODE [-p=PORT] -r=REMOTEHOST
  -m=MODE          mode: <play|record>
  -r=REMOTEHOST    remote hostname or ip
  -p=PORT          port (13801)
      -ag=VALUE    auto gain signal on play or record (0.0)
```

_example command line:_
```
java -jar netspeaker-rx.jar client -m=record -r=192.168.1.1
```
