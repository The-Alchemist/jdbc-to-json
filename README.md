# JDBC to JSON Export Tool

A Clojure CLI tool that exports PostgreSQL tables to JSON files using PostgreSQL's `row_to_json` function.

It uses PostgreSQL's `json_to_row` function to convert to JSON, so it's only support on PostgreSQL AFAIK (for now).

## Features

- Connect to any PostgreSQL database
- Export all tables from a specified schema (default: 'public')
- Uses PostgreSQL's native `row_to_json` function for efficient JSON conversion
- Saves each table as a separate JSON file
- Configurable output directory
- Comprehensive logging and error handling
- Command-line interface with validation

## Prerequisites

- [Clojure CLI](https://clojure.org/guides/install_clojure) installed on your system

## Dependencies

- PostgreSQL JDBC Driver
- Clojure tools.cli for command-line parsing
- Clojure java.jdbc for database connectivity
- minimal dependencies
    - `clojure.java.jdbc` for JDBC access
    - `org.postgresql/postgresql` for PostgreSQL drivers
    - `com.taoensso/timbre` for logging

## Usage

### Run the Tool

```bash
clojure -M:run [options]
```

### Command Line Options

- `-h, --host HOST` - Database host (default: localhost)
- `-p, --port PORT` - Database port (default: 5432)
- `-d, --database DATABASE` - Database name (required)
- `-u, --username USERNAME` - Database username (required)
- `-w, --password PASSWORD` - Database password (required)
- `-s, --schema SCHEMA` - Database schema (default: public)
- `-o, --output-dir OUTPUT_DIR` - Output directory for JSON files (default: ./output)
- `--help` - Show help message

### Examples

Export all tables from the 'public' schema:
```bash
clojure -M:run -h localhost -p 5432 -d myapp -u myuser -w mypassword -o ./exports
```

Export from a specific schema:
```bash
clojure -M:run -h db.example.com -p 5432 -d production -u readonly -w secret123 -s analytics -o ./data
```

Export to a specific directory:
```bash
clojure -M:run -d testdb -u testuser -w testpass -o /path/to/exports
```

### Building an Executable

To create a standalone JAR file:

```bash
clojure -X:uberjar
```

Then run with:
```bash
java -jar postgresql-to-json.jar [options]
```

## Output Format

Each table is exported as a separate JSON file named `{table_name}.json`. The JSON structure is an array of objects, where each object represents a row from the table, converted using PostgreSQL's `row_to_json` function.

Example output file (`users.json`):
```json
[
  {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "created_at": "2023-01-15T10:30:00"
  },
  {
    "id": 2,
    "name": "Jane Smith",
    "email": "jane@example.com",
    "created_at": "2023-01-16T14:22:00"
  }
]
```

## Error Handling

The tool includes comprehensive error handling:
- Database connection validation
- Schema and table existence checks
- Individual table export error reporting
- Detailed logging for troubleshooting

If any tables fail to export, the tool will continue with remaining tables and report the failures at the end.

## Development

### Project Structure

```
postgresql-to-json/
├── deps.edn                 # Project configuration and dependencies
├── src/
│   └── postgresql_to_json/
│       └── core.clj         # Main application logic
└── README.md               # This file
```

### Testing

You can test the tool with a local PostgreSQL instance. Make sure PostgreSQL is running and you have a test database with some tables.

### TODO
* support other databases
* use PostgreSQL's `COPY` for better performance
* test on other JDBC databases


## License

EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0 