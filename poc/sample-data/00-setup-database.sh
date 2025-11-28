#!/bin/bash

################################################################################
# Document Hub POC - Database Setup Script
################################################################################
# Purpose: Automated database setup and sample data loading
# Version: 1.0
# Date: 2025-11-27
################################################################################

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Database configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-dochub_poc}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-}"

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_DIR="$SCRIPT_DIR"

################################################################################
# Functions
################################################################################

print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Check if PostgreSQL client is installed
check_prerequisites() {
    print_header "Checking Prerequisites"

    if ! command -v psql &> /dev/null; then
        print_error "PostgreSQL client (psql) not found"
        echo "Please install PostgreSQL client:"
        echo "  - macOS: brew install postgresql"
        echo "  - Ubuntu: sudo apt-get install postgresql-client"
        echo "  - Windows: Download from https://www.postgresql.org/download/"
        exit 1
    fi

    print_success "PostgreSQL client found: $(psql --version)"
}

# Test database connection
test_connection() {
    print_header "Testing Database Connection"

    print_info "Connecting to $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME"

    if [ -n "$DB_PASSWORD" ]; then
        export PGPASSWORD="$DB_PASSWORD"
    fi

    if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "SELECT 1" > /dev/null 2>&1; then
        print_success "Database connection successful"
    else
        print_error "Cannot connect to database"
        echo ""
        echo "Connection details:"
        echo "  Host: $DB_HOST"
        echo "  Port: $DB_PORT"
        echo "  User: $DB_USER"
        echo "  Database: postgres"
        echo ""
        echo "Please check your connection settings and try again."
        exit 1
    fi
}

# Create database if it doesn't exist
create_database() {
    print_header "Creating Database"

    # Check if database exists
    DB_EXISTS=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'")

    if [ "$DB_EXISTS" = "1" ]; then
        print_warning "Database '$DB_NAME' already exists"
        read -p "Do you want to drop and recreate it? (yes/no): " -r REPLY
        echo
        if [[ $REPLY =~ ^[Yy]es$ ]]; then
            print_info "Dropping existing database..."
            psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"
            psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;"
            print_success "Database recreated"
        else
            print_info "Using existing database"
        fi
    else
        print_info "Creating database '$DB_NAME'..."
        psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;"
        print_success "Database created"
    fi
}

# Create schema
create_schema() {
    print_header "Creating Database Schema"

    SCHEMA_FILE="../../database/schemas/document_hub_schema.sql"

    if [ ! -f "$SCHEMA_FILE" ]; then
        print_error "Schema file not found: $SCHEMA_FILE"
        print_info "Looking for schema file in alternative locations..."

        # Try alternative paths
        SCHEMA_FILE="../database/schemas/document_hub_schema.sql"
        if [ ! -f "$SCHEMA_FILE" ]; then
            print_error "Cannot find schema file"
            echo "Please ensure document_hub_schema.sql exists in:"
            echo "  - database/schemas/document_hub_schema.sql"
            exit 1
        fi
    fi

    print_info "Executing schema file: $SCHEMA_FILE"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$SCHEMA_FILE"

    print_success "Schema created successfully"
}

# Load templates
load_templates() {
    print_header "Loading Template Definitions"

    TEMPLATE_FILE="$SQL_DIR/01-templates.sql"

    if [ ! -f "$TEMPLATE_FILE" ]; then
        print_error "Template file not found: $TEMPLATE_FILE"
        exit 1
    fi

    print_info "Loading templates from: $TEMPLATE_FILE"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$TEMPLATE_FILE"

    # Verify templates loaded
    TEMPLATE_COUNT=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tAc "SELECT COUNT(*) FROM master_template_definition;")
    print_success "Loaded $TEMPLATE_COUNT template(s)"
}

# Load documents
load_documents() {
    print_header "Loading Sample Documents"

    DOCUMENT_FILE="$SQL_DIR/02-documents.sql"

    if [ ! -f "$DOCUMENT_FILE" ]; then
        print_error "Document file not found: $DOCUMENT_FILE"
        exit 1
    fi

    print_info "Loading documents from: $DOCUMENT_FILE"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$DOCUMENT_FILE"

    # Verify documents loaded
    DOCUMENT_COUNT=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tAc "SELECT COUNT(*) FROM storage_index;")
    print_success "Loaded $DOCUMENT_COUNT document(s)"
}

# Verify data
verify_data() {
    print_header "Verifying Data"

    print_info "Template Summary:"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
        SELECT
            template_type,
            template_version,
            template_name,
            is_active,
            is_shared_document
        FROM master_template_definition
        ORDER BY template_type;
    "

    print_info "Document Summary:"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
        SELECT
            template_type,
            COUNT(*) as document_count,
            SUM(CASE WHEN is_shared THEN 1 ELSE 0 END) as shared_count,
            SUM(CASE WHEN is_shared THEN 0 ELSE 1 END) as customer_specific_count
        FROM storage_index
        GROUP BY template_type
        ORDER BY template_type;
    "

    print_success "Data verification complete"
}

# Create database indices for performance
create_indices() {
    print_header "Creating Performance Indices"

    print_info "Creating indices on storage_index table..."

    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << EOF
        -- Index for account key lookups
        CREATE INDEX IF NOT EXISTS idx_storage_index_account_key
        ON storage_index(account_key)
        WHERE account_key IS NOT NULL;

        -- Index for customer key lookups
        CREATE INDEX IF NOT EXISTS idx_storage_index_customer_key
        ON storage_index(customer_key)
        WHERE customer_key IS NOT NULL;

        -- Index for reference key lookups
        CREATE INDEX IF NOT EXISTS idx_storage_index_reference_key
        ON storage_index(reference_key, reference_key_type)
        WHERE reference_key IS NOT NULL;

        -- Index for template type
        CREATE INDEX IF NOT EXISTS idx_storage_index_template_type
        ON storage_index(template_type);

        -- Index for shared documents
        CREATE INDEX IF NOT EXISTS idx_storage_index_shared
        ON storage_index(is_shared)
        WHERE is_shared = true;

        -- GIN index for JSONB metadata
        CREATE INDEX IF NOT EXISTS idx_storage_index_metadata
        ON storage_index USING GIN(doc_metadata);

        -- Index for template lookups
        CREATE INDEX IF NOT EXISTS idx_master_template_active
        ON master_template_definition(template_type, is_active)
        WHERE is_active = true;
EOF

    print_success "Indices created successfully"
}

# Display summary
display_summary() {
    print_header "Setup Complete"

    echo -e "${GREEN}✅ Database setup successful!${NC}\n"

    echo "Database Details:"
    echo "  Host: $DB_HOST"
    echo "  Port: $DB_PORT"
    echo "  Database: $DB_NAME"
    echo "  User: $DB_USER"
    echo ""

    echo "Next Steps:"
    echo "  1. Review test scenarios: cat 03-test-scenarios.md"
    echo "  2. Start the POC application:"
    echo "     cd ../doc-hub-poc"
    echo "     mvn spring-boot:run"
    echo "  3. Test the API:"
    echo "     curl -X POST http://localhost:8080/api/documents/enquiry \\"
    echo "       -H 'Content-Type: application/json' \\"
    echo "       -d '{\"customerId\":\"C001\",\"accountId\":\"A001\",\"templateType\":\"MONTHLY_STATEMENT\"}'"
    echo ""

    print_info "For detailed test scenarios, see: 03-test-scenarios.md"
}

# Cleanup function for errors
cleanup_on_error() {
    print_error "Setup failed"
    echo ""
    echo "To retry, run: $0"
    exit 1
}

################################################################################
# Main Execution
################################################################################

trap cleanup_on_error ERR

main() {
    print_header "Document Hub POC - Database Setup"

    echo "This script will:"
    echo "  1. Check prerequisites"
    echo "  2. Test database connection"
    echo "  3. Create database (if needed)"
    echo "  4. Create schema"
    echo "  5. Load template definitions"
    echo "  6. Load sample documents"
    echo "  7. Create performance indices"
    echo "  8. Verify data"
    echo ""

    read -p "Continue? (yes/no): " -r REPLY
    echo
    if [[ ! $REPLY =~ ^[Yy]es$ ]]; then
        print_info "Setup cancelled"
        exit 0
    fi

    check_prerequisites
    test_connection
    create_database
    create_schema
    load_templates
    load_documents
    create_indices
    verify_data
    display_summary
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --host)
            DB_HOST="$2"
            shift 2
            ;;
        --port)
            DB_PORT="$2"
            shift 2
            ;;
        --database)
            DB_NAME="$2"
            shift 2
            ;;
        --user)
            DB_USER="$2"
            shift 2
            ;;
        --password)
            DB_PASSWORD="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  --host HOST         Database host (default: localhost)"
            echo "  --port PORT         Database port (default: 5432)"
            echo "  --database NAME     Database name (default: dochub_poc)"
            echo "  --user USER         Database user (default: postgres)"
            echo "  --password PASS     Database password (default: prompt)"
            echo "  --help              Show this help message"
            echo ""
            echo "Environment Variables:"
            echo "  DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD"
            echo ""
            echo "Example:"
            echo "  $0 --host localhost --database dochub_poc --user postgres"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Run main function
main
