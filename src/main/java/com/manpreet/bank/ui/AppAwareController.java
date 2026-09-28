package com.manpreet.bank.ui;

/**
 * Controllers that need navigation and service access implement this hook.
 */
public interface AppAwareController {

    void setSceneManager(SceneManager sceneManager);
}
