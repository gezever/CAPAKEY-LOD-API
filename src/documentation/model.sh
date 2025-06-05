#!/bin/bash  

  curl -H "Accept: text/turtle" http://localhost:8080/id/perceel/24504D0693/00B000 > /tmp/test.ttl

  sparql --results=TTL --data=/tmp/test.ttl  --query model.rq  > model.ttl
  rdf2dot  model.ttl | dot -Tpng > model.png
  rdf2dot  model.ttl  > model.dot
