FROM clamav/clamav:stable
RUN sed -i 's/^DatabaseMirror.*/DatabaseMirror http:\/\/clamav-mirror-service/g' /etc/clamav/freshclam.conf