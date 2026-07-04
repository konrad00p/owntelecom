package com.owntelecom.module;

import com.owntelecom.OwnTelecomPlugin;

public interface OwnTelecomModule {
    void enable(OwnTelecomPlugin plugin);
    void disable(OwnTelecomPlugin plugin);
    String getName();
}
