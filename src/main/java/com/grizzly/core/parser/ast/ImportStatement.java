package com.grizzly.core.parser.ast;

import java.util.List;

/**
 * Import statement for modules. Python-compatible.
 * 
 * <p><b>Examples:</b>
 * <pre>{@code
 * import re                    # re.match(), re.search() available
 * import datetime              # datetime.datetime.now() available
 * from decimal import Decimal  # Decimal("1.5") available
 * from datetime import datetime, timedelta
 * }</pre>
 * 
 * @param moduleName The name of the module (e.g., "re", "decimal", "datetime")
 * @param importedNames If non-null, from-import: names to bind in context. If null, simple import.
 * @param lineNumber Line number in source code
 */
public record ImportStatement(String moduleName, List<String> importedNames, int lineNumber) implements Statement {

    /** Simple import: import re */
    public static ImportStatement simple(String moduleName, int lineNumber) {
        return new ImportStatement(moduleName, null, lineNumber);
    }

    /** From import: from X import Y, Z */
    public static ImportStatement fromImport(String moduleName, List<String> names, int lineNumber) {
        return new ImportStatement(moduleName, names, lineNumber);
    }

    public boolean isFromImport() {
        return importedNames != null && !importedNames.isEmpty();
    }
}
