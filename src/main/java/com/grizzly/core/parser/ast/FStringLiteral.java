package com.grizzly.core.parser.ast;

/**
 * F-string literal: f"text {expr} more" — value is the raw string with {expr} placeholders.
 * Interpolation is evaluated at runtime.
 */
public record FStringLiteral(String value) implements Expression {}
