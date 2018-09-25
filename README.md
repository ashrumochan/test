# Docker Code


docker build -t testdocker .

docker run -p 4000:8080 testdocker

From tomcat:8-jre8
<br>
MAINTAINER "erashru@gmail.com"
<br>
RUN rm -rf /usr/local/tomcat/webapps/ROOT
<br>
ADD ROOT.war /usr/local/tomcat/webapps/
<br>
EXPOSE 8080 80 8888


<br>
## Get Shell of Container
<br>
docker exec -it a3084e215ce2 /bin/bash

<br>
# Removing Dangling Images -- with <none>
<br>
sudo docker image rm $(sudo docker images -f "dangling=true" -q)
