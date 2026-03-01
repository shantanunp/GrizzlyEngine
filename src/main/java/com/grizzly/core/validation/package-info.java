/**
 * Validation and access tracking for Grizzly transformations.
 * 
 * <p>This package provides mechanisms to track property accesses during template
 * execution and generate validation reports. This is essential for:
 * <ul>
 *   <li>Understanding why output fields are null (path broken vs data empty)</li>
 *   <li>Debugging transformation failures</li>
 *   <li>Audit trails for compliance</li>
 *   <li>Quality assurance of input data</li>
 * </ul>
 * 
 * <h2>Null Handling Modes</h2>
 * 
 * <p>Three modes control how null values are handled during property access:
 * 
 * <ul>
 *   <li><b>STRICT</b> - Throws exception on null access (use during development)</li>
 *   <li><b>SAFE</b> - Never crashes, tracks all accesses (recommended for production)</li>
 *   <li><b>SILENT</b> - Never crashes, no tracking (maximum performance)</li>
 * </ul>
 * 
 * <h2>Safe Navigation Operator</h2>
 * 
 * <p>Use {@code ?.} and {@code ?[} for explicit safe navigation:
 * <pre>{@code
 * OUTPUT["city"] = INPUT?.deal?.loan?.address?.city
 * OUTPUT["item"] = INPUT?["items"]?[0]
 * }</pre>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * JsonTemplate template = JsonTemplate.compile(templateCode, config);
 * JsonTransformationResult result = template.transformWithValidation(inputJson);
 * 
 * if (result.hasErrors()) {
 *     ValidationReport report = result.validationReport();
 *     for (AccessRecord error : report.getPathErrors()) {
 *         log.warn("Line {}: {} broken at '{}'", 
 *             error.lineNumber(), error.fullPath(), error.brokenAtSegment());
 *     }
 * }
 * }</pre>
 * 
 * @see com.grizzly.core.validation.NullHandling
 * @see com.grizzly.core.validation.ValidationReport
 * @see com.grizzly.core.validation.TransformationResult
 */
package com.grizzly.core.validation;
