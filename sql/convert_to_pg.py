#!/usr/bin/env python3
"""
Convert MySQL DDL dump to PostgreSQL DDL.
Handles:
- Backtick removal
- Type conversions (bigint AUTO_INCREMENT -> IDENTITY, datetime -> timestamp, tinyint -> smallint, int -> integer)
- Charset/collation/engine/row_format removal
- ON UPDATE CURRENT_TIMESTAMP removal
- USING BTREE removal
- sys_dept: ancestors -> path (ltree)
- DROP TABLE IF EXISTS removal
- SET NAMES / SET FOREIGN_KEY_CHECKS removal
- FOREIGN KEY constraint removal
"""

import re
import sys

def convert_mysql_to_pg(input_file, output_file):
    with open(input_file, 'r', encoding='utf-8') as f:
        content = f.read()

    lines = content.split('\n')
    output_lines = []

    # Add header
    output_lines.append('-- Zion-Admin PostgreSQL DDL')
    output_lines.append('-- Converted from MySQL 8.0.44')
    output_lines.append('-- Date: 2026/05/14')
    output_lines.append('')
    output_lines.append('CREATE EXTENSION IF NOT EXISTS ltree;')
    output_lines.append('')

    i = 0
    pending_sys_dept_inserts = None  # Collect sys_dept INSERTs to combine

    while i < len(lines):
        line = lines[i]

        # Skip MySQL-specific header comments (Navicat dump header)
        if line.strip().startswith('/*') or line.strip().startswith('*/') or line.strip().startswith('*'):
            i += 1
            continue

        # Skip SET NAMES, SET FOREIGN_KEY_CHECKS
        if line.strip().startswith('SET NAMES') or line.strip().startswith('SET FOREIGN_KEY_CHECKS'):
            i += 1
            continue

        # Skip DROP TABLE IF EXISTS
        if line.strip().startswith('DROP TABLE IF EXISTS'):
            # Flush pending sys_dept inserts before moving to next table
            if pending_sys_dept_inserts is not None:
                combined = convert_sys_dept_insert(pending_sys_dept_inserts)
                output_lines.append(combined)
                output_lines.append('')
                pending_sys_dept_inserts = None
            i += 1
            continue

        # Process CREATE TABLE
        if line.strip().startswith('CREATE TABLE'):
            # Flush pending sys_dept inserts
            if pending_sys_dept_inserts is not None:
                combined = convert_sys_dept_insert(pending_sys_dept_inserts)
                output_lines.append(combined)
                output_lines.append('')
                pending_sys_dept_inserts = None

            # Collect the entire CREATE TABLE statement
            stmt_lines = []
            while i < len(lines):
                stmt_lines.append(lines[i])
                if lines[i].strip().endswith(';'):
                    i += 1
                    break
                i += 1

            full_stmt = '\n'.join(stmt_lines)
            converted = convert_create_table(full_stmt)
            output_lines.append(converted)
            output_lines.append('')
            continue

        # Process table comment separator
        if '-- Table structure for' in line or '-- Records of' in line:
            i += 1
            continue

        # Process INSERT statements
        if line.strip().startswith('INSERT INTO'):
            # Collect the entire INSERT statement
            stmt_lines = []
            while i < len(lines):
                stmt_lines.append(lines[i])
                if lines[i].strip().endswith(';'):
                    i += 1
                    break
                i += 1

            full_stmt = '\n'.join(stmt_lines)

            # Check if this is a sys_dept INSERT (special handling to combine)
            if is_sys_dept_insert(full_stmt):
                if pending_sys_dept_inserts is None:
                    pending_sys_dept_inserts = ''
                pending_sys_dept_inserts += full_stmt + '\n'
            else:
                converted = convert_insert(full_stmt)
                if converted:
                    output_lines.append(converted.strip())
                    output_lines.append('')
            continue

        # Skip empty lines and other lines
        i += 1

    # Flush any remaining pending sys_dept inserts
    if pending_sys_dept_inserts is not None:
        combined = convert_sys_dept_insert(pending_sys_dept_inserts)
        output_lines.append(combined)
        output_lines.append('')

    # Write output
    with open(output_file, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(output_lines))

def get_table_name(create_stmt):
    """Extract table name from CREATE TABLE statement."""
    m = re.search(r'CREATE TABLE\s+`(\w+)`', create_stmt)
    if m:
        return m.group(1)
    m = re.search(r'CREATE TABLE\s+(\w+)', create_stmt)
    if m:
        return m.group(1)
    return None

def convert_create_table(stmt):
    """Convert a MySQL CREATE TABLE statement to PostgreSQL."""
    table_name = get_table_name(stmt)
    if not table_name:
        return stmt

    # Special handling for sys_dept
    if table_name == 'sys_dept':
        return convert_sys_dept_table(stmt)

    # 1. Remove backticks
    stmt = stmt.replace('`', '')

    # 2. Remove CHARACTER SET and COLLATE clauses
    stmt = re.sub(r'\s+CHARACTER\s+SET\s+\w+', '', stmt)
    stmt = re.sub(r'\s+COLLATE\s+\w+', '', stmt)

    # 3. Remove FOREIGN KEY CONSTRAINT lines completely (multi-line safe)
    # These appear as:
    #   CONSTRAINT name FOREIGN KEY (cols) REFERENCES table (cols) ON DELETE ... ON UPDATE ...
    # The comma before CONSTRAINT is on the previous line, so we need to handle that
    stmt = re.sub(
        r',\s*\n\s*CONSTRAINT\s+\w+\s+FOREIGN\s+KEY\s*\([^)]*\)\s*REFERENCES\s+[^\n;]*',
        '',
        stmt
    )

    # 4. Remove INDEX lines (they start with INDEX or UNIQUE INDEX after a comma)
    # Use greedy .* to handle nested parens like (col(100) ASC)
    # The .* between \( and \) greedily matches to the LAST ), handling nested parens

    # Regular INDEX - just remove (PG has separate CREATE INDEX)
    stmt = re.sub(
        r',\s*\n\s*INDEX\s+\w+\s*\(.*\)(\s*USING\s+BTREE)?',
        '',
        stmt
    )

    # UNIQUE INDEX -> UNIQUE constraint (also strip ASC/DESC)
    def convert_unique_index(m):
        cols = m.group(1)
        # Remove ASC and DESC from column list
        cols = re.sub(r'\s+ASC', '', cols)
        cols = re.sub(r'\s+DESC', '', cols)
        return ',\n    UNIQUE (' + cols + ')'

    stmt = re.sub(
        r',\s*\n\s*UNIQUE\s+INDEX\s+\w+\s*\((.*)\)(\s*USING\s+BTREE)?',
        convert_unique_index,
        stmt
    )

    # 5. Handle bigint NOT NULL AUTO_INCREMENT -> IDENTITY PRIMARY KEY
    stmt = re.sub(
        r'(\s+)bigint\s+NOT\s+NULL\s+AUTO_INCREMENT(\s+COMMENT\s+\'[^\']*\'(?:\s*,\s*)?)?',
        r'\1bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY\2',
        stmt
    )

    # 6. Remove any standalone PRIMARY KEY clause at end (already handled by IDENTITY)
    if 'GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY' in stmt:
        stmt = re.sub(
            r',\s*\n\s*PRIMARY\s+KEY\s*\([^)]+\)(\s*USING\s+BTREE)?',
            '',
            stmt
        )

    # 7. Convert tinyint to smallint (before int conversion!)
    stmt = re.sub(r'\btinyint\b', 'smallint', stmt)

    # 8. Convert int to integer (word-bounded, after tinyint/bigint handled)
    stmt = re.sub(r'\bint\b', 'integer', stmt)

    # 9. Convert datetime to timestamp
    stmt = re.sub(r'\bdatetime\b', 'timestamp', stmt)

    # 10. Remove ON UPDATE CURRENT_TIMESTAMP (must preserve trailing space for COMMENT)
    # Pattern: "ON UPDATE CURRENT_TIMESTAMP" optionally followed by COMMENT
    stmt = re.sub(
        r'\s+ON\s+UPDATE\s+CURRENT_TIMESTAMP(\s*\d*)?(\s+)(COMMENT\s)',
        r' \3',
        stmt
    )
    # Also handle case where ON UPDATE is at end of line (no COMMENT after)
    stmt = re.sub(r'\s+ON\s+UPDATE\s+CURRENT_TIMESTAMP(\s*\d*)?', '', stmt)

    # 11. Remove USING BTREE
    stmt = re.sub(r'\s+USING\s+BTREE', '', stmt)

    # 12. Remove ENGINE = ... clause at the very end
    stmt = re.sub(
        r'\s*\)\s*ENGINE\s*=\s*\w+[^;]*;',
        '\n);',
        stmt,
        flags=re.DOTALL
    )

    # 13. Clean up: remove trailing commas before closing paren
    stmt = re.sub(r',\s*\n\s*\)', '\n)', stmt)

    # 14. Clean up double commas
    stmt = re.sub(r',\s*,', ',', stmt)

    # 15. Clean up trailing whitespace
    stmt = re.sub(r'[ \t]+$', '', stmt, flags=re.MULTILINE)

    # 16. Clean up multiple blank lines -> single blank line
    stmt = re.sub(r'\n{3,}', '\n\n', stmt)

    # 17. Remove comments from table options (COMMENT = '...' at engine line, already removed)

    return stmt

def convert_sys_dept_table(stmt):
    """Special conversion for sys_dept table using ltree."""
    # Remove backticks
    stmt = stmt.replace('`', '')

    result = '''CREATE TABLE sys_dept (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    parent_id bigint DEFAULT 0,
    path ltree,
    dept_name varchar(50) NOT NULL,
    sort integer DEFAULT 0,
    leader varchar(50),
    phone varchar(20),
    email varchar(100),
    status smallint DEFAULT 1,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP,
    create_by bigint,
    update_by bigint,
    deleted smallint DEFAULT 0
);

CREATE INDEX idx_sys_dept_path ON sys_dept USING GIST (path);'''
    return result

def is_sys_dept_insert(stmt):
    """Check if this INSERT is for the sys_dept table."""
    m = re.search(r'INSERT\s+INTO\s+`?sys_dept`?\s+VALUES', stmt)
    return m is not None

def convert_insert(stmt):
    """Convert a MySQL INSERT statement to PostgreSQL."""
    # Remove backticks
    stmt = stmt.replace('`', '')
    return stmt

def convert_sys_dept_insert(stmt):
    """Convert sys_dept INSERT statements with ancestors -> path."""
    # Each INSERT is a single line: INSERT INTO `sys_dept` VALUES (1, 0, '0', ...);
    # We need to extract the values tuple, parse it, convert ancestors to path

    # Collect all INSERT lines for sys_dept
    all_rows = []
    lines = stmt.split('\n')
    for line in lines:
        line = line.strip()
        if not line.startswith('INSERT'):
            continue

        # Extract values: everything between VALUES ( and );
        # Handle optional backticks
        m = re.search(r'VALUES\s*\((.*)\)\s*;?\s*$', line.replace('`', ''))
        if not m:
            continue

        t = m.group(1)
        values = parse_value_tuple(t)

        if len(values) == 14:
            # MySQL order: id, parent_id, ancestors, dept_name, sort, leader, phone, email, status, create_time, update_time, create_by, update_by, deleted
            id_val = values[0]
            ancestors = values[2]  # e.g., '0' or '0,1' or '0,5'

            # Convert ancestors to ltree path: replace commas with dots, append .id
            ancestors_str = ancestors.strip("'").replace(',', '.')
            path_val = f"'{ancestors_str}.{id_val}'"

            # Build new row with path instead of ancestors
            new_values = values[:2] + [path_val] + values[3:]
            all_rows.append('(' + ', '.join(new_values) + ')')

    col_list = '(id, parent_id, path, dept_name, sort, leader, phone, email, status, create_time, update_time, create_by, update_by, deleted)'
    return f'INSERT INTO sys_dept {col_list} VALUES\n' + ',\n'.join(all_rows) + ';'

def parse_value_tuple(t):
    """Parse a comma-separated tuple of SQL values, respecting quoted strings."""
    values = []
    current = ''
    in_string = False
    i = 0
    while i < len(t):
        char = t[i]
        if char == "'":
            if in_string:
                # Check for escaped quote ''
                if i + 1 < len(t) and t[i+1] == "'":
                    current += "''"
                    i += 1
                else:
                    in_string = False
                    current += char
            else:
                in_string = True
                current += char
        elif char == ',' and not in_string:
            values.append(current.strip())
            current = ''
        else:
            current += char
        i += 1

    if current.strip():
        values.append(current.strip())

    return values


if __name__ == '__main__':
    import os
    script_dir = os.path.dirname(os.path.abspath(__file__))
    input_file = os.path.join(script_dir, 'zion-system.sql')
    output_file = os.path.join(script_dir, 'zion-system-postgresql.sql')
    convert_mysql_to_pg(input_file, output_file)
    print(f"Conversion complete: {output_file}")
