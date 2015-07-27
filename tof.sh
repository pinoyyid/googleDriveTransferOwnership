#!/bin/bash
# run the tof java app
export CLASSPATH=out/production/tof:libs/gson-2.1.jar:libs/google-api-client-1.20.0.jar:libs/google-http-client-1.20.0.jar:libs/google-api-client-gson-1.20.0.jar:libs/jackson-core-2.1.3.jar:libs/jackson-core-asl-1.9.11.jar:libs/google-http-client-jackson-1.20.0.jar:libs/google-http-client-jackson2-1.20.0.jar:libs/google-api-services-drive-v2-rev179-1.20.0.jar:libs/google-oauth-client-1.20.0.jar 
java couk.cleverthinking.tof.Main
