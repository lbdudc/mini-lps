/*%@
if (!feature.MV_Processes) return [];
return [{ fileName: fileName, basePath: basePath, context: context }];
%*/
# QGIS Process Translator

## Overview

This script extracts translations for a QGIS processing algorithm, including its name, description, input parameters, and output parameters, in different languages.

> NOTE: This solution is a workaround for a limitation in QGIS WPS, which strictly follows the OGC WPS standard. WPS requests do not include user or session context and cannot accept custom parameters, so this `qgis` module is designed to work with a client that handles this and is meant to include the translations extracted with this script. 

## Features

- Retrieves process descriptions and parameter details in different languages.
- Uses local QGIS translation files (`.qm`).
- Works across Windows and Linux.
- Outputs results in a structured JSON format.

## Requirements

- QGIS installed on the system.
- Python 3.x.
- The `processing` module must be available in QGIS.

## Installation

### Linux (Ubuntu/Debian)

Ensure you have QGIS installed:

```sh
sudo apt update && sudo apt install qgis python3-qgis
```

### Windows

Windows requires additional configuration because QGIS bundles its own Python environment that is not automatically accessible from your system Python. The steps to follow are explained in detail in this [tutorial](https://www.youtube.com/watch?v=9i16cFZy5M4&t=307s).

> NOTE: Steps may vary depending on the installed version. The following steps work for QGIS version 3.40.7.

#### 1. Install QGIS

Download and install QGIS from [QGIS.org](https://qgis.org/en/site/).

#### 2. Locate QGIS directory

Locate QGIS installation directory and move into the Python directory inside QGIS. In my case `C:\Program Files\QGIS 3.40.7\apps\Python312`. 

#### 3. Create a dedicated QGIS Python executable

Inside the directory above:

- Copy `python.exe`
- Rename the copy to: `python3qgis.exe`.

#### 4. Add the required environment variables

Open System Properties → Environment Variables and update the following variable values:

- `Path`: C:\Program Files\QGIS 3.40.7\apps\Python312\python3qgis;
C:\Program Files\QGIS 3.40.7\apps\Python312

- `PYTHONPATH`: C:\Program Files\QGIS 3.40.7\apps\qgis-ltr\python;
C:\Program Files\QGIS 3.40.7\apps\qgis-ltr\python\plugins;
C:\Program Files\QGIS 3.40.7\apps\qt5\plugins;
C:\Program Files\QGIS 3.40.7\apps\gdal\share\gdal


#### 5. Run scripts using the new QGIS Python

Use `python3qgis` instead of `python`:

```
python3qgis script.py <process_name> [context_name]
```

## Troublesome

- Depending on you system or the version of QGIS installed you might need to change translation paths on the script. Current script is made to work with version 3.40.7.

```python
TRANSLATION_PATHS = {
    "win": "C:/Program Files/QGIS 3.40.7/apps/qt5/translations/qt_{lang}.qm",
    "linux": "/usr/share/qgis/i18n/qgis_{lang}.qm",
}
```

## Usage

### Running the Script

Execute the script with the following syntax:

```
python script.py <process_name> [context_name]
```

#### Example:

```sh
python script.py native:buffer
```

This will extract the translations for the `native:buffer` algorithm.

#### Example with Context:

```sh
python script.py native:buffer QObject
```

The context in Qt translations determines which part of the application the text belongs to, allowing the translation system to choose the correct translation for identical words. If the context name is omitted, it defaults to `QObject`. Some of the most common translation contexts for processes are: `Processing` and `QGISAlgorithm`.

## Output Format

The script outputs a JSON object containing the translated process name, description, and parameter details:

```json
{
  "process": {
    "buffer": {
      "title": "Buffer",
      "description": "This algorithm creates a buffer area for all features in an input layer."
    }
  },
  "param": {
    "distance": {
      "title": "Distance",
      "description": "Distance for the buffer zone."
    }
  }
}
```

