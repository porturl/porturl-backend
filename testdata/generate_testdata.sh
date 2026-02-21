#!/bin/bash

# =================================================================
#                 CONFIGURATION VARIABLES
# =================================================================
# Keycloak and application details

# The Keycloak server URL, now including the /auth context path
KEYCLOAK_URL="https://sso.friehome.net/auth"
REALM="friehome.net"

# The Client ID for a client that has "Direct Access Grants" enabled
CLIENT_ID="admin-cli"

# Your application's backend URL
API_BASE_URL="http://localhost:8080/api"

# =================================================================
#               SCRIPT LOGIC (No changes needed below)
# =================================================================

# --- Source Environment Variables ---
if [ -f .env ]; then
  export $(cat .env | sed 's/#.*//g' | xargs)
fi

if [ -z "$USERNAME" ] || [ -z "$PASSWORD" ]; then
  echo "Error: USERNAME and PASSWORD must be set in a .env file." >&2
  exit 1
fi

# --- Helper Functions ---

# Base64 encoded placeholder images
IMG_B64="iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkaPj/DwAC/AG6I/SHLwAAAABJRU5ErkJggg=="
IMG_LARGE_B64="iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkaPj/DwAC/AG6I/SHLwAAAABJRU5ErkJggg=="
IMG_MEDIUM_B64="iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkaPj/DwAC/AG6I/SHLwAAAABJRU5ErkJggg=="

# Function to upload an image and return its unique filename
upload_icon() {
  local FILE_PATH=$1
  echo "Uploading icon from $FILE_PATH..." >&2
  local UPLOAD_RESPONSE=$(curl -s -X POST "$API_BASE_URL/images" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -F "file=@$FILE_PATH")

  local FILENAME=$(echo "$UPLOAD_RESPONSE" | jq -r .filename)
  if [ "$FILENAME" == "null" ] || [ -z "$FILENAME" ]; then
    echo "Error: Failed to upload icon '$FILE_PATH'." >&2
    echo "Response: $UPLOAD_RESPONSE" >&2
    echo -n ""
  else
    echo -n "$FILENAME"
  fi
}

# ** REFACTORED FUNCTION **
# Checks if a category exists. If so, returns its ID. If not, creates it and returns the new ID.
create_category() {
  local NAME=$1
  local SORT_ORDER=$2
  local DESCRIPTION=$3
  local APP_SORT_MODE=$4

  # Search the pre-fetched list of categories for a match by name
  local EXISTING_ID=$(echo "$EXISTING_CATEGORIES" | jq -r --arg name "$NAME" '.[] | select(.name == $name) | .id')

  if [ -n "$EXISTING_ID" ] && [ "$EXISTING_ID" != "null" ]; then
    echo "Category '$NAME' already exists with ID: $EXISTING_ID. Skipping creation." >&2
    echo -n "$EXISTING_ID"
  else
    echo "Creating new category: $NAME..." >&2
    local CREATE_RESPONSE=$(curl -s -X POST "$API_BASE_URL/categories" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $ACCESS_TOKEN" \
      -d "{\"name\": \"$NAME\", \"sortOrder\": $SORT_ORDER, \"description\": \"$DESCRIPTION\", \"applicationSortMode\": \"$APP_SORT_MODE\"}")

    local CATEGORY_ID=$(echo "$CREATE_RESPONSE" | jq -r .id)
    if [ "$CATEGORY_ID" == "null" ] || [ -z "$CATEGORY_ID" ]; then
      echo "Error: Failed to create category '$NAME'." >&2
      echo "Response: $CREATE_RESPONSE" >&2
      echo -n ""
    else
      # Add the newly created category to our list for subsequent checks
      EXISTING_CATEGORIES=$(echo "$EXISTING_CATEGORIES" | jq ". + [$CREATE_RESPONSE]")
      echo -n "$CATEGORY_ID"
    fi
  fi
}


# Function to create an application and link it to categories
create_app() {
  local NAME=$1
  local URL=$2
  local ICON=$3
  local ROLES_CSV=$4
  shift 4
  local CATEGORY_IDS=("$@")

  local CATEGORIES_JSON="["
  local FIRST=true
  for CAT_ID in "${CATEGORY_IDS[@]}"; do
    if [ "$FIRST" = false ]; then CATEGORIES_JSON+=","; fi
    CATEGORIES_JSON+="{\"id\": $CAT_ID}"
    FIRST=false
  done
  CATEGORIES_JSON+="]"

  local ROLES_JSON="[]"
  if [[ -n "$ROLES_CSV" ]]; then
    ROLES_JSON="["
    FIRST=true
    IFS=',' read -ra ADDR <<< "$ROLES_CSV"
    for ROLE in "${ADDR[@]}"; do
      if [ "$FIRST" = false ]; then ROLES_JSON+=","; fi
      ROLES_JSON+="\"$ROLE\""
      FIRST=false
    done
    ROLES_JSON+="]"
  fi

  local JSON_PAYLOAD="{\"name\": \"$NAME\", \"url\": \"$URL\", \"categories\": $CATEGORIES_JSON, \"roles\": $ROLES_JSON"
  if [[ -n "$ICON" ]]; then JSON_PAYLOAD+=", \"icon\": \"$ICON\""; fi
  JSON_PAYLOAD+="}"

  echo "Creating application: $NAME..." >&2
  curl -s -X POST "$API_BASE_URL/applications" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d "$JSON_PAYLOAD"
  echo
}

# --- Main Script Execution ---

echo "Attempting to get access token from Keycloak..."
TOKEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "client_id=$CLIENT_ID" -d "username=$USERNAME" -d "password=$PASSWORD" -d "grant_type=password")
ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r .access_token)

if [ "$ACCESS_TOKEN" == "null" ] || [ -z "$ACCESS_TOKEN" ]; then
  echo "Error: Could not obtain access token." >&2
  exit 1
fi
echo "Successfully obtained access token."
echo

# ** NEW STEP **
# Fetch all existing categories once at the beginning of the script.
echo "Fetching existing categories..." >&2
EXISTING_CATEGORIES=$(curl -s -X GET "$API_BASE_URL/categories" -H "Authorization: Bearer $ACCESS_TOKEN")
echo "Found $(echo $EXISTING_CATEGORIES | jq 'length') existing categories."
echo

# Create and upload placeholder images...
echo "Creating and uploading placeholder images..." >&2
echo "$IMG_LARGE_B64" | base64 -d > placeholder_large.png
echo "$IMG_MEDIUM_B64" | base64 -d > placeholder_medium.png
echo "$IMG_B64" | base64 -d > placeholder_thumbnail.png

ICON1=$(upload_icon "placeholder_thumbnail.png")
ICON2=$(upload_icon "placeholder_thumbnail.png")
echo

# --- Create Categories (will now skip if they exist) ---
echo "Creating categories..." >&2
CAT_HARDWARE_ID=$(create_category "Hardware" 0 "Physical server hardware" "CUSTOM")
CAT_MEDIA_ID=$(create_category "Media" 1 "Media streaming and management" "ALPHABETICAL")
CAT_HOME_ID=$(create_category "Home Automation" 2 "Smart home devices and dashboards" "CUSTOM")
CAT_MONITORING_ID=$(create_category "Monitoring" 3 "System and network monitoring" "ALPHABETICAL")
echo "Categories are set up."
echo

# --- Create Applications ---
echo "Creating sample applications..." >&2
create_app "Plex" "https://plex.tv" "$ICON1" "admin,user" "$CAT_MEDIA_ID"
create_app "Synology NAS" "http://192.168.1.10:5000" "$ICON2" "admin" "$CAT_HARDWARE_ID"
create_app "Grafana" "http://192.168.1.21:3000" "$ICON2" "viewer,editor,admin" "$CAT_HOME_ID" "$CAT_MONITORING_ID"
create_app "Home Assistant" "http://192.168.1.20:8123" "$ICON2" "admin,user" "$CAT_HOME_ID"
create_app "GitHub" "https://github.com" "" ""
echo "All applications created!"
echo

# --- Cleanup and Verification ---
echo "Cleaning up temporary images..." >&2
rm placeholder_large.png placeholder_medium.png placeholder_thumbnail.png
echo

echo "Fetching all categories to verify:" >&2
curl -s -X GET "$API_BASE_URL/categories" -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
echo

echo "Fetching all applications to verify:" >&2
curl -s -X GET "$API_BASE_URL/applications" -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
echo

