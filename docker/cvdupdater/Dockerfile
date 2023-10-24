FROM python:slim
WORKDIR /scripts
COPY docker/cvdupdater/updateclamdb.sh updateclamdb.sh
RUN apt update && apt install dos2unix && dos2unix updateclamdb.sh && pip install cvdupdate && cvd && cvd config set --dbdir /persistent/storage/clamdb
