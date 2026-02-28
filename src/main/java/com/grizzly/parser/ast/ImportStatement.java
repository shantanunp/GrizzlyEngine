package com.grizzly.parser.ast;

/**
 * Import statement for modules.
 * 
 * <p>Currently supports simple imports for making module functions available.
 * 
 * <p><b>Examples:</b>
 * <pre>{@code
 * import re              # Makes re.match() available
 * import decimal         # Makes Decimal() available  
 * }</pre>
 * 
 * @param moduleName The name of the module to import (e.g., "re", "decimal")
 * @param lineNumber Line number in source code
 */
public record ImportStatement(String moduleName, int lineNumber) implements Statement {
}
