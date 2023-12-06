# How to run the server
## Set up Environment
- Three different leveldb directories.
    1. MAPPING_ROOT=/root/mapping/dw
    2. SELECTED_TABLES=/root/mapping/tabs
    3. CSV_MAPPING_ROOT=/root/mapping/csv
- One temp directory.
    - TMP_ROOT=/root/tmp
- One file storage directory.
    - FILE_ROOT=/root/file
- One persistence property file. (Store in the static directory by default.)
    - PROPERTY_PATH=${WORK_DIR}/src/main/resources/static/persistence.properties
  
