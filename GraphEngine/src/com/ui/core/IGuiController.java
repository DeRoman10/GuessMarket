package com.ui.core;

import com.maneger.SystemManager;

public interface IGuiController extends IRefreshable {

    void setManagers(SystemManager systemManager, ViewManager viewManager);
}