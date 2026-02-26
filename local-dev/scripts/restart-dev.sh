#!/bin/bash
set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
IMPORT_DIR="$BASE_DIR/keycloak-import"
TLS_DIR="$BASE_DIR/tls"
CA_KEY="$TLS_DIR/local_ca.key"
CA_CERT="$TLS_DIR/local_ca.crt"
SERVER_KEY="$TLS_DIR/server.key"
SERVER_CERT="$TLS_DIR/server.crt"
SERVER_CSR="$TLS_DIR/server.csr"
TRUSTSTORE="$TLS_DIR/truststore.jks"

# Load variables from .env
if [ -f "$BASE_DIR/.env" ]; then
    export $(grep -v '^#' "$BASE_DIR/.env" | xargs)
fi

mkdir -p "$TLS_DIR"

# 1. Create Root CA if it doesn't exist (valid for 10 years)
if [ ! -f "$CA_CERT" ]; then
    echo "--- Creating persistent local Root CA (10 years) ---"
    openssl genrsa -out "$CA_KEY" 4096
    
    # Config for Root CA
    cat > "$TLS_DIR/ca.cnf" <<EOF
[req]
prompt = no
distinguished_name = dn
x509_extensions = v3_ca

[dn]
C = DE
ST = Local
L = Local
O = PortUrl Dev CA
CN = PortUrl Root CA

[v3_ca]
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
basicConstraints = critical, CA:TRUE
keyUsage = critical, digitalSignature, cRLSign, keyCertSign
EOF

    openssl req -x509 -new -nodes -key "$CA_KEY" -sha256 -days 3650 \
        -out "$CA_CERT" \
        -config "$TLS_DIR/ca.cnf"
fi

# 2. Generate Server Certificate (Always regenerate for fresh window)
echo "--- Generating fresh server certificate (valid for 90 days) ---"

# Server Key
openssl genrsa -out "$SERVER_KEY" 2048

# Config for CSR
cat > "$TLS_DIR/server.cnf" <<EOF
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
req_extensions = v3_req

[dn]
C = DE
ST = Local
L = Local
O = PortUrl Dev
CN = localhost

[v3_req]
basicConstraints = CA:FALSE
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = localhost
IP.1 = 127.0.0.1
IP.2 = 10.0.2.2
EOF

# Create CSR
openssl req -new -key "$SERVER_KEY" -out "$SERVER_CSR" -config "$TLS_DIR/server.cnf"

# Sign with Root CA
openssl x509 -req -in "$SERVER_CSR" -CA "$CA_CERT" -CAkey "$CA_KEY" \
    -CAcreateserial -out "$SERVER_CERT" -days 90 -sha256 \
    -extensions v3_req -extfile "$TLS_DIR/server.cnf"

# Always refresh the Java TrustStore to ensure it matches the current CA
echo "--- Refreshing Java TrustStore ---"
rm -f "$TRUSTSTORE"
keytool -importcert -alias porturl-ca -file "$CA_CERT" \
    -keystore "$TRUSTSTORE" -storepass "changeit" -noprompt

echo "Certificates generated in $TLS_DIR"

# 1.1 Copy CA certificate to Android project if path is set
if [ ! -z "$ANDROID_PROJECT_PATH" ]; then
    FULL_ANDROID_PATH="$(cd "$BASE_DIR" && cd "$ANDROID_PROJECT_PATH" && pwd)"
    ANDROID_RES_RAW="$FULL_ANDROID_PATH/app/src/main/res/raw"
    
    if [ -d "$ANDROID_RES_RAW" ]; then
        echo "Copying Root CA to Android project: $ANDROID_RES_RAW"
        cp "$CA_CERT" "$ANDROID_RES_RAW/local_ca.crt"
    else
        echo "Warning: Android res/raw directory not found at $ANDROID_RES_RAW"
    fi
fi

# 1.2 Ensure the container user (UID 1000) can read the files
echo "--- Setting TLS file permissions ---"
# Use podman unshare for everything to ensure we have permission even if files are mapped to UID 1000
podman unshare chmod 777 "$TLS_DIR"
podman unshare chmod 666 "$TLS_DIR"/*
podman unshare chown -R 1000:1000 "$TLS_DIR"

# 2. Run the environment reset
echo "--- Stopping environment ---"
podman-compose -f "$BASE_DIR/docker-compose.yml" --env-file "$BASE_DIR/.env" down

echo "--- Cleaning database volume ---"
podman volume rm local-dev_keycloak_db_data || true

echo "--- Starting environment ---"
podman-compose -f "$BASE_DIR/docker-compose.yml" --env-file "$BASE_DIR/.env" up -d

# 3. Setup Android Port Reversal
if command -v adb &> /dev/null; then
    echo "--- Setting up Android ADB port reversal for Emulator ---"
    echo "This allows the emulator to access 'localhost' on your host machine."
    adb reverse tcp:8443 tcp:8443 || echo "Warning: No Android device found for adb reverse"
    adb reverse tcp:8081 tcp:8081 || true
    adb reverse tcp:8080 tcp:8080 || true
fi

echo "--- Following logs ---"
podman-compose -f "$BASE_DIR/docker-compose.yml" --env-file "$BASE_DIR/.env" logs -t -n -f --tail 20
