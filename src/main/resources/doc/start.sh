#!/bin/bash
# --sun-misc-unsafe-memory-access=allow: XStream, which reads and writes the database, asks once
# per start whether it may write fields through sun.misc.Unsafe by trying it. That method is
# deprecated for removal, so JDK 25 answers with a five line warning on every start. XStream falls
# back on its own when the call stops working, so this permits what it is going to do anyway.
java --add-opens java.base/java.time=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow \
  -jar mystic-crypt-ui.jar
