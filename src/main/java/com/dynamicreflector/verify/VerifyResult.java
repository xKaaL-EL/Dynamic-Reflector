package com.dynamicreflector.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VerifyResult {
    private final boolean success;
    private final List<String> messages;

    public VerifyResult(boolean success, List<String> messages) {
        this.success = success;
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public boolean isSuccess() {
        return success;
    }

    public List<String> getMessages() {
        return messages;
    }
}
