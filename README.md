# JDBC to JSONL Export Tool

A Clojure CLI tool that exports PostgreSQL tables to JSONL (JSON Lines) files and imports them back to PostgreSQL databases.

The export uses PostgreSQL's native JDBC drivers for efficient data conversion to JSON format.

## Features

- Connect to any PostgreSQL database
- Export all tables from a specified schema to JSONL format (default: 'public')
- Import JSONL files back to PostgreSQL databases
- Each table saved as a separate JSONL file (one JSON object per line)
- Configurable output/input directories
- Advanced import options: create tables, clear, skip columns/tables, disable foreign keys
- Comprehensive logging and error handling
- Command-line interface with validation

## Prerequisites

- [Clojure CLI](https://clojure.org/guides/install_clojure) installed on your system

## Dependencies

- PostgreSQL JDBC Driver
- Clojure tools.cli for command-line parsing
- Clojure java.jdbc for database connectivity
- Cheshire for JSON processing
- minimal dependencies
    - `clojure.java.jdbc` for JDBC access
    - `org.postgresql/postgresql` for PostgreSQL drivers
    - `cheshire/cheshire` for JSON parsing/generation
    - `com.taoensso/timbre` for logging

## Usage

### Export Tables to JSONL

```bash
clojure -M:run [options]
```

### Import JSONL Files to Database

```bash
clojure -M:import [options]
```

### Command Line Options

#### Export Options
- `-h, --host HOST` - Database host (default: localhost)
- `-p, --port PORT` - Database port (default: 5432)
- `-d, --database DATABASE` - Database name (required)
- `-u, --username USERNAME` - Database username (required)
- `-w, --password PASSWORD` - Database password (required)
- `-s, --schema SCHEMA` - Database schema (default: public)
- `-o, --output-dir OUTPUT_DIR` - Output directory for JSONL files (default: ./output)
- `--help` - Show help message

#### Import Options
- All database connection options above, plus:
- `-i, --input-dir INPUT_DIR` - Input directory containing JSONL files (default: ./output)
- `-F, --file FILE` - Import a single JSONL file instead of all files
- `-c, --create-tables` - Create tables if they don't exist
- `-t, --clear` - Clear existing tables before import
- `-f, --disable-foreign-keys` - Disable foreign key constraints during import
- `-S, --skip-columns COLUMNS` - Skip columns (format: table_name.column_name,table2.col2)
- `-T, --skip-tables TABLES` - Skip tables (comma-separated table names)

### Examples

#### Export Examples

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

#### Import Examples

Import all JSONL files from a directory:
```bash
clojure -M:import -h localhost -p 5432 -d myapp -u myuser -w mypassword -i ./exports
```

Import a single table with table creation:
```bash
clojure -M:import -h localhost -p 5432 -d myapp -u myuser -w mypassword -F users.jsonl -c
```

Import with column exclusions:
```bash
clojure -M:import -h localhost -p 5432 -d myapp -u myuser -w mypassword -S users.created_at,posts.internal_id
```

### Building an Executable

To create a standalone JAR file:

```bash
clojure -X:uberjar
```

Then run with:
```bash
java -jar jdbc-to-json.jar [options]
```

## Output Format

Each table is exported as a separate JSONL file named `{table_name}.jsonl`. JSONL (JSON Lines) format contains one JSON object per line, making it efficient for streaming and processing large datasets.

Example output file (`users.jsonl`):
```json
{"id": 1, "name": "John Doe", "email": "john@example.com", "created_at": "2023-01-15T10:30:00"}
{"id": 2, "name": "Jane Smith", "email": "jane@example.com", "created_at": "2023-01-16T14:22:00"}
{"id": 3, "name": "Bob Wilson", "email": "bob@example.com", "created_at": "2023-01-17T09:15:30"}
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
jdbc-to-json/
├── deps.edn                 # Project configuration and dependencies
├── src/
│   └── jdbc_to_json/
│       ├── core.clj         # Export functionality
│       ├── import.clj       # Import functionality
│       └── common.clj       # Shared utilities
└── README.md               # This file
```

### Testing

You can test the tool with a local PostgreSQL instance. Make sure PostgreSQL is running and you have a test database with some tables.

### TODO
* support other databases beyond PostgreSQL
* use PostgreSQL's `COPY` for better export performance
* add support for streaming large tables
* add data validation options
* support for custom data transformations


## License

EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0 