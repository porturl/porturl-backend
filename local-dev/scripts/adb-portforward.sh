#!/bin/bash

adb reverse tcp:8443 tcp:8443 || echo "Warning: No Android device found for adb reverse"
adb reverse tcp:8081 tcp:8081 || true
adb reverse tcp:8080 tcp:8080 || true

