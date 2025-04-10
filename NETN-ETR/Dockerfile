FROM busybox:latest
ENTRYPOINT [ "sh", "-c", " \
   if [ -n \"$VOLUME\" -a -n \"$1\" ]; then \
      if [ -n \"$OPTS\" ]; then \
         cp $OPTS $VOLUME $1; \
      else \
         cp -a $VOLUME $1; \
      fi; \
      echo \"Copied $VOLUME to $1\"; \
   fi" , \
"--" ]

ARG version
ARG name

ADD ./build/distributions/${name}-${version}.tar /root/conf/TestSuites/
ENV VOLUMEDIR /root/conf/TestSuites/${name}-${version}
ENV VOLUME ${VOLUMEDIR}/.
VOLUME ${VOLUMEDIR}
