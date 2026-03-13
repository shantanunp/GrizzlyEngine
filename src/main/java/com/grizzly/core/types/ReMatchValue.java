package com.grizzly.core.types;

import java.util.ArrayList;
import java.util.List;

/**
 * Python-compatible re.match/re.search result with .group(), .groups() methods.
 */
public record ReMatchValue(String fullMatch, List<Value> groups) implements Value {

    @Override
    public String typeName() {
        return "Match";
    }

    @Override
    public boolean isTruthy() {
        return true;
    }

    /** Python: m.group(0) or m.group() = full match, m.group(1) = first group, etc. */
    public Value group(int index) {
        if (index < 0 || index > groups.size()) {
            return NullValue.INSTANCE;
        }
        if (index == 0) {
            return new StringValue(fullMatch);
        }
        Value v = groups.get(index - 1);
        return v != null ? v : NullValue.INSTANCE;
    }

    /** Python: m.groups() = tuple of captured groups (group 1, 2, ...). */
    public ListValue groupsList() {
        return new ListValue(new ArrayList<>(groups));
    }
}
