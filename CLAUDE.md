# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EasyArchive is a data archiving and migration tool built with Java and Spring Boot. It provides a framework for moving data between databases with support for MySQL as both source and sink.

## Architecture

The project follows a modular architecture with these key components:

- **easyarchive-common**: Core APIs and utilities shared across modules
- **easyarchive-core**: Main business logic including execution engine, configuration, and MySQL implementations
- **easyarchive-starter**: Spring Boot application entry point

Note: MySQL source and sink implementations are included within the `easyarchive-core` module as sub-packages, not as separate modules.

## Key Components

### Core Architecture
- **ArchiveGroupExecutor**: Manages execution of archive groups with thread pooling and locking
- **SyncExecutor**: Handles individual data migration operations
- **ArchiveConfig**: Configuration properties for archive operations
- **TableRule/ArchiveGroup**: Data models defining archive rules and groups

### Data Flow
1. Configuration loaded via `TableRuleLoader`
2. Groups executed concurrently via `ArchiveGroupExecutor`
3. Individual tables processed via `SyncExecutor`
4. Data read from source (`PageSource`) → written to sink (`Sink`) → cleaned from source

### Extension Points
- **PageSource**: Interface for reading data from sources
- **Sink**: Interface for writing data to targets
- **Writer**: Base interface for data writing operations

## Build & Development

### Prerequisites
- Java 8
- Maven 3.x

### Build Commands
```bash
# Build entire project
mvn clean install

# Build specific module
mvn clean install -pl easyarchive-core

# Skip tests
mvn clean install -DskipTests

# Build with tests
mvn clean test

# Run tests for specific module
mvn test -pl easyarchive-core

# Run single test class
mvn test -Dtest=ArchiveGroupExecutorTest

# Run single test method
mvn test -Dtest=ArchiveGroupExecutorTest#testExecuteMethod

# Run tests with verbose output
mvn test -Dtest=*Test -q

# Skip test compilation
mvn test -Dmaven.test.skip=true
```

### Module Structure
```
easy-archive/
├── easyarchive-common/          # Shared APIs and utilities
├── easyarchive-core/            # Main business logic and MySQL implementations
│   ├── source/mysql/           # MySQL data source implementation
│   └── sink/mysql/             # MySQL data sink implementation
├── easyarchive-starter/         # Spring Boot application
└── pom.xml                      # Parent POM with dependency management
```

## Configuration

Archive operations are configured through Spring Boot properties:

```properties
# Source database connection
sync.connection.source=jdbc:mysql://localhost:3306/source_db

# Target database connection
sync.connection.target=jdbc:mysql://localhost:3306/target_db

# Configuration database connection
sync.config.connection=jdbc:mysql://localhost:3306/config_db

# Configuration table
sync.config.table=archive_config

# Performance settings
sync.reader.load.max.rows=5000
sync.reader.load.unit-time.max.try.frequency=10000
sync.archive.step.interval.time=50
```

## Adding New Data Sources/Sinks

To implement a new data source or sink:

1. Implement `PageSource` interface for reading data
2. Implement `Sink` interface (which extends `Writer`) for writing data
3. Add configuration properties in `ArchiveConfig` if needed
4. Register the implementation in the appropriate module

## Database Schema

The tool expects configuration tables to define archive rules:
- **ArchiveGroup**: Groups of tables to archive together
- **ArchiveGroupItem**: Individual table rules within groups
- **ConnectionConfig**: Database connection configurations

## Error Handling

- Uses SLF4J for logging
- Exceptions are wrapped and rethrown using `ExceptionUtils`
- Thread interruption is properly handled in concurrent operations
- Database connections are managed via `ConnectionFactory`

## Performance Considerations

- Configurable batch sizes via `maxLoadRows`
- Throttling via `archiveStepIntervalTime`
- Concurrent group execution with locking
- Connection pooling should be configured at the database level

## Dependencies

Key dependencies managed in parent POM:
- Spring Boot 2.3.2
- MySQL Connector 5.1.49
- MyBatis 3.5.0
- Guava 31.1-jre
- Lombok (for code generation)

## Development Environment

### IDE Setup
- **IntelliJ IDEA**: Project includes `.idea` configuration files
- **VS Code**: Use Java Extension Pack for Maven support
- **Eclipse**: Import as Maven project

### Code Style
- Uses Lombok for code generation (requires Lombok plugin in IDE)
- Java 8 language level
- UTF-8 encoding enforced in Maven configuration

### Git Workflow
```bash
# Check status
git status

# Create feature branch
git checkout -b feature/new-data-source

# Commit changes
git add .
git commit -m "feat: add new data source implementation"

# Push to remote
git push origin feature/new-data-source
```