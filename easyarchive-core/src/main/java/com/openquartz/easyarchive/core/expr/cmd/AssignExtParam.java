package com.openquartz.easyarchive.core.expr.cmd;

import com.google.common.collect.ImmutableSortedSet;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author svnee
 */
@Getter
public class AssignExtParam {

    private Map<String, Object> params = new HashMap<>();
    public static final AssignExtParam EMPTY_PARAM = AssignExtParam.create();

    static {
        EMPTY_PARAM.params = Collections.unmodifiableMap(EMPTY_PARAM.params);
    }

    private AssignExtParam() {

    }

    public AssignExtParam set(String key, Object value) {
        this.params.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return ImmutableSortedSet.copyOf(params.keySet()).stream()
            .map(key -> "{" + key + "-" + params.get(key) + "}").collect(Collectors.joining());
    }

    public static AssignExtParam create() {
        return new AssignExtParam();
    }

}
